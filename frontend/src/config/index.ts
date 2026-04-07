// 后端 API 基础 URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083/api/v1';

// 构建完整的图片 URL
export const getImageUrl = (path: string): string => {
  if (!path) return '';
  // 如果已经是完整 URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  // 否则拼接后端基础 URL
  const baseUrl = API_BASE_URL.replace('/api/v1', '');
  return `${baseUrl}${path}`;
};
