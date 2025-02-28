"use client";

import { useState, useEffect } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useToast } from "@/hooks/use-toast";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { contentRequest } from "@/services/index";
import { API_URLS } from "@/config/api.config";
import { CourseService, type TeachplanDTO } from "@/services/course";

const sectionFormSchema = z.object({
  name: z.string().min(2, "名称至少2个字符"),
});

interface SectionFormProps {
  courseId: number;
  chapterId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (newSection: TeachplanDTO) => void;
  initialData?: {
    id?: number;
    name: string;
    orderBy?: number;
  };
}

export function SectionForm({
  courseId,
  chapterId,
  open,
  onOpenChange,
  onSuccess,
  initialData,
}: SectionFormProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const form = useForm<z.infer<typeof sectionFormSchema>>({
    resolver: zodResolver(sectionFormSchema),
    defaultValues: {
      name: initialData?.name || "",
    },
  });

  // 如果是编辑模式，加载原有数据
  useEffect(() => {
    if (initialData) {
      form.setValue('name', initialData.name);
    }
  }, [initialData, form]);

  async function onSubmit(values: z.infer<typeof sectionFormSchema>) {
    try {
      setLoading(true);

      // 如果是新增，获取当前章节的小节列表来计算新的序号
      let orderBy = 1;
      if (!initialData) {
        const data = await CourseService.getTeachplanTree(courseId);
        const currentChapter = data.find(chapter => chapter.id === chapterId);
        if (currentChapter && currentChapter.children) {
          // 找到最大的序号并加1
          orderBy = Math.max(...currentChapter.children.map(section => section.orderBy || 0)) + 1;
        }
      }

      const newSectionId = await CourseService.saveTeachplan({
        id: initialData?.id,
        courseId,
        parentId: chapterId,
        name: values.name,
        level: 2,
        // 如果是编辑，保持原有序号；如果是新增，使用计算的序号
        orderBy: initialData?.orderBy || orderBy,
      });

      const newSection: TeachplanDTO = {
        id: newSectionId,
        name: values.name,
        parentId: chapterId,
        courseId,
        level: 2,
        orderBy: initialData?.orderBy || orderBy,
        children: [],
      };

      toast({
        title: `${initialData ? "更新" : "添加"}成功`,
        description: `小节 "${values.name}" 已${initialData ? "更新" : "添加"}`,
      });

      onSuccess?.(newSection);
      onOpenChange(false);
    } catch (error) {
      toast({
        title: `${initialData ? "更新" : "添加"}失败`,
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {initialData ? "编辑" : "添加"}小节
          </DialogTitle>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            {/* 名称 */}
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>名称</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="请输入小节名称" 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 表单按钮 */}
            <div className="flex justify-end gap-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={loading}
              >
                取消
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? "提交中..." : (initialData ? "更新" : "添加")}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
} 