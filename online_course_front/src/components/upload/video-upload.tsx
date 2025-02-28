"use client";

import { useState, useRef, useCallback, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress"; 
import { Upload, Pause, Play, X } from "lucide-react";
import { MediaService } from "@/services/media";
import { UploadProgressManager } from "@/lib/upload-progress";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { UploadStateSchema, type UploadState } from '@/schemas/upload.schema';

// 常量定义
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB 分片大小
const MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB 最大文件大小
const ALLOWED_TYPES = ['video/mp4']; // 允许的文件类型

interface VideoUploadProps {
  organizationId: number;
  onUploadComplete?: (result: { fileUrl: string; mediaFileId: string }) => void;
  onUploadError?: (error: Error) => void;
  maxSize?: number;
  accept?: string;
}

interface UploadState {
  status: 'idle' | 'uploading' | 'paused' | 'completed' | 'error';
  progress: number;
  uploadedChunks: number;
  totalChunks: number;
  currentFile?: File;
  uploadId?: string;
  error?: string;
}

// 修改 MD5 计算任务的类型定义
interface MD5Result {
  index: number;
  md5: string;
}

export function VideoUpload({
  organizationId,
  onUploadComplete,
  onUploadError,
  maxSize = MAX_FILE_SIZE,
  accept = 'video/mp4'
}: VideoUploadProps) {
  // 状态管理
  const [state, setState] = useState<UploadState>({
    status: 'idle',
    progress: 0,
    uploadedChunks: 0,
    totalChunks: 0
  });

  // refs
  const fileInputRef = useRef<HTMLInputElement>(null);
  const abortControllerRef = useRef<AbortController | undefined>(undefined);

  // 添加一个暂停标志
  const [isPaused, setIsPaused] = useState(false);
  const activeUploadsRef = useRef<{ [key: number]: AbortController }>({});

  // 添加已上传分片的跟踪
  const uploadedChunksRef = useRef<Set<number>>(new Set());

  // 添加 worker 引用
  const workerRef = useRef<Worker | null>(null);

  // 添加 MD5 缓存
  const md5CacheRef = useRef<Map<number, string>>(new Map());

  // 添加一个 ref 来跟踪当前上传到的位置
  const currentChunkIndexRef = useRef<number>(0);

  // 添加一个 ref 来跟踪已上传的分片
  const uploadedPartsRef = useRef<{ index: number; etag: string }[]>([]);

  // 初始化 worker
  useEffect(() => {
    if (typeof window !== 'undefined') {
      workerRef.current = new Worker(new URL('../../workers/md5.worker.ts', import.meta.url));
    }
    
    return () => {
      workerRef.current?.terminate();
    };
  }, []);

  // 文件验证
  const validateFile = (file: File): boolean => {
    if (file.size > maxSize) {
      setState(prev => ({ ...prev, error: '文件大小超过限制' }));
      return false;
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      setState(prev => ({ ...prev, error: '不支持的文件类型' }));
      return false;
    }
    return true;
  };

  // 修改 MD5 计算方法，增加缓存
  const calculateMD5 = async (chunk: Blob, chunkIndex: number): Promise<string> => {
    // 检查缓存
    const cachedMD5 = md5CacheRef.current.get(chunkIndex);
    if (cachedMD5) {
      return cachedMD5;
    }

    return new Promise((resolve, reject) => {
      if (!workerRef.current) {
        reject(new Error('Worker not initialized'));
        return;
      }

      const worker = workerRef.current;

      const handleMessage = (e: MessageEvent) => {
        if (e.data.chunkIndex === chunkIndex) {
          worker.removeEventListener('message', handleMessage);
          if (e.data.success) {
            // 存入缓存
            md5CacheRef.current.set(chunkIndex, e.data.md5Base64);
            resolve(e.data.md5Base64);
          } else {
            reject(new Error(e.data.error));
          }
        }
      };

      worker.addEventListener('message', handleMessage);
      worker.postMessage({ chunk, chunkIndex });
    });
  };

  // 更新进度计算
  const updateProgress = (chunkIndex: number, isUploaded: boolean) => {
    if (isUploaded) {
      uploadedChunksRef.current.add(chunkIndex);
    } else {
      uploadedChunksRef.current.delete(chunkIndex);
    }

    const uploadedCount = uploadedChunksRef.current.size;
    setState(prev => ({
      ...prev,
      uploadedChunks: uploadedCount,
      progress: Math.min(Math.round((uploadedCount / prev.totalChunks) * 100), 100)
    }));
  };

  // 处理文件选择
  const handleFileSelect = async (file?: File) => {
    if (!file) return;
    
    if (!validateFile(file)) return;

    console.log('Starting upload with organizationId:', organizationId);
    
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    
    // 重置上传进度跟踪
    uploadedChunksRef.current.clear();

    try {
      const response = await MediaService.initiateUpload(
        organizationId,
        file,
        'VIDEO'
      );

      console.log('Upload initiated:', response);
      setState({
        status: 'uploading',
        progress: 0,
        uploadedChunks: 0,
        totalChunks,
        currentFile: file,
        uploadId: response.uploadId,
        error: undefined
      });

      // 保存上传进度
      UploadProgressManager.saveProgress({
        fileName: file.name,
        fileSize: file.size,
        uploadId: response.uploadId,
        chunkSize: CHUNK_SIZE,
        totalChunks,
        uploadedChunks: [],
        lastUpdated: Date.now()
      });

    } catch (error) {
      console.error('Upload initialization failed:', error);
      setState(prev => ({
        ...prev,
        status: 'error',
        error: '初始化上传失败'
      }));
      onUploadError?.(error as Error);
    }
  };

  // 处理文件拖放
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  }, []);

  // 创建分片
  const createChunks = (file: File): Blob[] => {
    const chunks: Blob[] = [];
    let start = 0;
    while (start < file.size) {
      chunks.push(file.slice(start, start + CHUNK_SIZE));
      start += CHUNK_SIZE;
    }
    return chunks;
  };

  // 修改上传分片的逻辑
  const uploadChunk = async (
    chunk: Blob,
    chunkIndex: number,
    uploadId: string,
    md5Base64: string
  ): Promise<string> => {
    const controller = new AbortController();
    activeUploadsRef.current[chunkIndex] = controller;

    try {
      // 获取预签名 URL
      if (isPaused) throw new Error('PAUSED');
      
      const { presignedUrl } = await MediaService.getPresignedUrl(
        uploadId,
        chunkIndex + 1
      );

      if (isPaused) throw new Error('PAUSED');

      const response = await fetch(presignedUrl, {
        method: 'PUT',
        body: chunk,
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-MD5': md5Base64
        } as HeadersInit,
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error('Upload failed');
      }

      const etag = response.headers.get('ETag')?.replace(/['"]/g, '') || '';
      if (!etag) {
        throw new Error('No ETag received');
      }
      return etag;

    } catch (error) {
      if (error instanceof Error) {
        if (error.name === 'AbortError' || error.message === 'PAUSED') {
          throw new Error('PAUSED');
        }
        // 处理网络错误
        if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
          throw new Error('网络错误，请检查网络连接');
        }
      }
      throw error;
    } finally {
      delete activeUploadsRef.current[chunkIndex];
    }
  };

  // 修改开始上传的逻辑
  const startUpload = async (startFromIndex?: number) => {
    if (!state.currentFile || !state.uploadId) return;

    try {
      setIsPaused(false);
      const chunks = createChunks(state.currentFile);
      
      // 先并发计算所有分片的 MD5
      console.log('Calculating MD5 for all chunks...');
      const md5Tasks = chunks.map(async (chunk, index) => {
        if (isPaused) throw new Error('PAUSED');
        const md5 = await calculateMD5(chunk, index);
        return { index, md5 } as MD5Result;
      });

      const md5Results = await Promise.all(md5Tasks);
      if (isPaused) return;

      // 从指定位置开始上传
      const startIndex = startFromIndex ?? 0;

      // 串行上传分片
      for (let i = startIndex; i < chunks.length && !isPaused; i++) {
        currentChunkIndexRef.current = i;

        // 检查分片是否已上传
        if (uploadedPartsRef.current.some(part => part.index === i)) {
          continue;
        }

        try {
          const etag = await uploadChunk(chunks[i], i, state.uploadId, md5Results[i].md5);
          uploadedPartsRef.current.push({ index: i, etag });
          
          // 更新进度
          setState(prev => ({
            ...prev,
            uploadedChunks: uploadedPartsRef.current.length,
            progress: Math.round((uploadedPartsRef.current.length / chunks.length) * 100)
          }));

        } catch (error) {
          if (error instanceof Error && error.message === 'PAUSED') {
            break;
          }
          throw error;
        }
      }

      // 如果所有分片都上传完成且未暂停，直接完成上传
      if (!isPaused && uploadedPartsRef.current.length === chunks.length) {
        console.log('All chunks uploaded, completing upload...');
        
        const result = await MediaService.completeUpload({
          uploadId: state.uploadId,
          parts: uploadedPartsRef.current
            .sort((a, b) => a.index - b.index)
            .map(part => ({
              partNumber: part.index + 1,
              etag: part.etag
            }))
        });

        setState(prev => ({
          ...prev,
          status: 'completed',
          progress: 100
        }));

        onUploadComplete?.(result);
        
        // 清理状态
        uploadedPartsRef.current = [];
        currentChunkIndexRef.current = 0;
        md5CacheRef.current.clear();
      }

    } catch (error) {
      if (!isPaused) {
        setState(prev => ({
          ...prev,
          status: 'error',
          error: error instanceof Error ? error.message : '上传失败'
        }));
        onUploadError?.(error instanceof Error ? error : new Error('上传失败'));
      }
    }
  };

  // 修改暂停上传的逻辑
  const pauseUpload = () => {
    setIsPaused(true);
    Object.values(activeUploadsRef.current).forEach(controller => {
      try {
        controller.abort();
      } catch (error) {
        console.error('Error aborting upload:', error);
      }
    });
    setState(prev => UploadStateSchema.parse({
      ...prev,
      status: 'paused'
    }));
  };

  // 修改继续上传的逻辑
  const resumeUpload = () => {
    setIsPaused(false);
    setState(prev => ({
      ...prev,
      status: 'uploading'
    }));
    activeUploadsRef.current = {};
    // 从当前位置继续上传
    startUpload(currentChunkIndexRef.current);
  };

  // 修改取消上传的逻辑
  const cancelUpload = async () => {
    try {
      abortControllerRef.current?.abort();
      
      if (state.uploadId) {
        await MediaService.abortUpload(state.uploadId);
        
        // 重置所有状态
        setIsPaused(false);
        currentChunkIndexRef.current = 0;
        md5CacheRef.current.clear();
        uploadedPartsRef.current = [];
        
        setState(prev => ({
          ...prev,
          status: 'error',
          error: '上传已取消'
        }));
        
        UploadProgressManager.clearProgress();
      }
    } catch (error) {
      console.error('Cancel upload failed:', error);
      setState(prev => ({
        ...prev,
        status: 'error',
        error: '取消上传失败'
      }));
    }
  };

  // 在文件选择后自动开始上传
  useEffect(() => {
    if (state.status === 'uploading' && state.currentFile && state.uploadId) {
      startUpload();
    }
  }, [state.status, state.currentFile, state.uploadId]);

  // 在组件卸载时清理缓存
  useEffect(() => {
    return () => {
      md5CacheRef.current.clear();
    };
  }, []);

  // 更新控制按钮部分的渲染
  const renderControls = () => (
    <div className="flex justify-end space-x-2">
      {state.status === 'uploading' ? (
        <Button
          variant="outline"
          size="sm"
          onClick={pauseUpload}
        >
          <Pause className="h-4 w-4 mr-2" />
          暂停
        </Button>
      ) : state.status === 'paused' ? (
        <Button
          variant="outline"
          size="sm"
          onClick={resumeUpload}
        >
          <Play className="h-4 w-4 mr-2" />
          继续
        </Button>
      ) : null}
      <Button
        variant="destructive"
        size="sm"
        onClick={cancelUpload}
      >
        <X className="h-4 w-4 mr-2" />
        取消
      </Button>
    </div>
  );

  // 渲染上传区域
  return (
    <Card className="w-full">
      <div
        className={cn(
          "relative p-6",
          "border-2 border-dashed rounded-lg",
          "flex flex-col items-center justify-center",
          "min-h-[200px]",
          state.status === 'idle' ? "hover:border-primary cursor-pointer" : ""
        )}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onClick={() => state.status === 'idle' && fileInputRef.current?.click()}
      >
        {/* 隐藏的文件输入 */}
        <input
          ref={fileInputRef}
          type="file"
          accept={accept}
          className="hidden"
          onChange={e => handleFileSelect(e.target.files?.[0])}
        />

        {/* 上传状态显示 */}
        {state.status === 'idle' && (
          <div className="text-center">
            <Upload className="mx-auto h-12 w-12 text-muted-foreground" />
            <p className="mt-2">拖拽视频文件到此处或点击上传</p>
            <p className="text-sm text-muted-foreground mt-1">
              支持 MP4 格式，单个文件最大 {Math.round(maxSize / 1024 / 1024)}MB
            </p>
          </div>
        )}

        {/* 进度显示 */}
        {(state.status === 'uploading' || state.status === 'paused') && (
          <div className="w-full">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">
                {state.currentFile?.name}
              </span>
              <span className="text-sm text-muted-foreground">
                {state.progress}%
              </span>
            </div>
            <Progress value={state.progress} className="mb-2" />
            {renderControls()}
          </div>
        )}

        {/* 错误显示 */}
        {state.status === 'error' && (
          <div className="text-center text-destructive">
            <X className="mx-auto h-12 w-12" />
            <p className="mt-2">{state.error}</p>
            {state.error !== '上传已取消' && (
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => setState(prev => ({ ...prev, status: 'idle' }))}
              >
                重试
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  );
}
