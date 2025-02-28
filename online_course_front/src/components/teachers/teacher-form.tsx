"use client";

import { useState } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { useToast } from "@/hooks/use-toast";

const teacherFormSchema = z.object({
  name: z.string().min(2, "教师姓名至少2个字符"),
  position: z.string().min(2, "职位至少2个字符"),
  description: z.string().min(10, "简介至少10个字符"),
});

interface TeacherFormProps {
  organizationId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
  initialData?: CourseTeacher;
}

export function TeacherForm({ 
  organizationId, 
  open, 
  onOpenChange, 
  onSuccess,
  initialData 
}: TeacherFormProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const form = useForm<z.infer<typeof teacherFormSchema>>({
    resolver: zodResolver(teacherFormSchema),
    defaultValues: {
      name: initialData?.name || "",
      position: initialData?.position || "",
      description: initialData?.description || "",
    },
  });

  async function onSubmit(values: z.infer<typeof teacherFormSchema>) {
    try {
      setLoading(true);
      if (initialData) {
        // 更新教师
        await TeacherService.updateTeacher({
          ...initialData,
          ...values,
        });
      } else {
        // 创建教师
        await TeacherService.createTeacher({
          ...values,
          organizationId,
        });
      }

      toast({
        title: `${initialData ? "更新" : "添加"}成功`,
        description: `教师 "${values.name}" 已${initialData ? "更新" : "添加"}`,
      });

      onSuccess?.();
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
            {initialData ? "编辑教师" : "添加教师"}
          </DialogTitle>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            {/* 教师姓名 */}
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>教师姓名</FormLabel>
                  <FormControl>
                    <Input placeholder="请输入教师姓名" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 教师职位 */}
            <FormField
              control={form.control}
              name="position"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>教师职位</FormLabel>
                  <FormControl>
                    <Input placeholder="请输入教师职位" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 教师简介 */}
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>教师简介</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="请输入教师简介"
                      className="resize-none"
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
                {initialData ? "更新" : "添加"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}