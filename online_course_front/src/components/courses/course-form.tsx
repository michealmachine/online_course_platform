"use client";

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { CourseService } from "@/services/course";
import { useToast } from "@/hooks/use-toast";

// 课程表单验证模式
const courseFormSchema = z.object({
  name: z.string()
    .min(2, { message: "课程名称至少2个字符" })
    .max(50, { message: "课程名称最多50个字符" }),
  brief: z.string()
    .min(10, { message: "课程简介至少10个字符" })
    .max(200, { message: "课程简介最多200个字符" }),
  mt: z.string({ required_error: "请选择课程大类" }),
  st: z.string({ required_error: "请选择课程小类" }),
  charge: z.enum(["201001", "201002"], {
    required_error: "请选择收费类型",
  }),
  price: z.number().min(0).optional(),
  organizationId: z.number({ 
    required_error: "请输入机构ID",
    invalid_type_error: "机构ID必须是数字",
  }).min(1, "机构ID必须大于0"),
});

// 课程大类选项
const MT_OPTIONS = [
  { label: "前端开发", value: "1" },
  { label: "后端开发", value: "2" },
  { label: "移动开发", value: "3" },
];

// 课程小类选项
const ST_OPTIONS = {
  "1": [
    { label: "HTML/CSS", value: "101" },
    { label: "JavaScript", value: "102" },
    { label: "React", value: "103" },
    { label: "Vue", value: "104" },
  ],
  "2": [
    { label: "Java", value: "201" },
    { label: "Python", value: "202" },
    { label: "Node.js", value: "203" },
    { label: "Go", value: "204" },
  ],
  "3": [
    { label: "Android", value: "301" },
    { label: "iOS", value: "302" },
    { label: "Flutter", value: "303" },
    { label: "React Native", value: "304" },
  ],
};

interface CourseFormProps {
  organizationId: number;
  courseId?: number;
  onSuccess: (courseId: number) => void;
}

/**
 * 课程表单组件
 * 用于创建和编辑课程信息
 */
export function CourseForm({ organizationId, courseId, onSuccess }: CourseFormProps) {
  const router = useRouter();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [selectedMt, setSelectedMt] = useState<string>("");

  // 创建表单实例
  const form = useForm<z.infer<typeof courseFormSchema>>({
    resolver: zodResolver(courseFormSchema),
    defaultValues: {
      name: "",
      brief: "",
      charge: "201001", // 默认免费
      price: 0,
      organizationId: 1, // 默认机构ID
    },
  });

  // 提交表单
  async function onSubmit(values: z.infer<typeof courseFormSchema>) {
    try {
      setLoading(true);
      const courseId = await CourseService.createCourse(values);
      
      // 显示成功提示
      toast({
        title: "创建成功",
        description: `课程 "${values.name}" 已创建`,
      });

      // 返回课程列表页面
      router.push('/courses');
      router.refresh(); // 刷新列表数据
      onSuccess(courseId);
    } catch (error) {
      // 显示错误提示
      toast({
        title: "创建失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
      console.error("创建课程失败:", error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* 机构ID - 开发阶段临时字段 */}
        <FormField
          control={form.control}
          name="organizationId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>机构ID</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  min="1"
                  placeholder="输入机构ID"
                  {...field}
                  onChange={(e) => field.onChange(Number(e.target.value))}
                />
              </FormControl>
              <FormDescription>
                开发阶段临时字段，用于指定课程所属机构
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 课程名称 */}
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>课程名称</FormLabel>
              <FormControl>
                <Input placeholder="输入课程名称" {...field} />
              </FormControl>
              <FormDescription>
                一个好的课程名称应该简洁明了，能够清晰表达课程内容
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 课程简介 */}
        <FormField
          control={form.control}
          name="brief"
          render={({ field }) => (
            <FormItem>
              <FormLabel>课程简介</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="输入课程简介"
                  className="resize-none"
                  {...field}
                />
              </FormControl>
              <FormDescription>
                简要描述课程的主要内容和学习目标
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 课程大类 */}
        <FormField
          control={form.control}
          name="mt"
          render={({ field }) => (
            <FormItem>
              <FormLabel>课程大类</FormLabel>
              <Select
                onValueChange={(value) => {
                  field.onChange(value);
                  setSelectedMt(value);
                  // 切换大类时，清空小类的选择
                  form.setValue("st", "");
                }}
                defaultValue={field.value}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择课程大类" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {MT_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormDescription>
                选择课程所属的主要技术领域
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 课程小类 */}
        <FormField
          control={form.control}
          name="st"
          render={({ field }) => (
            <FormItem>
              <FormLabel>课程小类</FormLabel>
              <Select
                onValueChange={field.onChange}
                defaultValue={field.value}
                disabled={!selectedMt}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择课程小类" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {selectedMt &&
                    ST_OPTIONS[selectedMt as keyof typeof ST_OPTIONS].map(
                      (option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      )
                    )}
                </SelectContent>
              </Select>
              <FormDescription>
                选择课程的具体技术方向
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 收费类型 */}
        <FormField
          control={form.control}
          name="charge"
          render={({ field }) => (
            <FormItem>
              <FormLabel>收费类型</FormLabel>
              <Select onValueChange={field.onChange} defaultValue={field.value}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="选择收费类型" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="201001">免费</SelectItem>
                  <SelectItem value="201002">收费</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* 课程价格 */}
        {form.watch("charge") === "201002" && (
          <FormField
            control={form.control}
            name="price"
            render={({ field }) => (
              <FormItem>
                <FormLabel>课程价格</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="输入课程价格"
                    {...field}
                    onChange={(e) => field.onChange(Number(e.target.value))}
                  />
                </FormControl>
                <FormDescription>设置合理的课程价格（元）</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        <div className="flex justify-end gap-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.back()}
            disabled={loading}
          >
            取消
          </Button>
          <Button type="submit" disabled={loading}>
            {loading ? "创建中..." : "创建课程"}
          </Button>
        </div>
      </form>
    </Form>
  );
} 