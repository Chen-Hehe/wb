package com.weibo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weibo.dto.CommentVO;
import com.weibo.dto.UserVO;
import com.weibo.entity.Comment;
import com.weibo.entity.CommentLike;
import com.weibo.entity.User;
import com.weibo.entity.Weibo;
import com.weibo.exception.BusinessException;
import com.weibo.mapper.CommentLikeMapper;
import com.weibo.mapper.CommentMapper;
import com.weibo.mapper.UserMapper;
import com.weibo.mapper.WeiboMapper;
import com.weibo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final CommentMapper commentMapper;
    private final WeiboMapper weiboMapper;
    private final UserMapper userMapper;
    private final CommentLikeMapper commentLikeMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO addComment(Long userId, Comment comment) {
        if (!StringUtils.hasText(comment.getContent())) {
            throw BusinessException.of("评论内容不能为空");
        }
        
        // 检查微博是否存在
        Weibo weibo = weiboMapper.selectById(comment.getWeiboId());
        if (weibo == null || weibo.getStatus() == 0) {
            throw BusinessException.of("微博不存在");
        }
        
        comment.setUserId(userId);
        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setParentId(comment.getParentId() != null ? comment.getParentId() : 0L);
        
        commentMapper.insert(comment);
        
        // 更新微博评论数
        weibo.setCommentCount(weibo.getCommentCount() + 1);
        weiboMapper.updateById(weibo);
        
        return getCommentVO(comment, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of("评论不存在");
        }
        
        if (!comment.getUserId().equals(userId)) {
            throw BusinessException.of("只能删除自己的评论");
        }
        
        comment.setStatus(0);
        commentMapper.updateById(comment);
        
        // 更新微博评论数
        Weibo weibo = weiboMapper.selectById(comment.getWeiboId());
        if (weibo != null) {
            weibo.setCommentCount(Math.max(0, weibo.getCommentCount() - 1));
            weiboMapper.updateById(weibo);
        }
    }
    
    @Override
    public Page<CommentVO> getWeiboComments(Long weiboId, Integer pageNum, Integer pageSize) {
        Page<Comment> commentPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getWeiboId, weiboId)
                   .eq(Comment::getParentId, 0)
                   .eq(Comment::getStatus, 1)
                   .orderByDesc(Comment::getCreatedTime);
        
        Page<Comment> page = commentMapper.selectPage(commentPage, queryWrapper);
        List<CommentVO> commentVOs = page.getRecords().stream()
                .map(comment -> getCommentVO(comment, null))
                .collect(Collectors.toList());
        
        Page<CommentVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(commentVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    public Page<CommentVO> getCommentReplies(Long commentId, Integer pageNum, Integer pageSize) {
        Page<Comment> commentPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, commentId)
                   .eq(Comment::getStatus, 1)
                   .orderByAsc(Comment::getCreatedTime);
        
        Page<Comment> page = commentMapper.selectPage(commentPage, queryWrapper);
        List<CommentVO> commentVOs = page.getRecords().stream()
                .map(comment -> getCommentVO(comment, null))
                .collect(Collectors.toList());
        
        Page<CommentVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(commentVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == 0) {
            throw BusinessException.of("评论不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getCommentId, commentId)
                   .eq(CommentLike::getUserId, userId);
        if (commentLikeMapper.selectCount(queryWrapper) > 0) {
            throw BusinessException.of("已点赞");
        }
        
        // 添加点赞记录
        CommentLike commentLike = new CommentLike();
        commentLike.setCommentId(commentId);
        commentLike.setUserId(userId);
        commentLikeMapper.insert(commentLike);
        
        // 更新点赞数
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(comment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == 0) {
            throw BusinessException.of("评论不存在");
        }
        
        // 删除点赞记录
        LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentLike::getCommentId, commentId)
                   .eq(CommentLike::getUserId, userId);
        CommentLike commentLike = commentLikeMapper.selectOne(queryWrapper);
        
        if (commentLike == null) {
            throw BusinessException.of("未点赞");
        }
        
        commentLikeMapper.deleteById(commentLike);
        
        // 更新点赞数
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentMapper.updateById(comment);
    }
    
    private CommentVO getCommentVO(Comment comment, Long currentUserId) {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(comment.getId());
        commentVO.setWeiboId(comment.getWeiboId());
        commentVO.setUserId(comment.getUserId());
        commentVO.setParentId(comment.getParentId());
        commentVO.setContent(comment.getContent());
        commentVO.setLikeCount(comment.getLikeCount());
        commentVO.setCreatedTime(comment.getCreatedTime());
        
        // 设置用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatar(user.getAvatar());
            commentVO.setUser(userVO);
        }
        
        // 设置是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<CommentLike> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CommentLike::getCommentId, comment.getId())
                       .eq(CommentLike::getUserId, currentUserId);
            commentVO.setIsLiked(commentLikeMapper.selectCount(queryWrapper) > 0);
        } else {
            commentVO.setIsLiked(false);
        }
        
        return commentVO;
    }
}
