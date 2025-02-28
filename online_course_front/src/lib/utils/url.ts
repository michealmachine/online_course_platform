/**
 * 获取媒体文件的完整访问URL
 * @param key 媒体文件的key
 * @returns 完整的访问URL
 */
export function getMediaUrl(key?: string): string {
  if (!key) return '';
  
  // 如果已经是完整URL，直接返回
  if (key.startsWith('http://') || key.startsWith('https://')) {
    return key;
  }

  // Minio 直接访问地址
  return `http://localhost:8999${key}`;
} 