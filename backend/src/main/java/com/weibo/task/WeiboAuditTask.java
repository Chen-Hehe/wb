package com.weibo.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weibo.entity.Weibo;
import com.weibo.mapper.WeiboMapper;
import com.weibo.mapper.UserMapper;
import com.weibo.service.WeiboService;
import com.weibo.service.WeiboService.AiCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

/**
 * 微博定时审核任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeiboAuditTask {
    
    private final WeiboMapper weiboMapper;
    private final UserMapper userMapper;
    private final WeiboService weiboService;
    private final JavaMailSender mailSender;
    
    @Value("${weibo.audit.fixed-rate:300000}")
    private long auditFixedRate;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    /**
     * 定时审核待审核的微博
     * 每 5 分钟执行一次（可通过配置调整）
     */
    @Scheduled(fixedRateString = "${weibo.audit.fixed-rate:300000}")
    public void auditWeibos() {
        log.info("===== 开始执行微博定时审核任务 =====");
        
        try {
            // 查询所有待审核微博（status=1 且 wb_pass=0）
            List<Weibo> pendingWeibos = getPendingWeibos();
            
            if (pendingWeibos.isEmpty()) {
                log.info("本次扫描未找到待审核微博");
                return;
            }
            
            log.info("本次扫描到 {} 条待审核微博", pendingWeibos.size());
            
            // 逐条审核
            for (Weibo weibo : pendingWeibos) {
                auditSingleWeibo(weibo);
            }
            
            log.info("===== 微博定时审核任务执行完成 =====");
        } catch (Exception e) {
            log.error("微博定时审核任务执行异常", e);
        }
    }
    
    /**
     * 查询待审核微博列表
     */
    private List<Weibo> getPendingWeibos() {
        LambdaQueryWrapper<Weibo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Weibo::getStatus, 1)
               .eq(Weibo::getPass, 0)
               .orderByAsc(Weibo::getCreatedTime);
        
        return weiboMapper.selectList(wrapper);
    }
    
    /**
     * 审核单条微博
     */
    private void auditSingleWeibo(Weibo weibo) {
        try {
            log.info("开始审核微博：weiboId={}, userId={}, contentLength={}", 
                    weibo.getId(), weibo.getUserId(), 
                    weibo.getContent() != null ? weibo.getContent().length() : 0);
            
            // 清洗文本（去掉换行符等）
            String title = cleanText(weibo.getContent(), 20);
            String content = cleanText(weibo.getContent(), 500);
            
            // 调用 AI 审核
            AiCheckResult result = weiboService.aiCheck(title, content);
            
            if (result.getIspass() == 1) {
                // 审核通过
                weibo.setPass(1);
                weibo.setRemark("");
                weiboMapper.updateById(weibo);
                log.info("微博审核通过：weiboId={}", weibo.getId());
            } else {
                // 审核不通过
                weibo.setPass(2);
                weibo.setRemark(result.getReson() != null ? result.getReson() : "内容违规");
                weiboMapper.updateById(weibo);
                log.info("微博审核不通过：weiboId={}, reason={}", weibo.getId(), weibo.getRemark());
                
                // 发送邮件通知
                sendAuditFailedMail(weibo.getUserId(), weibo, weibo.getRemark());
            }
        } catch (Exception e) {
            log.error("审核微博失败：weiboId={}, error={}", weibo.getId(), e.getMessage());
            // 审核失败时不改变状态，下次定时任务会继续尝试
        }
    }
    
    /**
     * 清洗文本内容
     */
    private String cleanText(String text, int maxLength) {
        if (text == null) return "";
        
        // 去掉换行符和多余空格
        String cleaned = text.replace("\r\n", " ")
                            .replace("\r", " ")
                            .replace("\n", " ")
                            .replaceAll("\\s+", " ")
                            .trim();
        
        // 截断到指定长度
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        
        return cleaned;
    }
    
    /**
     * 发送审核失败通知邮件
     */
    private void sendAuditFailedMail(Long userId, Weibo weibo, String reason) {
        com.weibo.entity.User user = userMapper.selectById(userId);
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
}
