import SparkMD5 from 'spark-md5';

interface UploadChunkMessage {
  type: 'upload';
  chunk: Blob;
  presignedUrl: string;
}

// 计算 MD5
async function calculateMD5(chunk: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsBinaryString(chunk);
    reader.onload = (e) => {
      const binary = e.target?.result;
      if (binary) {
        const md5 = SparkMD5.hash(binary as string);
        resolve(md5);
      } else {
        reject(new Error('Failed to read chunk'));
      }
    };
    reader.onerror = (e) => reject(e);
  });
}

// 上传分片
async function uploadChunk(presignedUrl: string, chunk: Blob) {
  const md5 = await calculateMD5(chunk);
  
  const response = await fetch(presignedUrl, {
    method: 'PUT',
    body: chunk,
    headers: {
      'Content-Type': 'application/octet-stream',
      'Content-MD5': md5
    }
  });

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.status}`);
  }
}

// 监听消息
self.addEventListener('message', async (e: MessageEvent<UploadChunkMessage>) => {
  if (e.data.type === 'upload') {
    try {
      await uploadChunk(e.data.presignedUrl, e.data.chunk);
      self.postMessage({ type: 'success' });
    } catch (error) {
      self.postMessage({ 
        type: 'error', 
        error: error instanceof Error ? error.message : 'Upload failed' 
      });
    }
  }
}); 