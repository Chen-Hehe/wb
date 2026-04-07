import request from '../utils/request';

export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  nickname: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
  introduction?: string;
  status?: number;
  createdTime?: string;
  updatedTime?: string;
  followingCount?: number;
  followerCount?: number;
  weiboCount?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = () => {
  return request.get('/users/current');
};

/**
 * 获取用户信息
 */
export const getUserInfo = (userId: number) => {
  return request.get(`/users/${userId}`);
};

/**
 * 更新用户信息
 */
export const updateUserInfo = (data: Partial<User>) => {
  return request.put('/users', data);
};

/**
 * 关注用户
 */
export const followUser = (userId: number) => {
  return request.post(`/users/${userId}/follow`);
};

/**
 * 取消关注
 */
export const unfollowUser = (userId: number) => {
  return request.delete(`/users/${userId}/follow`);
};

/**
 * 获取关注列表
 */
export const getFollowingList = (userId: number, pageNum = 1, pageSize = 10) => {
  return request.get(`/users/${userId}/following`, { params: { pageNum, pageSize } });
};

/**
 * 获取粉丝列表
 */
export const getFollowerList = (userId: number, pageNum = 1, pageSize = 10) => {
  return request.get(`/users/${userId}/followers`, { params: { pageNum, pageSize } });
};

/**
 * 检查是否关注
 */
export const checkFollowing = (userId: number) => {
  return request.get(`/users/${userId}/following/check`);
};
