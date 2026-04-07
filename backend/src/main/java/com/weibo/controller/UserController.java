package com.weibo.controller;

import com.weibo.common.PageResult;
import com.weibo.common.Result;
import com.weibo.dto.UserVO;
import com.weibo.entity.User;
import com.weibo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getCurrentUser(userId));
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserVO> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping
    public Result<UserVO> updateUserInfo(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.updateUserInfo(userId, user));
    }
    
    /**
     * 关注用户
     */
    @PostMapping("/{userId}/follow")
    public Result<Void> followUser(HttpServletRequest request, @PathVariable("userId") Long followeeId) {
        Long followerId = (Long) request.getAttribute("userId");
        userService.followUser(followerId, followeeId);
        return Result.success("关注成功", null);
    }
    
    /**
     * 取消关注
     */
    @DeleteMapping("/{userId}/follow")
    public Result<Void> unfollowUser(HttpServletRequest request, @PathVariable("userId") Long followeeId) {
        Long followerId = (Long) request.getAttribute("userId");
        userService.unfollowUser(followerId, followeeId);
        return Result.success("取消关注成功", null);
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/{userId}/following")
    public Result<PageResult<UserVO>> getFollowingList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = userService.getFollowingList(userId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 获取粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public Result<PageResult<UserVO>> getFollowerList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = userService.getFollowerList(userId, pageNum, pageSize);
        var result = PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 检查是否关注
     */
    @GetMapping("/{userId}/following/check")
    public Result<Boolean> checkFollowing(HttpServletRequest request, @PathVariable("userId") Long followeeId) {
        Long followerId = (Long) request.getAttribute("userId");
        return Result.success(userService.isFollowing(followerId, followeeId));
    }
}
