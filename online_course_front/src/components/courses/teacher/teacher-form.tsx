"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { Button } from "@/components/ui/button";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { useToast } from "@/hooks/use-toast";

/**
 * 教师表单验证模式
 */
const teacherFormSchema = z.object({
  teacherName: z
    .string()
    .min(2, { message: "教师姓名至少2个字符" })
    .max(50, { message: "教师姓名最多50个字符" }),
  position: z
    .string()
    .min(2, { message: "职位至少2个字符" })
    .max(50, { message: "职位最多50个字符" }),
  introduction: z
    .string()
    .min(10, { message: "简介至少10个字符" })
    .max(500, { message: "简介最多500个字符" }),
  photograph: z.string().optional(),
});

type TeacherFormValues = z.infer<typeof teacherFormSchema>;

interface TeacherFormProps {
  courseId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialData?: CourseTeacher;
  onSuccess?: () => void;
}

/**
 * 课程教师表单组件
 * 
 * 用于添加和编辑课程教师信息
 * 包含：教师姓名、职位、简介、照片等字段
 * 
 * @example
 * <TeacherForm
 *   courseId={1}
 *   open={showForm}
 *   onOpenChange={setShowForm}
 *   initialData={editingTeacher}
 *   onSuccess={() => {
 *     setShowForm(false);
 *     refreshList();
 *   }}
 * />
 */
export function TeacherForm({
  courseId,
  open,
  onOpenChange,
  initialData,
  onSuccess,
}: TeacherFormProps) {
  const { toast } = useToast();

  // 创建表单实例
  const form = useForm<TeacherFormValues>({
    resolver: zodResolver(teacherFormSchema),
    defaultValues: {
      teacherName: initialData?.teacherName || "",
      position: initialData?.position || "",
      introduction: initialData?.introduction || "",
      photograph: initialData?.photograph || "",
    },
  });

  // 提交表单
  const onSubmit = async (values: TeacherFormValues) => {
    try {
      await TeacherService.saveTeacher({
        ...values,
        courseId,
        id: initialData?.id,
      });

      toast({
        title: `${initialData ? "更新" : "添加"}成功`,
        description: `教师 "${values.teacherName}" 已${initialData ? "更新" : "添加"}`,
      });

      onSuccess?.();
      onOpenChange(false);
    } catch (error) {
      toast({
        title: `${initialData ? "更新" : "添加"}失败`,
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

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
              name="teacherName"
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
              name="introduction"
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

            {/* 教师照片 - TODO: 后续添加图片上传功能 */}
            <FormField
              control={form.control}
              name="photograph"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>教师照片</FormLabel>
                  <FormControl>
                    <Input placeholder="请输入照片URL" {...field} />
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
              >
                取消
              </Button>
              <Button type="submit">
                {initialData ? "更新" : "添加"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
} 