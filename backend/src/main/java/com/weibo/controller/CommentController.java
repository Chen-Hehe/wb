package com.weibo.controller;

import com.weibo.common.PageResult;
import com.weibo.common.Result;
import com.weibo.dto.CommentVO;
import com.weibo.entity.Comment;
import com.weibo.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 发表评论
     */
    @PostMapping
    public Result<CommentVO> addComment(HttpServletRequest request, @RequestBody Comment comment) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success("评论成功", commentService.addComment(userId, comment));
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(HttpServletRequest request, @PathVariable Long commentId) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(userId, commentId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取微博评论列表
     */
    @GetMapping("/weibo/{weiboId}")
    public Result<PageResult<CommentVO>> getWeiboComments(
            @PathVariable Long weiboId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = commentService.getWeiboComments(weiboId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 获取评论的子评论
     */
    @GetMapping("/{commentId}/replies")
    public Result<PageResult<CommentVO>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = commentService.getCommentReplies(commentId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Result<Void> likeComment(HttpServletRequest request, @PathVariable Long commentId) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.likeComment(userId, commentId);
        return Result.success("点赞成功", null);
    }
    
    /**
     * 取消点赞评论
     */
    @DeleteMapping("/{commentId}/like")
    public Result<Void> unlikeComment(HttpServletRequest request, @PathVariable Long commentId) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.unlikeComment(userId, commentId);
        return Result.success("取消点赞成功", null);
    }
}
