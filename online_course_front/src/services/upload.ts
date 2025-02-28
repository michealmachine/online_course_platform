import { contentRequest } from './index';
import { API_URLS } from '@/config/api.config';

/**
 * 图片上传服务
 */
export class UploadService {
  /**
   * 上传课程封面到临时存储
   */
  static async uploadCourseLogo(courseId: number, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    
    const url = API_URLS.COURSE.LOGO.TEMP.replace(':courseId', String(courseId));
    const response = await contentRequest.post(url, formData);
    return response.data;  // 返回临时key
  }

  /**
   * 确认保存课程封面
   */
  static async confirmCourseLogo(courseId: number, tempKey: string): Promise<void> {
    const url = API_URLS.COURSE.LOGO.CONFIRM.replace(':courseId', String(courseId));
    return contentRequest.post(url, { tempKey });
  }
} 