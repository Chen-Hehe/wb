package com.weibo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 评论信息 VO
 */
@Data
@Accessors(chain = true)
public class CommentVO {
    
    private Long id;
    
    private Long weiboId;
    
    private Long userId;
    
    private Long parentId;
    
    private String content;
    
    private Integer likeCount;
    
    private LocalDateTime createdTime;
    
    /**
     * 用户信息
     */
    private UserVO user;
    
    /**
     * 是否已点赞
     */
    private Boolean isLiked;
}
