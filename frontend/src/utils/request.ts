import axios from 'axios';
import { message } from 'antd';

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 120000, // 120 秒，AI 文生图需要较长时间
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { code, message: msg, data } = response.data;
    
    if (code === 200) {
      return data;
    } else if (code === 401) {
      // 未登录或 token 过期
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
      return Promise.reject(new Error(msg || '未登录'));
    } else {
      message.error(msg || '请求失败');
      return Promise.reject(new Error(msg || '请求失败'));
    }
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误';
    message.error(msg);
    return Promise.reject(error);
  }
);

export default request;
