import createRequest from '@/lib/request';
import { API_CONFIG } from '@/config/api.config';
import axios from 'axios';

// 创建内容服务实例
export const contentRequest = createRequest({
  baseURL: API_CONFIG.CONTENT_API.baseURL,
  timeout: API_CONFIG.CONTENT_API.timeout,
  requestOptions: {
    // 默认转换响应
    isTransformResponse: true,
    // 默认不返回原始响应
    isReturnNativeResponse: false,
  },
});

// 创建媒体服务的请求实例
export const mediaRequest = axios.create({
  baseURL: API_CONFIG.MEDIA_API.baseURL,
  timeout: API_CONFIG.MEDIA_API.timeout,
});

// 添加响应拦截器
mediaRequest.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Media service error:', error);
    return Promise.reject(error);
  }
); 