import request from '../utils/request';

export interface LoginParams {
  username: string;
  password: string;
}

export interface RegisterParams {
  username: string;
  password: string;
  confirmPassword: string;
  email: string;
  phone?: string;
  nickname?: string;
}

/**
 * 用户登录
 */
export const login = (data: LoginParams) => {
  return request.post('/auth/login', data);
};

/**
 * 用户注册
 */
export const register = (data: RegisterParams) => {
  return request.post('/auth/register', data);
};

/**
 * 刷新 Token
 */
export const refreshToken = (refreshToken: string) => {
  return request.post('/auth/refresh', null, { params: { refreshToken } });
};

/**
 * 退出登录
 */
export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

/**
 * 获取当前登录用户信息
 */
export const getCurrentUser = () => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    return JSON.parse(userStr);
  }
  return null;
};
