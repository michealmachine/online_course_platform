import { mediaRequest } from './index';
import { API_URLS } from '@/config/api.config';

// 媒体类型
export type MediaType = 'IMAGE' | 'VIDEO';

// 初始化上传请求
export interface InitiateMultipartUploadRequest {
  fileName: string;
  fileSize: number;
  mediaType: MediaType;
  mimeType?: string;
  purpose?: string;
  organizationId?: number;
}

// 初始化上传响应
export interface InitiateMultipartUploadResponse {
  uploadId: string;
  key: string;
  bucket: string;
}

// 获取预签名URL请求
export interface GetPresignedUrlRequest {
  uploadId: string;
  chunkIndex: number;
}

// 获取预签名URL响应
export interface GetPresignedUrlResponse {
  presignedUrl: string;
  expirationTime: number;
}

// 完成上传请求
export interface CompleteMultipartUploadRequest {
  uploadId: string;
  parts: Array<{
    partNumber: number;
    etag: string;
  }>;
}

// 完成上传响应
export interface CompleteMultipartUploadResponse {
  fileUrl: string;
  mediaFileId: string;
}

// 上传状态响应
export interface UploadStatusResponse {
  uploadId: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABORTED';
  uploadedParts: Array<{
    partNumber: number;
    etag: string;
  }>;
}

/**
 * 媒体文件信息接口
 */
export interface MediaFile {
  mediaFileId: string;
  fileName: string;
  fileSize: number;
  url: string;
  fileType: string;
  purpose: string;
  status: string;
}

/**
 * 完成上传请求接口
 */
export interface CompleteUploadRequest {
  uploadId: string;
  parts?: Array<{
    partNumber: number;
    etag?: string;
  }>;
}

/**
 * 媒体服务类
 */
export class MediaService {
  /**
   * 初始化分片上传
   */
  static async initiateUpload(
    organizationId: number,
    file: File,
    mediaType: MediaType,
    purpose?: string
  ): Promise<InitiateMultipartUploadResponse> {
    const request: InitiateMultipartUploadRequest = {
      fileName: file.name,
      fileSize: file.size,
      mediaType,
      mimeType: file.type,
      purpose,
      organizationId
    };

    console.log('Initiating upload with request:', {
      url: API_URLS.MEDIA_UPLOAD.INITIATE,
      params: { organizationId }, // URL参数
      body: request // 请求体
    });

    const response = await mediaRequest.post<InitiateMultipartUploadResponse>(
      API_URLS.MEDIA_UPLOAD.INITIATE,
      request,
      {
        params: { organizationId } // 添加URL参数
      }
    );

    console.log('Upload initiated response:', response.data);
    return response.data;
  }

  /**
   * 获取分片上传的预签名URL
   */
  static async getPresignedUrl(
    uploadId: string,
    partNumber: number
  ): Promise<GetPresignedUrlResponse> {
    const params: GetPresignedUrlRequest = {
      uploadId,
      chunkIndex: partNumber
    };

    console.log('Getting presigned URL:', {
      url: API_URLS.MEDIA_UPLOAD.PRESIGNED_URL,
      params,
      headers: mediaRequest.defaults.headers
    });

    try {
      const response = await mediaRequest.get<GetPresignedUrlResponse>(
        API_URLS.MEDIA_UPLOAD.PRESIGNED_URL,
        { params }
      );

      console.log('Presigned URL response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Failed to get presigned URL:', error);
      throw error;
    }
  }

  /**
   * 完成分片上传
   */
  static async completeUpload(
    request: CompleteMultipartUploadRequest
  ): Promise<CompleteMultipartUploadResponse> {
    console.log('Completing upload:', {
      url: API_URLS.MEDIA_UPLOAD.COMPLETE,
      body: request
    });

    const response = await mediaRequest.post<CompleteMultipartUploadResponse>(
      API_URLS.MEDIA_UPLOAD.COMPLETE,
      request
    );

    console.log('Complete upload response:', response.data);
    return response.data;
  }

  /**
   * 取消分片上传
   */
  static async abortUpload(uploadId: string): Promise<void> {
    await mediaRequest.post(
      API_URLS.MEDIA_UPLOAD.ABORT,
      null,
      { params: { uploadId } }
    );
  }

  /**
   * 获取上传状态
   */
  static async getUploadStatus(uploadId: string): Promise<UploadStatusResponse> {
    const response = await mediaRequest.get<UploadStatusResponse>(
      API_URLS.MEDIA_UPLOAD.STATUS,
      { params: { uploadId } }
    );

    return response.data;
  }

  /**
   * 上传课程封面
   */
  static async uploadCourseLogo(courseId: number, organizationId: number, file: File) {
    const url = API_URLS.MEDIA.UPLOAD_LOGO.replace(':courseId', String(courseId));
    const formData = new FormData();
    formData.append('file', file);
    formData.append('organizationId', String(organizationId));

    const response = await mediaRequest.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response;
  }

  /**
   * 删除媒体文件
   */
  static async deleteMediaFile(url: string) {
    const encodedUrl = encodeURIComponent(url);
    const apiUrl = API_URLS.MEDIA.DELETE.replace(':url', encodedUrl);
    return mediaRequest.delete(apiUrl);
  }
} 