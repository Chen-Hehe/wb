package com.weibo.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 微博服务实现类
 */
@Service
@RequiredArgsConstructor
public class WeiboServiceImpl implements WeiboService {
    
    private final WeiboMapper weiboMapper;
    private final UserMapper userMapper;
    private final AttentionMapper attentionMapper;
    private final WeiboLikeMapper weiboLikeMapper;
    private final UserService userService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeiboVO publishWeibo(Long userId, Weibo weibo) {
        if (!StringUtils.hasText(weibo.getContent())) {
            throw BusinessException.of("微博内容不能为空");
        }
        
        weibo.setUserId(userId);
        weibo.setStatus(1);
        weibo.setRepostCount(0);
        weibo.setCommentCount(0);
        weibo.setLikeCount(0);
        
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
    
    private WeiboVO getWeiboVO(Weibo weibo, Long currentUserId) {
        WeiboVO weiboVO = new WeiboVO();
        weiboVO.setId(weibo.getId());
        weiboVO.setUserId(weibo.getUserId());
        weiboVO.setContent(weibo.getContent());
        weiboVO.setImages(StringUtils.hasText(weibo.getImages()) ? 
                JSONUtil.toList(weibo.getImages(), String.class) : List.of());
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
