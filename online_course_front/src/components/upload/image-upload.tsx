"use client";

import { useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Upload, X } from "lucide-react";
import Image from "next/image";
import { cn } from "@/lib/utils";

interface ImageUploadProps {
  value?: string;
  onChange?: (value: string) => void;
  onUpload: (file: File) => Promise<string>;
  onDelete?: () => Promise<void>;
  loading?: boolean;
  aspectRatio?: number;
  className?: string;
  maxSize?: number; // 最大文件大小（字节）
  accept?: string; // 允许的文件类型
}

/**
 * 通用图片上传组件
 * 
 * 支持：
 * 1. 图片预览
 * 2. 拖拽上传
 * 3. 文件大小限制
 * 4. 自定义宽高比
 * 
 * @example
 * <ImageUpload
 *   value={imageUrl}
 *   onChange={setImageUrl}
 *   onUpload={(file) => uploadService.upload(file)}
 *   onConfirm={(key) => uploadService.confirm(key)}
 *   aspectRatio={1}
 * />
 */
export function ImageUpload({
  value,
  onChange,
  onUpload,
  onDelete,
  loading,
  aspectRatio = 16/9,
  className,
  maxSize = 5 * 1024 * 1024, // 默认5MB
  accept = "image/*"
}: ImageUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();

  // 验证文件
  const validateFile = (file: File): boolean => {
    // 检查文件类型
    if (!file.type.startsWith('image/')) {
      toast({
        title: "文件类型错误",
        description: "只能上传图片文件（jpg、png等）",
        variant: "destructive",
      });
      return false;
    }

    // 检查文件大小
    if (file.size > maxSize) {
      toast({
        title: "文件过大",
        description: `图片大小不能超过 ${maxSize / 1024 / 1024}MB`,
        variant: "destructive",
      });
      return false;
    }

    return true;
  };

  // 处理文件选择
  const handleFileSelect = async (file?: File) => {
    if (!file) return;

    if (!validateFile(file)) return;

    try {
      await onUpload(file);
    } catch (error) {
      // 错误已经在 onUpload 中处理过了，这里不需要重复处理
      console.error('Upload failed:', error);
    }
  };

  // 处理拖放
  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  // 处理文件放下
  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const file = e.dataTransfer.files?.[0];
    if (!file) return;

    if (!validateFile(file)) return;

    try {
      await onUpload(file);
    } catch (error) {
      console.error('Upload failed:', error);
    }
  };

  return (
    <div 
      className={cn(
        "relative",
        className
      )}
      style={{ paddingBottom: `${100 / aspectRatio}%` }}
    >
      <div 
        className={cn(
          "absolute inset-0 rounded-lg border border-dashed",
          dragActive ? "border-primary" : "border-border",
          "transition-colors duration-200",
          "flex items-center justify-center"
        )}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
      >
        {/* 隐藏的文件输入框 */}
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          className="hidden"
          onChange={(e) => handleFileSelect(e.target.files?.[0])}
        />

        {value ? (
          // 图片预览
          <div className="relative w-full h-full group">
            <Image
              src={value}
              alt="Uploaded image"
              fill
              className="object-cover rounded-lg"
            />
            {/* 操作按钮 */}
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                disabled={loading}
                onClick={() => inputRef.current?.click()}
              >
                <Upload className="w-4 h-4 mr-2" />
                更换图片
              </Button>
              {onDelete && (
                <Button
                  type="button"
                  variant="destructive"
                  size="sm"
                  disabled={loading}
                  onClick={onDelete}
                >
                  <X className="w-4 h-4 mr-2" />
                  删除图片
                </Button>
              )}
            </div>
          </div>
        ) : (
          // 上传按钮
          <div className="text-center">
            <Button
              type="button"
              variant="outline"
              disabled={loading}
              onClick={() => inputRef.current?.click()}
            >
              <Upload className="w-4 h-4 mr-2" />
              {loading ? "上传中..." : "上传图片"}
            </Button>
            <p className="text-sm text-muted-foreground mt-2">
              支持拖放或点击上传<br />
              仅支持 jpg、png 格式，最大 {maxSize / 1024 / 1024}MB
            </p>
          </div>
        )}
      </div>
    </div>
  );
} 