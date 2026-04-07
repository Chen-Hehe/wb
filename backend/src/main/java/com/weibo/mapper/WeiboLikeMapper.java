package com.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weibo.entity.WeiboLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微博点赞 Mapper 接口
 */
@Mapper
public interface WeiboLikeMapper extends BaseMapper<WeiboLike> {
}
