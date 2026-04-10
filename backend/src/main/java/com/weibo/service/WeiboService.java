package com.weibo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weibo.dto.WeiboVO;
import com.weibo.entity.Weibo;

import java.util.List;

/**
 * 微博服务接口
 */
public interface WeiboService {
    
    /**
     * 发布微博
     */
    WeiboVO publishWeibo(Long userId, Weibo weibo);
    
    /**
     * 删除微博
     */
    void deleteWeibo(Long userId, Long weiboId);
    
    /**
     * 获取微博详情
     */
    WeiboVO getWeiboDetail(Long userId, Long weiboId);
    
    /**
     * 获取微博列表
     */
    Page<WeiboVO> getWeiboList(Integer pageNum, Integer pageSize);
    
    /**
     * 获取用户微博列表
     */
    Page<WeiboVO> getUserWeiboList(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取关注的人的微博列表
     */
    Page<WeiboVO> getFollowingWeiboList(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 点赞微博
     */
    void likeWeibo(Long userId, Long weiboId);
    
    /**
     * 取消点赞
     */
    void unlikeWeibo(Long userId, Long weiboId);
    
    /**
     * 转发微博
     */
    WeiboVO repostWeibo(Long userId, Long weiboId, String content);
    
    /**
     * AI 审核微博内容
     * @param title 微博标题
     * @param content 微博内容
     * @return 审核结果 {ispass: 1/0, reson: ""/"原因"}
     */
    AiCheckResult aiCheck(String title, String content);
    
    /**
     * AI 审核结果 DTO
     */
    class AiCheckResult {
        private Integer ispass;
        private String reson;
        
        public AiCheckResult(Integer ispass, String reson) {
            this.ispass = ispass;
            this.reson = reson;
        }
        
        public Integer getIspass() { return ispass; }
        public String getReson() { return reson; }
    }
}
