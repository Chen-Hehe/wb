import request from '../utils/request';
import { PageResult, User } from './user';

export interface Weibo {
  id: number;
  userId: number;
  content: string;
  images?: string[];
  repostCount: number;
  commentCount: number;
  likeCount: number;
  createdTime: string;
  updatedTime: string;
  user?: User;
  isLiked?: boolean;
}

/**
 * 发布微博
 */
export const publishWeibo = (data: { content: string; images?: string[] }) => {
  return request.post('/weibos', data);
};

/**
 * 删除微博
 */
export const deleteWeibo = (weiboId: number) => {
  return request.delete(`/weibos/${weiboId}`);
};

/**
 * 获取微博详情
 */
export const getWeiboDetail = (weiboId: number) => {
  return request.get(`/weibos/${weiboId}`);
};

/**
 * 获取微博列表
 */
export const getWeiboList = (pageNum = 1, pageSize = 10) => {
  return request.get('/weibos', { params: { pageNum, pageSize } });
};

/**
 * 获取用户微博列表
 */
export const getUserWeiboList = (userId: number, pageNum = 1, pageSize = 10) => {
  return request.get(`/weibos/user/${userId}`, { params: { pageNum, pageSize } });
};

/**
 * 获取关注的人的微博列表
 */
export const getFollowingWeiboList = (pageNum = 1, pageSize = 10) => {
  return request.get('/weibos/following', { params: { pageNum, pageSize } });
};

/**
 * 点赞微博
 */
export const likeWeibo = (weiboId: number) => {
  return request.post(`/weibos/${weiboId}/like`);
};

/**
 * 取消点赞
 */
export const unlikeWeibo = (weiboId: number) => {
  return request.delete(`/weibos/${weiboId}/like`);
};

/**
 * 转发微博
 */
export const repostWeibo = (weiboId: number, content?: string) => {
  return request.post(`/weibos/${weiboId}/repost`, content);
};
