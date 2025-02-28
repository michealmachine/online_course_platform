export interface MediaFile {
  key: string;
  url: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
}

/** 初始化分片上传请求 */
export interface InitiateMultipartUploadRequest {
  fileName: string;
  fileSize: number;
  mediaType: 'IMAGE' | 'VIDEO';
  mimeType?: string;
  purpose?: string;
  organizationId?: number;
}

/** 初始化分片上传响应 */
export interface InitiateMultipartUploadResponse {
  uploadId: string;
  mediaFileId: string;
  bucket: string;
  filePath: string;
  chunkSize: number;
  totalChunks: number;
}

/** 获取预签名URL请求 */
export interface GetPresignedUrlRequest {
  uploadId: string;
  chunkIndex: number;
}

/** 获取预签名URL响应 */
export interface GetPresignedUrlResponse {
  presignedUrl: string;
  chunkIndex: number;
  expirationTime: number;
}

/** 完成分片上传请求 */
export interface CompleteMultipartUploadRequest {
  uploadId: string;
  parts?: Array<{
    partNumber: number;
    etag?: string;
  }>;
}

/** 完成分片上传响应 */
export interface CompleteMultipartUploadResponse {
  mediaFileId: string;
  fileUrl: string;
  fileSize: number;
  status: string;
  completeTime: number;
}

export interface MediaFileDTO {
  mediaFileId: string;
  fileName: string;
  fileSize: number;
  url: string;
  fileType: string;
  purpose: string;
  status: string;
} 