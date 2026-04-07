package com.weibo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weibo.dto.LoginDTO;
import com.weibo.dto.RegisterDTO;
import com.weibo.dto.UserVO;
import com.weibo.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    UserVO register(RegisterDTO registerDTO);
    
    /**
     * 用户登录
     */
    String login(LoginDTO loginDTO);
    
    /**
     * 刷新 Token
     */
    String refreshToken(String refreshToken);
    
    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser(Long userId);
    
    /**
     * 获取用户信息
     */
    UserVO getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     */
    UserVO updateUserInfo(Long userId, User user);
    
    /**
     * 关注用户
     */
    void followUser(Long followerId, Long followeeId);
    
    /**
     * 取消关注
     */
    void unfollowUser(Long followerId, Long followeeId);
    
    /**
     * 获取关注列表
     */
    Page<UserVO> getFollowingList(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取粉丝列表
     */
    Page<UserVO> getFollowerList(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 检查是否关注
     */
    boolean isFollowing(Long followerId, Long followeeId);
}
