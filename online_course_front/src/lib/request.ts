import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

/**
 * 响应结果接口
 */
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

/**
 * 自定义错误类
 */
export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * 创建axios实例的配置接口
 */
export interface CreateAxiosOptions extends AxiosRequestConfig {
  requestOptions?: {
    /**
     * 是否返回原始响应头
     */
    isReturnNativeResponse?: boolean;
    /**
     * 是否直接获取响应数据
     */
    isTransformResponse?: boolean;
    /**
     * 是否加入时间戳
     */
    joinTime?: boolean;
    /**
     * 是否忽略重复请求
     */
    ignoreCancelToken?: boolean;
  };
}

/**
 * 封装的请求类
 */
export class Request {
  private axiosInstance: AxiosInstance;
  private readonly options: CreateAxiosOptions;

  constructor(options: CreateAxiosOptions) {
    this.options = options;
    this.axiosInstance = axios.create(options);
    this.setupInterceptors();
  }

  /**
   * 设置拦截器
   */
  private setupInterceptors() {
    // 请求拦截器
    this.axiosInstance.interceptors.request.use(
      (config) => {
        // 添加请求日志
        console.log('🚀 Request:', {
          method: config.method?.toUpperCase(),
          url: config.baseURL + config.url,
          params: config.params,
          data: config.data,
        });
        return config;
      },
      (error) => {
        console.error('❌ Request Error:', error);
        return Promise.reject(error);
      }
    );

    // 响应拦截器
    this.axiosInstance.interceptors.response.use(
      (response) => {
        // 添加响应日志
        console.log('✅ Response:', {
          url: response.config.url,
          status: response.status,
          data: response.data,
        });

        const { data } = response;
        
        // 如果需要返回原始响应
        if (this.options.requestOptions?.isReturnNativeResponse) {
          return response;
        }

        // 如果不需要转换响应
        if (this.options.requestOptions?.isTransformResponse === false) {
          return data;
        }

        // 处理业务状态码
        if (data.code !== 0) {
          throw new ApiError(data.code, data.message, data.data);
        }

        // 正常返回数据
        return data.data;
      },
      (error) => {
        // 添加错误日志
        console.error('❌ Response Error:', {
          url: error.config?.url,
          status: error.response?.status,
          message: error.message,
          data: error.response?.data,
        });
        
        const message = error.response?.data?.message || error.message;
        const code = error.response?.status || 500;
        throw new ApiError(code, message);
      }
    );
  }

  /**
   * 发送请求的通用方法
   */
  request<T = any>(config: AxiosRequestConfig): Promise<T> {
    return this.axiosInstance.request(config);
  }

  /**
   * GET请求
   */
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.request({ ...config, method: 'GET', url });
  }

  /**
   * POST请求
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.request({ ...config, method: 'POST', url, data });
  }

  /**
   * PUT请求
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.request({ ...config, method: 'PUT', url, data });
  }

  /**
   * DELETE请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.request({ ...config, method: 'DELETE', url });
  }
}

/**
 * 创建请求实例
 */
const createRequest = (options: CreateAxiosOptions) => {
  return new Request(options);
};

export default createRequest; 