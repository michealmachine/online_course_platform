"use client";

import { useState, useEffect } from "react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { 
  CourseService, 
  COURSE_CHARGE_LABELS,
  COURSE_CHARGE,
  COURSE_AUDIT_STATUS
} from "@/services/course";
import { useToast } from "@/hooks/use-toast";
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
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { DialogFooter } from "@/components/ui/dialog";
import { CoursePreviewDTO, TeachplanDTO, CourseTeacherDTO } from "@/types/course";

const auditFormSchema = z.object({
  auditStatus: z.enum(["pass", "reject"]),
  auditMessage: z.string().min(1, "驳回时必须填写审核意见").optional(),
});

interface Props {
  courseId: number;
  courseName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

interface CoursePreviewDTO {
  courseBase: CourseBase;
  teachplans: TeachplanDTO[];
  teachers: CourseTeacherDTO[];
}

export function CourseAuditDialog({
  courseId,
  courseName,
  open,
  onOpenChange,
  onSuccess,
}: Props) {
  const [loading, setLoading] = useState(false);
  const [courseDetail, setCourseDetail] = useState<CoursePreviewDTO | null>(null);
  const { toast } = useToast();
  
  const form = useForm<z.infer<typeof auditFormSchema>>({
    resolver: zodResolver(auditFormSchema),
    defaultValues: {
      auditStatus: "pass",
      auditMessage: "",
    },
  });

  // 加载课程详情
  useEffect(() => {
    if (open && courseId) {
      loadCourseDetail();
    }
  }, [open, courseId]);

  const loadCourseDetail = async () => {
    try {
      setLoading(true);
      const detail = await CourseService.getAuditCourseDetail(courseId);
      setCourseDetail(detail);
    } catch (error) {
      toast({
        title: "加载失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: z.infer<typeof auditFormSchema>) => {
    try {
      setLoading(true);
      await CourseService.auditCourse({
        courseId,
        auditStatus: data.auditStatus === "pass" ? COURSE_AUDIT_STATUS.APPROVED : COURSE_AUDIT_STATUS.REJECTED,
        auditMessage: data.auditMessage,
      });
      
      toast({
        title: "审核成功",
        description: `课程「${courseName}」已${data.auditStatus === "pass" ? "通过" : "驳回"}审核`,
      });
      
      onSuccess?.();
      onOpenChange(false);
    } catch (error) {
      toast({
        title: "审核失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>课程审核 - {courseName}</DialogTitle>
        </DialogHeader>
        
        {loading ? (
          <div className="py-8 text-center">加载中...</div>
        ) : courseDetail ? (
          <div className="space-y-6">
            {/* 课程基本信息 */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">基本信息</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">课程名称：</span>
                  {courseDetail.courseBase.name}
                </div>
                <div>
                  <span className="text-muted-foreground">课程分类：</span>
                  {courseDetail.courseBase.mtName} / {courseDetail.courseBase.stName}
                </div>
                <div className="col-span-2">
                  <span className="text-muted-foreground">课程简介：</span>
                  {courseDetail.courseBase.brief}
                </div>
                <div>
                  <span className="text-muted-foreground">收费规则：</span>
                  {COURSE_CHARGE_LABELS[courseDetail.courseBase.charge] || courseDetail.courseBase.charge}
                </div>
                {courseDetail.courseBase.charge === COURSE_CHARGE.CHARGE && courseDetail.courseBase.price && (
                  <div>
                    <span className="text-muted-foreground">课程价格：</span>
                    ¥{courseDetail.courseBase.price}
                  </div>
                )}
              </div>
            </div>

            {/* 课程计划 */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">课程计划</h3>
              <div className="space-y-2">
                {courseDetail.teachplans.map(chapter => (
                  <div key={chapter.id} className="space-y-2">
                    <div className="font-medium">{chapter.name}</div>
                    {chapter.children?.map(section => (
                      <div key={section.id} className="ml-4 text-sm">
                        • {section.name}
                      </div>
                    ))}
                  </div>
                ))}
                {courseDetail.teachplans.length === 0 && (
                  <div className="text-muted-foreground text-sm">暂无课程计划</div>
                )}
              </div>
            </div>

            {/* 课程教师 */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">授课教师</h3>
              <div className="grid grid-cols-2 gap-4">
                {courseDetail.teachers.map(teacher => (
                  <div key={teacher.id} className="p-4 border rounded-lg">
                    <div className="flex items-center gap-3">
                      {teacher.avatar && (
                        <img 
                          src={teacher.avatar} 
                          alt={teacher.name}
                          className="w-12 h-12 rounded-full object-cover"
                        />
                      )}
                      <div>
                        <div className="font-medium">{teacher.name}</div>
                        <div className="text-sm text-muted-foreground">
                          {teacher.position}
                        </div>
                      </div>
                    </div>
                    {teacher.description && (
                      <div className="mt-2 text-sm text-muted-foreground">
                        {teacher.description}
                      </div>
                    )}
                  </div>
                ))}
                {courseDetail.teachers.length === 0 && (
                  <div className="text-muted-foreground text-sm">暂无教师信息</div>
                )}
              </div>
            </div>

            {/* 审核表单 */}
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                <FormField
                  control={form.control}
                  name="auditStatus"
                  render={({ field }) => (
                    <FormItem className="space-y-3">
                      <FormLabel>审核结果</FormLabel>
                      <FormControl>
                        <RadioGroup
                          onValueChange={field.onChange}
                          defaultValue={field.value}
                          className="flex flex-col space-y-1"
                        >
                          <FormItem className="flex items-center space-x-3 space-y-0">
                            <FormControl>
                              <RadioGroupItem value="pass" />
                            </FormControl>
                            <FormLabel className="font-normal">
                              通过
                            </FormLabel>
                          </FormItem>
                          <FormItem className="flex items-center space-x-3 space-y-0">
                            <FormControl>
                              <RadioGroupItem value="reject" />
                            </FormControl>
                            <FormLabel className="font-normal">
                              驳回
                            </FormLabel>
                          </FormItem>
                        </RadioGroup>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="auditMessage"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>审核意见</FormLabel>
                      <FormControl>
                        <Textarea
                          placeholder="请输入审核意见（驳回时必填）"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="flex justify-end space-x-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => onOpenChange(false)}
                    disabled={loading}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={loading}>
                    {loading ? "提交中..." : "确认"}
                  </Button>
                </div>
              </form>
            </Form>
          </div>
        ) : (
          <div className="py-8 text-center text-muted-foreground">
            加载失败,请重试
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
} 