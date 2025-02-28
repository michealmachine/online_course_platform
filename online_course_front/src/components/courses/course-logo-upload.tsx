"use client";

import { useState } from "react";
import { ImageUpload } from "@/components/upload/image-upload";
import { ImageCropper } from "@/components/upload/image-cropper";
import { ImagePreview } from "@/components/ui/image-preview";
import { CourseService } from "@/services/course";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import Image from "next/image";
import { getMediaUrl } from "@/lib/utils/url";

interface CourseCoverUploadProps {
  courseId: number;
  value?: string;
  onChange?: (url: string) => void;
}

export function CourseCoverUpload({ courseId, value, onChange }: CourseCoverUploadProps) {
  const [loading, setLoading] = useState(false);
  const [cropperOpen, setCropperOpen] = useState(false);
  const [tempImageUrl, setTempImageUrl] = useState<string>();
  const [tempFile, setTempFile] = useState<File>();
  const [tempKey, setTempKey] = useState<string>();
  const [previewOpen, setPreviewOpen] = useState(false);
  const { toast } = useToast();

  // 处理文件选择
  const handleFileSelect = async (file: File) => {
    // 创建临时URL用于预览和裁剪
    const url = URL.createObjectURL(file);
    setTempImageUrl(url);
    setTempFile(file);
    setCropperOpen(true);
  };

  // 处理裁剪完成
  const handleCropComplete = async (croppedBlob: Blob) => {
    try {
      setLoading(true);
      
      // 将Blob转换为File
      const croppedFile = new File([croppedBlob], tempFile?.name || 'cropped.jpg', {
        type: 'image/jpeg'
      });

      // 上传到临时存储
      const tempKey = await CourseService.uploadCourseLogo(courseId, croppedFile);
      
      // 保存临时key和预览URL
      setTempKey(tempKey);
      const previewUrl = URL.createObjectURL(croppedBlob);
      setTempImageUrl(previewUrl);

      toast({
        title: "上传成功",
        description: "请确认是否使用此图片作为课程封面",
      });
    } catch (error) {
      toast({
        title: "上传失败",
        description: error instanceof Error ? error.message : "上传文件时发生错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 确认使用当前图片
  const handleConfirm = async () => {
    if (!tempKey) return;

    try {
      setLoading(true);
      await CourseService.confirmCourseLogo(courseId, tempKey);
      
      // 更新父组件的值
      onChange?.(tempKey);
      
      // 清理临时状态
      setTempKey(undefined);
      
      toast({
        title: "保存成功",
        description: "课程封面已更新",
      });
    } catch (error) {
      toast({
        title: "保存失败",
        description: error instanceof Error ? error.message : "保存失败",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 重新选择图片
  const handleReselect = () => {
    // 清理之前的临时URL
    if (tempImageUrl) {
      URL.revokeObjectURL(tempImageUrl);
    }
    setTempImageUrl(undefined);
    setTempKey(undefined);
    setTempFile(undefined);
    // 触发文件选择
    document.querySelector<HTMLInputElement>('input[type="file"]')?.click();
  };

  // 删除图片
  const handleDelete = async () => {
    try {
      setLoading(true);
      await CourseService.deleteCourseLogo(courseId);
      onChange?.("");
      setTempImageUrl(undefined);
      setTempKey(undefined);
      toast({
        title: "删除成功",
        description: "课程封面已删除",
      });
    } catch (error) {
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "删除失败",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* 当有临时预览图或已有封面时显示预览和操作按钮 */}
      {tempImageUrl || value ? (
        <div className="space-y-4">
          <div 
            className="relative w-[240px] aspect-video cursor-pointer"
            onClick={() => setPreviewOpen(true)}
          >
            <Image
              src={tempImageUrl || getMediaUrl(value)}
              alt="Course cover"
              fill
              className="object-cover rounded-lg hover:opacity-90 transition-opacity"
            />
          </div>
          <div className="flex gap-2">
            <Button 
              variant="outline" 
              onClick={handleReselect}
              disabled={loading}
            >
              {tempImageUrl ? '更换图片' : '上传新图片'}
            </Button>
            {tempImageUrl && (
              <Button 
                onClick={handleConfirm}
                disabled={loading || !tempKey}
              >
                确认使用
              </Button>
            )}
            {value && !tempImageUrl && (
              <Button 
                variant="destructive"
                onClick={handleDelete}
                disabled={loading}
              >
                删除图片
              </Button>
            )}
          </div>
        </div>
      ) : (
        // 无图片时显示上传组件
        <ImageUpload
          value={value ? getMediaUrl(value) : undefined}
          onChange={onChange}
          onUpload={handleFileSelect}
          onDelete={handleDelete}
          loading={loading}
          aspectRatio={16/9}
          className="w-[240px]"
          maxSize={5 * 1024 * 1024}
          accept="image/jpeg,image/png"
        />
      )}

      {/* 裁剪对话框 */}
      {cropperOpen && tempImageUrl && (
        <ImageCropper
          open={cropperOpen}
          onClose={() => {
            setCropperOpen(false);
            if (!tempKey && tempImageUrl) {
              URL.revokeObjectURL(tempImageUrl);
              setTempImageUrl(undefined);
            }
          }}
          imageUrl={tempImageUrl}
          aspectRatio={16/9}
          onCropComplete={handleCropComplete}
        />
      )}

      {/* 添加图片预览对话框 */}
      {(tempImageUrl || value) && (
        <ImagePreview
          src={tempImageUrl || getMediaUrl(value)}
          alt="Course cover preview"
          open={previewOpen}
          onClose={() => setPreviewOpen(false)}
        />
      )}
    </div>
  );
} 