package com.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weibo.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞 Mapper 接口
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
}
