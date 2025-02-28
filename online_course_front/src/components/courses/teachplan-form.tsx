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
import { CourseService, type TeachplanDTO } from "@/services/course";
import { useToast } from "@/hooks/use-toast";

const teachplanFormSchema = z.object({
  name: z.string().min(2, "名称至少2个字符"),
  orderBy: z.coerce.number().int().min(1, "排序号必须大于0").optional(),
});

interface TeachplanFormProps {
  courseId: number;
  parentId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (teachplan: TeachplanDTO) => void;
  initialData?: TeachplanDTO;
}

export function TeachplanForm({
  courseId,
  parentId,
  open,
  onOpenChange,
  onSuccess,
  initialData,
}: TeachplanFormProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const form = useForm<z.infer<typeof teachplanFormSchema>>({
    resolver: zodResolver(teachplanFormSchema),
    defaultValues: {
      name: initialData?.name || "",
      orderBy: initialData?.orderBy,
    },
  });

  async function onSubmit(values: z.infer<typeof teachplanFormSchema>) {
    try {
      setLoading(true);
      const newTeachplanId = await CourseService.saveTeachplan({
        id: initialData?.id,
        courseId,
        parentId,
        name: values.name,
        level: parentId === 0 ? 1 : 2,
        orderBy: values.orderBy,
      });

      // 构造新的课程计划对象
      const newTeachplan: TeachplanDTO = {
        id: newTeachplanId,
        name: values.name,
        parentId,
        courseId,
        level: parentId === 0 ? 1 : 2,
        orderBy: values.orderBy || 1,
        children: [],
      };

      toast({
        title: `${initialData ? "更新" : "添加"}成功`,
        description: `${parentId === 0 ? "章节" : "小节"} "${values.name}" 已${initialData ? "更新" : "添加"}`,
      });

      onSuccess?.(newTeachplan);
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
            {initialData ? "编辑" : "添加"}
            {parentId === 0 ? "章节" : "小节"}
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
                      placeholder={`请输入${parentId === 0 ? "章节" : "小节"}名称`} 
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 排序号 */}
            <FormField
              control={form.control}
              name="orderBy"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>排序号</FormLabel>
                  <FormControl>
                    <Input 
                      type="number"
                      placeholder="请输入排序号（可选）" 
                      {...field}
                      value={field.value || ""}
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