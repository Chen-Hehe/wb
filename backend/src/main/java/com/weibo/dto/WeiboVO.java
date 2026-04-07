package com.weibo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 微博信息 VO
 */
@Data
@Accessors(chain = true)
public class WeiboVO {
    
    private Long id;
    
    private Long userId;
    
    private String content;
    
    private List<String> images;
    
    private Integer repostCount;
    
    private Integer commentCount;
    
    private Integer likeCount;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
    
    /**
     * 用户信息
     */
    private UserVO user;
    
    /**
     * 是否已点赞
     */
    private Boolean isLiked;
}
