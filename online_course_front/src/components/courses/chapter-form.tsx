"use client";

import { useState, useEffect } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { CourseService, type TeachplanDTO } from "@/services/course";
import { useToast } from "@/hooks/use-toast";

const formSchema = z.object({
  name: z.string().min(2, "名称至少2个字符"),
  // 移除 orderBy 的验证，因为我们会自动处理
});

interface ChapterFormProps {
  courseId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (newChapter: TeachplanDTO) => void;
  initialData?: {
    id?: number;
    name: string;
    orderBy?: number;
  };
}

export function ChapterForm({
  courseId,
  open,
  onOpenChange,
  onSuccess,
  initialData,
}: ChapterFormProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
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

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setLoading(true);

      // 如果是新增，获取当前课程的章节列表来计算新的序号
      let orderBy = 1;
      if (!initialData) {
        const data = await CourseService.getTeachplanTree(courseId);
        if (data.length > 0) {
          // 找到最大的序号并加1
          orderBy = Math.max(...data.map(chapter => chapter.orderBy || 0)) + 1;
        }
      }

      const newChapterId = await CourseService.saveTeachplan({
        id: initialData?.id,
        courseId,
        parentId: 0,
        name: values.name,
        level: 1,
        // 如果是编辑，保持原有序号；如果是新增，使用计算的序号
        orderBy: initialData?.orderBy || orderBy,
      });

      const newChapter: TeachplanDTO = {
        id: newChapterId,
        name: values.name,
        parentId: 0,
        courseId,
        level: 1,
        orderBy: initialData?.orderBy || orderBy,
        children: [],
      };

      toast({
        title: `${initialData ? "更新" : "添加"}成功`,
        description: `章节 "${values.name}" 已${initialData ? "更新" : "添加"}`,
      });

      onSuccess?.(newChapter);
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
            {initialData ? "编辑" : "添加"}章节
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
                      placeholder="请输入章节名称" 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 移除排序号输入框，改为自动计算 */}

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