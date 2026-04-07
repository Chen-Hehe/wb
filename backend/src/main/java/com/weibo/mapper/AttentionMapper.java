package com.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weibo.entity.Attention;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注关系 Mapper 接口
 */
@Mapper
public interface AttentionMapper extends BaseMapper<Attention> {
}
