import SparkMD5 from 'spark-md5';

self.onmessage = async (e: MessageEvent) => {
  try {
    const { chunk, chunkIndex } = e.data;
    
    // 将 Blob 转换为 ArrayBuffer
    const arrayBuffer = await chunk.arrayBuffer();
    
    // 创建 MD5 哈希
    const spark = new SparkMD5.ArrayBuffer();
    spark.append(arrayBuffer);
    
    // 获取十六进制 MD5
    const hexHash = spark.end();
    
    // 将十六进制转换为二进制数组
    const binaryArray = new Uint8Array(16);
    for (let i = 0; i < 32; i += 2) {
      binaryArray[i / 2] = parseInt(hexHash.substr(i, 2), 16);
    }
    
    // 将二进制数组转换为 Base64
    const md5Base64 = btoa(String.fromCharCode.apply(null, Array.from(binaryArray)));
    
    // 发送结果回主线程
    self.postMessage({
      chunkIndex,
      md5Base64,
      success: true
    });
  } catch (error: any) {
    self.postMessage({
      chunkIndex: e.data.chunkIndex,
      error: error.message,
      success: false
    });
  }
}; 