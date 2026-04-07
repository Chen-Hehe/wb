package com.weibo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
@Accessors(chain = true)
public class UserVO {
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    private String nickname;
    
    private String avatar;
    
    private Integer gender;
    
    private LocalDate birthday;
    
    private String introduction;
    
    private Integer status;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
    
    /**
     * 关注数
     */
    private Integer followingCount;
    
    /**
     * 粉丝数
     */
    private Integer followerCount;
    
    /**
     * 微博数
     */
    private Integer weiboCount;
}
