package com.weibo.controller;

import com.weibo.common.PageResult;
import com.weibo.common.Result;
import com.weibo.dto.WeiboVO;
import com.weibo.entity.Weibo;
import com.weibo.service.WeiboService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 微博控制器
 */
@RestController
@RequestMapping("/weibos")
@RequiredArgsConstructor
public class WeiboController {
    
    private final WeiboService weiboService;
    
    /**
     * 发布微博
     */
    @PostMapping
    public Result<WeiboVO> publishWeibo(HttpServletRequest request, @RequestBody Weibo weibo) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success("发布成功", weiboService.publishWeibo(userId, weibo));
    }
    
    /**
     * 删除微博
     */
    @DeleteMapping("/{weiboId}")
    public Result<Void> deleteWeibo(HttpServletRequest request, @PathVariable Long weiboId) {
        Long userId = (Long) request.getAttribute("userId");
        weiboService.deleteWeibo(userId, weiboId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取微博详情
     */
    @GetMapping("/{weiboId}")
    public Result<WeiboVO> getWeiboDetail(HttpServletRequest request, @PathVariable Long weiboId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(weiboService.getWeiboDetail(userId, weiboId));
    }
    
    /**
     * 获取微博列表
     */
    @GetMapping
    public Result<PageResult<WeiboVO>> getWeiboList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = weiboService.getWeiboList(pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 获取用户微博列表
     */
    @GetMapping("/user/{userId}")
    public Result<PageResult<WeiboVO>> getUserWeiboList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = weiboService.getUserWeiboList(userId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 获取关注的人的微博列表
     */
    @GetMapping("/following")
    public Result<PageResult<WeiboVO>> getFollowingWeiboList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        var page = weiboService.getFollowingWeiboList(userId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 点赞微博
     */
    @PostMapping("/{weiboId}/like")
    public Result<Void> likeWeibo(HttpServletRequest request, @PathVariable Long weiboId) {
        Long userId = (Long) request.getAttribute("userId");
        weiboService.likeWeibo(userId, weiboId);
        return Result.success("点赞成功", null);
    }
    
    /**
     * 取消点赞
     */
    @DeleteMapping("/{weiboId}/like")
    public Result<Void> unlikeWeibo(HttpServletRequest request, @PathVariable Long weiboId) {
        Long userId = (Long) request.getAttribute("userId");
        weiboService.unlikeWeibo(userId, weiboId);
        return Result.success("取消点赞成功", null);
    }
    
    /**
     * 转发微博
     */
    @PostMapping("/{weiboId}/repost")
    public Result<WeiboVO> repostWeibo(
            HttpServletRequest request,
            @PathVariable Long weiboId,
            @RequestBody(required = false) String content) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success("转发成功", weiboService.repostWeibo(userId, weiboId, content));
    }
}
