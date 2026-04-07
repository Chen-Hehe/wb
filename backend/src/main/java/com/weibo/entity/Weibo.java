package com.weibo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微博实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("weibos")
public class Weibo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 微博ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 微博内容
     */
    private String content;
    
    /**
     * 图片URL数组(JSON)
     */
    private String images;
    
    /**
     * 转发数
     */
    private Integer repostCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 状态 0-删除 1-正常
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
