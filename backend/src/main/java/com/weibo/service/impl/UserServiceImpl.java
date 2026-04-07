package com.weibo.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weibo.config.JwtUtil;
import com.weibo.dto.LoginDTO;
import com.weibo.dto.RegisterDTO;
import com.weibo.dto.UserVO;
import com.weibo.entity.Attention;
import com.weibo.entity.User;
import com.weibo.entity.Weibo;
import com.weibo.exception.BusinessException;
import com.weibo.mapper.AttentionMapper;
import com.weibo.mapper.UserMapper;
import com.weibo.mapper.WeiboMapper;
import com.weibo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final AttentionMapper attentionMapper;
    private final WeiboMapper weiboMapper;
    private final JwtUtil jwtUtil;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw BusinessException.of("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, registerDTO.getEmail());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw BusinessException.of("邮箱已被注册");
        }
        
        // 检查密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw BusinessException.of("两次输入的密码不一致");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setGender(0);
        user.setStatus(1);
        
        userMapper.insert(user);
        
        return convertToVO(user);
    }
    
    @Override
    public String login(LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        
        if (user.getStatus() == 0) {
            throw BusinessException.of("账号已被禁用");
        }
        
        // 验证密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw BusinessException.of("密码错误");
        }
        
        // 生成 Token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }
    
    @Override
    public String refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw BusinessException.of("刷新 Token 无效");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        return jwtUtil.generateToken(userId, username);
    }
    
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return convertToVO(user);
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        UserVO userVO = convertToVO(user);
        
        // 设置关注数、粉丝数、微博数
        LambdaQueryWrapper<Attention> followingQuery = new LambdaQueryWrapper<>();
        followingQuery.eq(Attention::getFollowerId, userId).eq(Attention::getStatus, 1);
        userVO.setFollowingCount(attentionMapper.selectCount(followingQuery).intValue());
        
        LambdaQueryWrapper<Attention> followerQuery = new LambdaQueryWrapper<>();
        followerQuery.eq(Attention::getFolloweeId, userId).eq(Attention::getStatus, 1);
        userVO.setFollowerCount(attentionMapper.selectCount(followerQuery).intValue());
        
        LambdaQueryWrapper<Weibo> weiboQuery = new LambdaQueryWrapper<>();
        weiboQuery.eq(Weibo::getUserId, userId).eq(Weibo::getStatus, 1);
        userVO.setWeiboCount(weiboMapper.selectCount(weiboQuery).intValue());
        
        return userVO;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(Long userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw BusinessException.of("用户不存在");
        }
        
        if (StringUtils.hasText(user.getNickname())) {
            existingUser.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getAvatar())) {
            existingUser.setAvatar(user.getAvatar());
        }
        if (user.getGender() != null) {
            existingUser.setGender(user.getGender());
        }
        if (user.getBirthday() != null) {
            existingUser.setBirthday(user.getBirthday());
        }
        if (StringUtils.hasText(user.getIntroduction())) {
            existingUser.setIntroduction(user.getIntroduction());
        }
        
        userMapper.updateById(existingUser);
        return convertToVO(existingUser);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw BusinessException.of("不能关注自己");
        }
        
        // 检查被关注用户是否存在
        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw BusinessException.of("用户不存在");
        }
        
        // 检查是否已关注
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getFollowerId, followerId)
                   .eq(Attention::getFolloweeId, followeeId);
        Attention existing = attentionMapper.selectOne(queryWrapper);
        
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw BusinessException.of("已关注该用户");
            }
            existing.setStatus(1);
            attentionMapper.updateById(existing);
        } else {
            Attention attention = new Attention();
            attention.setFollowerId(followerId);
            attention.setFolloweeId(followeeId);
            attention.setStatus(1);
            attentionMapper.insert(attention);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long followerId, Long followeeId) {
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getFollowerId, followerId)
                   .eq(Attention::getFolloweeId, followeeId);
        Attention attention = attentionMapper.selectOne(queryWrapper);
        
        if (attention == null || attention.getStatus() == 0) {
            throw BusinessException.of("未关注该用户");
        }
        
        attention.setStatus(0);
        attentionMapper.updateById(attention);
    }
    
    @Override
    public Page<UserVO> getFollowingList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Attention> attentionPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getFollowerId, userId)
                   .eq(Attention::getStatus, 1)
                   .orderByDesc(Attention::getCreatedTime);
        
        Page<Attention> page = attentionMapper.selectPage(attentionPage, queryWrapper);
        
        List<Long> followeeIds = page.getRecords().stream()
                .map(Attention::getFolloweeId)
                .collect(Collectors.toList());
        
        List<UserVO> userVOs = followeeIds.isEmpty() ? List.of() : 
                userMapper.selectBatchIds(followeeIds).stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
        
        Page<UserVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(userVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    public Page<UserVO> getFollowerList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Attention> attentionPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getFolloweeId, userId)
                   .eq(Attention::getStatus, 1)
                   .orderByDesc(Attention::getCreatedTime);
        
        Page<Attention> page = attentionMapper.selectPage(attentionPage, queryWrapper);
        
        List<Long> followerIds = page.getRecords().stream()
                .map(Attention::getFollowerId)
                .collect(Collectors.toList());
        
        List<UserVO> userVOs = followerIds.isEmpty() ? List.of() : 
                userMapper.selectBatchIds(followerIds).stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
        
        Page<UserVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(userVOs);
        result.setTotal(page.getTotal());
        return result;
    }
    
    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        LambdaQueryWrapper<Attention> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attention::getFollowerId, followerId)
                   .eq(Attention::getFolloweeId, followeeId)
                   .eq(Attention::getStatus, 1);
        return attentionMapper.selectCount(queryWrapper) > 0;
    }
    
    private UserVO convertToVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setGender(user.getGender());
        userVO.setBirthday(user.getBirthday());
        userVO.setIntroduction(user.getIntroduction());
        userVO.setStatus(user.getStatus());
        userVO.setCreatedTime(user.getCreatedTime());
        userVO.setUpdatedTime(user.getUpdatedTime());
        return userVO;
    }
}
