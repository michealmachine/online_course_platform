"use client";

import { useState } from "react";
import { ImageUpload } from "@/components/upload/image-upload";
import { CourseService } from "@/services/course";
import { useToast } from "@/hooks/use-toast";

interface CourseCoverUploadProps {
  courseId: number;
  value?: string;
  onChange?: (url: string) => void;
}

export function CourseCoverUpload({ courseId, value, onChange }: CourseCoverUploadProps) {
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const handleUpload = async (file: File): Promise<string> => {
    try {
      setLoading(true);
      // 1. 上传到临时存储
      const tempKey = await CourseService.uploadCourseLogo(courseId, file);
      
      // 2. 确认保存
      await CourseService.confirmCourseLogo(courseId, tempKey);
      
      // 3. 更新URL
      onChange?.(tempKey);
      
      toast({
        title: "上传成功",
        description: "课程封面已更新",
      });

      return tempKey;
    } catch (error) {
      toast({
        title: "上传失败",
        description: "请稍后重试",
        variant: "destructive",
      });
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    try {
      setLoading(true);
      await CourseService.deleteCourseLogo(courseId);
      onChange?.("");
      toast({
        title: "删除成功",
        description: "课程封面已删除",
      });
    } catch (error) {
      toast({
        title: "删除失败",
        description: "请稍后重试",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ImageUpload
      value={value}
      onChange={onChange}
      onUpload={handleUpload}
      onDelete={handleDelete}
      loading={loading}
      aspectRatio={16/9}
      className="w-[240px]"
      maxSize={2 * 1024 * 1024} // 2MB
    />
  );
} 