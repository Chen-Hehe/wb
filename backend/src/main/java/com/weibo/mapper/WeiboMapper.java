package com.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weibo.entity.Weibo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微博 Mapper 接口
 */
@Mapper
public interface WeiboMapper extends BaseMapper<Weibo> {
}
