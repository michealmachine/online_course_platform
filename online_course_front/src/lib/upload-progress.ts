/**
 * 上传进度接口
 */
export interface UploadProgress {
  fileName: string;
  fileSize: number;
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  uploadedChunks: Array<{
    index: number;
    etag: string;
    md5: string;
  }>;
  lastUpdated: number;
}

/**
 * 上传进度管理类
 */
export class UploadProgressManager {
  private static readonly STORAGE_KEY = 'video_upload_progress';
  private static readonly EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时

  /**
   * 保存上传进度
   */
  static saveProgress(progress: UploadProgress): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(progress));
  }

  /**
   * 获取上传进度
   */
  static getProgress(fileName: string, fileSize: number): UploadProgress | null {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (!saved) return null;

      const progress: UploadProgress = JSON.parse(saved);
      
      // 验证文件信息和过期时间
      if (progress.fileName === fileName && 
          progress.fileSize === fileSize && 
          Date.now() - progress.lastUpdated < this.EXPIRE_TIME) {
        return progress;
      }
      
      // 如果不匹配或已过期，清除存储
      this.clearProgress();
      return null;
    } catch (error) {
      console.error('Error reading upload progress:', error);
      return null;
    }
  }

  /**
   * 清除上传进度
   */
  static clearProgress(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }
} 