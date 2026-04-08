import request from '../utils/request';

/**
 * 文生图 - 根据 prompt 生成图片
 * @param prompt 图片描述
 */
export const txt2img = (prompt: string) => {
  return request.post('/ai/wan/txt2img', null, {
    params: { prompt },
  });
};

/**
 * 网络图片转存本地
 * @param url 网络图片 URL
 */
export const uploadImageByUrl = (url: string) => {
  return request.post('/upload/imagesByUrl', null, {
    params: { url },
  });
};
