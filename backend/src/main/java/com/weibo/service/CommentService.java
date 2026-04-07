package com.weibo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weibo.dto.CommentVO;
import com.weibo.entity.Comment;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 发表评论
     */
    CommentVO addComment(Long userId, Comment comment);
    
    /**
     * 删除评论
     */
    void deleteComment(Long userId, Long commentId);
    
    /**
     * 获取微博评论列表
     */
    Page<CommentVO> getWeiboComments(Long weiboId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取评论的子评论
     */
    Page<CommentVO> getCommentReplies(Long commentId, Integer pageNum, Integer pageSize);
    
    /**
     * 点赞评论
     */
    void likeComment(Long userId, Long commentId);
    
    /**
     * 取消点赞评论
     */
    void unlikeComment(Long userId, Long commentId);
}
