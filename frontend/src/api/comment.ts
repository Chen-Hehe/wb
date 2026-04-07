import request from '../utils/request';
import { PageResult, User } from './user';

export interface Comment {
  id: number;
  weiboId: number;
  userId: number;
  parentId: number;
  content: string;
  likeCount: number;
  createdTime: string;
  user?: User;
  isLiked?: boolean;
}

/**
 * 发表评论
 */
export const addComment = (data: { weiboId: number; content: string; parentId?: number }) => {
  return request.post('/comments', data);
};

/**
 * 删除评论
 */
export const deleteComment = (commentId: number) => {
  return request.delete(`/comments/${commentId}`);
};

/**
 * 获取微博评论列表
 */
export const getWeiboComments = (weiboId: number, pageNum = 1, pageSize = 10) => {
  return request.get(`/comments/weibo/${weiboId}`, { params: { pageNum, pageSize } });
};

/**
 * 获取评论的子评论
 */
export const getCommentReplies = (commentId: number, pageNum = 1, pageSize = 10) => {
  return request.get(`/comments/${commentId}/replies`, { params: { pageNum, pageSize } });
};

/**
 * 点赞评论
 */
export const likeComment = (commentId: number) => {
  return request.post(`/comments/${commentId}/like`);
};

/**
 * 取消点赞评论
 */
export const unlikeComment = (commentId: number) => {
  return request.delete(`/comments/${commentId}/like`);
};
