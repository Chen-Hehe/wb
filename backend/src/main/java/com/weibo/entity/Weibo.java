package com.weibo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.weibo.handler.StringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 微博实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "weibos", autoResultMap = true)
public class Weibo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 微博 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 微博内容
     */
    private String content;
    
    /**
     * 图片 URL 数组 (JSON)
     */
    @TableField(value = "images", typeHandler = StringListTypeHandler.class)
    private List<String> images;
    
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
     * 审核状态 0-待审核 1-通过 2-不通过
     */
    @TableField("wb_pass")
    private Integer pass;
    
    /**
     * 审核备注/不通过原因
     */
    @TableField("wb_remark")
    private String remark;
    
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
