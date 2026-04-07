import request from '../utils/request';

/**
 * 上传单张图片
 */
export const uploadImage = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/upload/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

/**
 * 批量上传图片
 */
export const uploadImages = (files: File[]) => {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  return request.post('/upload/images/batch', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};
