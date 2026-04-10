package com.weibo.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weibo.dto.UserVO;
import com.weibo.dto.WeiboVO;
import com.weibo.entity.Attention;
import com.weibo.entity.User;
import com.weibo.entity.Weibo;
import com.weibo.entity.WeiboLike;
import com.weibo.exception.BusinessException;
import com.weibo.mapper.AttentionMapper;
import com.weibo.mapper.UserMapper;
import com.weibo.mapper.WeiboLikeMapper;
import com.weibo.mapper.WeiboMapper;
import com.weibo.service.UserService;
import com.weibo.service.WeiboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 微博服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeiboServiceImpl implements WeiboService {
    
    private final WeiboMapper weiboMapper;
    private final UserMapper userMapper;
    private final AttentionMapper attentionMapper;
    private final WeiboLikeMapper weiboLikeMapper;
    private final UserService userService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    private static final String DASHSCOPE_CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeiboVO publishWeibo(Long userId, Weibo weibo) {
        if (!StringUtils.hasText(weibo.getContent()) && (weibo.getImages() == null || weibo.getImages().isEmpty())) {
            throw BusinessException.of("微博内容或图片不能为空");
        }
        
        weibo.setUserId(userId);
        weibo.setStatus(1);
        weibo.setRepostCount(0);
        weibo.setCommentCount(0);
        weibo.setLikeCount(0);
        // 设置待审核状态
        weibo.setPass(0);
        weibo.setRemark("");
        
        weiboMapper.insert(weibo);
        return getWeiboVO(weibo, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWeibo(Long userId, Long weiboId) {
        Weibo weibo = weiboMapper.selectById(weiboId);
        if (weibo == null) {
            throw BusinessException.of("微博不存在");
        }
        
        if (!weibo.getUserId().equals(userId)) {
            throw BusinessException.of("只能删除自己的微博");
        }
        
        weibo.setStatus(0);
        weiboMapper.updateById(weibo);
    }
    
    @Override
    public WeiboVO getWeiboDetail(Long userId, Long weiboId) {
        Weibo weibo = weiboMapper.selectById(weiboId);
        if (weibo == null || weibo.getStatus() == 0) {
            throw BusinessException.of("微博不存在");
        }
        return getWeiboVO(weibo, userId);
    }
    
    @Override
    public Page<WeiboVO> getWeiboList(Integer pageNum, Integer pageSize) {
        Page<Weibo> weiboPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Weibo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Weibo::getStatus, 1)
                   .eq(Weibo::getPass, 1)  // 只显示审核通过的数据
                   .orderByDesc(Weibo::getCreatedTime);
        
        Page<Weibo> page = weiboMapper.selectPage(weiboPage, queryWrapper);
        List<WeiboVO> weiboVOs = page.getRecords().stream()
                .map(weibo -> getWeiboVO(weibo, null))
                .collect(Collectors.toList());
        
        Page<WeiboVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(weiboVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    public Page<WeiboVO> getUserWeiboList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Weibo> weiboPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Weibo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Weibo::getUserId, userId)
                   .eq(Weibo::getStatus, 1)
                   .eq(Weibo::getPass, 1)  // 只显示审核通过的数据
                   .orderByDesc(Weibo::getCreatedTime);
        
        Page<Weibo> page = weiboMapper.selectPage(weiboPage, queryWrapper);
        List<WeiboVO> weiboVOs = page.getRecords().stream()
                .map(weibo -> getWeiboVO(weibo, null))
                .collect(Collectors.toList());
        
        Page<WeiboVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(weiboVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    public Page<WeiboVO> getFollowingWeiboList(Long userId, Integer pageNum, Integer pageSize) {
        // 获取关注的用户 ID 列表
        LambdaQueryWrapper<Attention> attentionQuery = new LambdaQueryWrapper<>();
        attentionQuery.eq(Attention::getFollowerId, userId)
                     .eq(Attention::getStatus, 1);
        List<Attention> attentions = attentionMapper.selectList(attentionQuery);
        
        if (attentions.isEmpty()) {
            Page<WeiboVO> result = new Page<>(pageNum, pageSize);
            result.setRecords(List.of());
            result.setTotal(0L);
            return result;
        }
        
        List<Long> followeeIds = attentions.stream()
                .map(Attention::getFolloweeId)
                .collect(Collectors.toList());
        
        Page<Weibo> weiboPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Weibo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Weibo::getUserId, followeeIds)
                   .eq(Weibo::getStatus, 1)
                   .eq(Weibo::getPass, 1)  // 只显示审核通过的数据
                   .orderByDesc(Weibo::getCreatedTime);
        
        Page<Weibo> page = weiboMapper.selectPage(weiboPage, queryWrapper);
        List<WeiboVO> weiboVOs = page.getRecords().stream()
                .map(weibo -> getWeiboVO(weibo, userId))
                .collect(Collectors.toList());
        
        Page<WeiboVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(weiboVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeWeibo(Long userId, Long weiboId) {
        Weibo weibo = weiboMapper.selectById(weiboId);
        if (weibo == null || weibo.getStatus() == 0) {
            throw BusinessException.of("微博不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<WeiboLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WeiboLike::getWeiboId, weiboId)
                   .eq(WeiboLike::getUserId, userId);
        if (weiboLikeMapper.selectCount(queryWrapper) > 0) {
            throw BusinessException.of("已点赞");
        }
        
        // 添加点赞记录
        WeiboLike weiboLike = new WeiboLike();
        weiboLike.setWeiboId(weiboId);
        weiboLike.setUserId(userId);
        weiboLikeMapper.insert(weiboLike);
        
        // 更新点赞数
        weibo.setLikeCount(weibo.getLikeCount() + 1);
        weiboMapper.updateById(weibo);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeWeibo(Long userId, Long weiboId) {
        Weibo weibo = weiboMapper.selectById(weiboId);
        if (weibo == null || weibo.getStatus() == 0) {
            throw BusinessException.of("微博不存在");
        }
        
        // 删除点赞记录
        LambdaQueryWrapper<WeiboLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WeiboLike::getWeiboId, weiboId)
                   .eq(WeiboLike::getUserId, userId);
        WeiboLike weiboLike = weiboLikeMapper.selectOne(queryWrapper);
        
        if (weiboLike == null) {
            throw BusinessException.of("未点赞");
        }
        
        weiboLikeMapper.deleteById(weiboLike);
        
        // 更新点赞数
        weibo.setLikeCount(Math.max(0, weibo.getLikeCount() - 1));
        weiboMapper.updateById(weibo);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeiboVO repostWeibo(Long userId, Long weiboId, String content) {
        Weibo originalWeibo = weiboMapper.selectById(weiboId);
        if (originalWeibo == null || originalWeibo.getStatus() == 0) {
            throw BusinessException.of("原微博不存在");
        }
        
        // 创建转发微博
        Weibo repostWeibo = new Weibo();
        repostWeibo.setUserId(userId);
        repostWeibo.setContent(content != null ? content : "转发微博");
        repostWeibo.setStatus(1);
        repostWeibo.setRepostCount(0);
        repostWeibo.setCommentCount(0);
        repostWeibo.setLikeCount(0);
        
        weiboMapper.insert(repostWeibo);
        
        // 更新原微博转发数
        originalWeibo.setRepostCount(originalWeibo.getRepostCount() + 1);
        weiboMapper.updateById(originalWeibo);
        
        return getWeiboVO(repostWeibo, userId);
    }
    
    @Override
    public AiCheckResult aiCheck(String title, String content) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("未配置 DASHSCOPE_API_KEY，默认审核通过");
            return new AiCheckResult(1, "");
        }
        
        String prompt = String.format(
            "你是一个严谨的内容审核员，需要审核用户发表的博客的标题和内容是否包含非法、违规、敏感或不适当的内容。\n" +
            "请仅输出标准 JSON 格式，不要有任何其他文字、标记或解释。\n" +
            "如果内容合规，返回：{\"ispass\":1,\"reson\":\"\"}\n" +
            "如果内容违规，返回：{\"ispass\":0,\"reson\":\"具体违规原因\"}\n" +
            "审核标准：\n" +
            "1. 不能包含政治敏感内容\n" +
            "2. 不能包含色情低俗内容\n" +
            "3. 不能包含暴力恐怖内容\n" +
            "4. 不能包含违法信息\n" +
            "5. 不能包含人身攻击或歧视内容\n" +
            "\n" +
            "标题：%s\n" +
            "内容：%s",
            title != null ? title : "",
            content != null ? content : ""
        );
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            Map<String, Object> body = Map.of(
                "model", "qwen-plus",
                "messages", List.of(
                    Map.of(
                        "role", "system",
                        "content", "你是一个严谨的内容审核员，只输出标准 JSON 格式，不要有任何其他文字。"
                    ),
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                ),
                "temperature", 0.1
            );
            
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("===== AI 审核请求 =====");
            log.info("标题：{}", title);
            log.info("内容长度：{}", content != null ? content.length() : 0);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_CHAT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            log.info("===== AI 审核响应 =====");
            log.info("状态码：{}", response.statusCode());
            log.info("响应体：{}", responseBody);
            
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI 审核 API 调用失败 status={}", response.statusCode());
                return new AiCheckResult(1, ""); // API 失败时默认通过
            }
            
            JsonNode root = objectMapper.readTree(responseBody);
            String aiResponse = root.path("choices").path(0).path("message").path("content").asText();
            log.info("AI 审核原始响应：{}", aiResponse);
            
            // 解析 AI 返回的 JSON
            if (!StringUtils.hasText(aiResponse)) {
                log.warn("AI 审核返回内容为空");
                return new AiCheckResult(1, "");
            }
            
            // 提取 JSON 部分（防止 AI 返回多余文字）
            String jsonStr = extractJson(aiResponse);
            if (jsonStr == null) {
                log.warn("无法从 AI 响应中提取 JSON: {}", aiResponse);
                return new AiCheckResult(1, "");
            }
            
            try {
                cn.hutool.json.JSONObject result = JSONUtil.parseObj(jsonStr);
                Integer ispass = result.get("ispass", Integer.class);
                String reson = result.getStr("reson", "");
                
                if (ispass == null) ispass = 1;
                if (reson == null) reson = "";
                
                log.info("审核结果：ispass={}, reson={}", ispass, reson);
                return new AiCheckResult(ispass, reson);
            } catch (Exception e) {
                log.warn("解析 AI 审核结果 JSON 失败：{}", e.getMessage());
                return new AiCheckResult(1, "");
            }
            
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("调用 AI 审核失败", e);
            return new AiCheckResult(1, ""); // 调用失败时默认通过
        }
    }
    
    /**
     * 从文本中提取 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null) return null;
        
        // 尝试直接解析
        if (text.trim().startsWith("{")) {
            return text.trim();
        }
        
        // 查找 JSON 对象
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return null;
    }
    
    /**
     * 发送审核不通过邮件通知
     */
    public void sendAuditFailedMail(Long userId, Weibo weibo, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，无法发送邮件：userId={}", userId);
            return;
        }
        
        if (!StringUtils.hasText(user.getEmail())) {
            log.info("用户邮箱为空，跳过邮件发送：userId={}, username={}", userId, user.getUsername());
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailUsername);
            helper.setTo(user.getEmail());
            helper.setSubject("微博审核未通过通知");
            
            String content = String.format(
                "亲爱的 %s：\n\n" +
                "您发布的微博审核未通过。\n\n" +
                "微博内容：%s\n\n" +
                "未通过原因：%s\n\n" +
                "请修改后重新发布。\n\n" +
                "微博系统",
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                weibo.getContent().length() > 100 ? weibo.getContent().substring(0, 100) + "..." : weibo.getContent(),
                reason != null ? reason : "未知原因"
            );
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("邮件发送成功：userId={}, email={}", userId, user.getEmail());
        } catch (MessagingException e) {
            log.error("邮件发送失败：userId={}, email={}, error={}", userId, user.getEmail(), e.getMessage());
        }
    }
    
    private WeiboVO getWeiboVO(Weibo weibo, Long currentUserId) {
        WeiboVO weiboVO = new WeiboVO();
        weiboVO.setId(weibo.getId());
        weiboVO.setUserId(weibo.getUserId());
        weiboVO.setContent(weibo.getContent());
        weiboVO.setImages(weibo.getImages() != null ? weibo.getImages() : List.of());
        weiboVO.setRepostCount(weibo.getRepostCount());
        weiboVO.setCommentCount(weibo.getCommentCount());
        weiboVO.setLikeCount(weibo.getLikeCount());
        weiboVO.setCreatedTime(weibo.getCreatedTime());
        weiboVO.setUpdatedTime(weibo.getUpdatedTime());
        
        // 设置用户信息
        User user = userMapper.selectById(weibo.getUserId());
        if (user != null) {
            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatar(user.getAvatar());
            weiboVO.setUser(userVO);
        }
        
        // 设置是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<WeiboLike> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WeiboLike::getWeiboId, weibo.getId())
                       .eq(WeiboLike::getUserId, currentUserId);
            weiboVO.setIsLiked(weiboLikeMapper.selectCount(queryWrapper) > 0);
        } else {
            weiboVO.setIsLiked(false);
        }
        
        return weiboVO;
    }
}
