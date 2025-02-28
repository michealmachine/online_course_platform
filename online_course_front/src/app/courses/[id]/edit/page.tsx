"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { CourseChapterList } from "@/components/courses/course-chapter-list";
import { contentRequest } from "@/services/index";
import { API_URLS } from "@/config/api.config";
import { CourseTeacherList } from "@/components/courses/course-teacher-list";
import { CourseService } from "@/services/course";
import { COURSE_STATUS } from "@/services/course";
import { CourseCoverUpload } from "@/components/courses/course-logo-upload";

/**
 * 课程基本信息接口
 */
interface CourseBase {
  id: number;
  name: string;
  brief: string;
  mt: string;
  mtName?: string;
  st: string;
  stName?: string;
  charge: string;
  price: number;
  logo?: string;
  valid: boolean;
  status: string;
}

/**
 * 课程编辑页面
 * 包含：
 * 1. 课程基本信息展示
 * 2. 课程计划管理
 * 3. 课程教师管理（待实现）
 * 4. 课程审核提交（待实现）
 */
export default function CourseEditPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const { toast } = useToast();
  const [course, setCourse] = useState<CourseBase | null>(null);
  const [loading, setLoading] = useState(true);
  const courseId = Number(params.id);
  const organizationId = Number(searchParams.get('organizationId'));

  console.log('编辑页面 - organizationId:', organizationId); // 添加日志

  useEffect(() => {
    // 如果没有 organizationId，重定向到列表页
    if (!organizationId) {
      router.push('/organization/courses');
      return;
    }
  }, [organizationId, router]);

  // 加载课程信息
  useEffect(() => {
    loadCourse();
  }, [courseId]);

  const loadCourse = async () => {
    try {
      setLoading(true);
      const url = API_URLS.COURSE.DETAIL.replace(':id', String(courseId));
      const data = await contentRequest.get<CourseBase>(url);
      setCourse(data);
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

  // 提交审核
  const handleSubmitAudit = async () => {
    try {
      await CourseService.submitForAudit(courseId);
      toast({
        title: "提交成功",
        description: "课程已提交审核",
      });
      router.refresh();
    } catch (error) {
      toast({
        title: "提交失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  // 重新提交审核
  const handleResubmit = async () => {
    try {
      await CourseService.resubmitForAudit(courseId);
      toast({
        title: "提交成功",
        description: "课程已重新提交审核",
      });
      router.refresh();
    } catch (error) {
      toast({
        title: "提交失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center text-muted-foreground">加载中...</div>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center text-muted-foreground">课程不存在</div>
      </div>
    );
  }

  return (
    <div className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">编辑课程</h1>
        {course.status === COURSE_STATUS.DRAFT && (
          <Button
            onClick={async () => {
              try {
                await CourseService.submitForAudit(course.id);
                toast({
                  title: "提交成功",
                  description: "课程已提交审核",
                });
                router.refresh();
              } catch (error) {
                toast({
                  title: "提交失败",
                  description: error instanceof Error ? error.message : "未知错误",
                  variant: "destructive",
                });
              }
            }}
          >
            提交审核
          </Button>
        )}
      </div>

      {/* 课程内容管理标签页 */}
      <Tabs defaultValue="basic">
        <TabsList>
          <TabsTrigger value="basic">基本信息</TabsTrigger>
          <TabsTrigger value="cover">封面管理</TabsTrigger>
          <TabsTrigger value="chapters">课程计划</TabsTrigger>
          <TabsTrigger value="teacher">课程教师</TabsTrigger>
        </TabsList>

        {/* 基本信息 */}
        <TabsContent value="basic" className="mt-6">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle>{course.name}</CardTitle>
                  <CardDescription className="mt-2">
                    {course.brief}
                  </CardDescription>
                </div>
                <Button onClick={handleSubmitAudit}>提交审核</Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">课程分类：</span>
                  <span>{course.mtName} - {course.stName}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">收费类型：</span>
                  <span>{course.charge === "201001" ? "免费" : `¥${course.price}`}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* 封面管理 */}
        <TabsContent value="cover" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>课程封面</CardTitle>
              <CardDescription>
                上传课程封面图片，建议尺寸 1920x1080px (16:9)，支持 jpg、png 格式，最大 2MB
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="max-w-[600px] mx-auto">
                <CourseCoverUpload
                  courseId={Number(params.id)}
                  value={course?.logo}
                  onChange={async (url) => {
                    setCourse(prev => prev ? ({
                      ...prev,
                      logo: url
                    }) : prev);
                  }}
                />
                {course?.logo && (
                  <p className="text-sm text-muted-foreground mt-4">
                    当前封面图片: {course.logo}
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* 课程计划 */}
        <TabsContent value="chapters" className="mt-6">
          <CourseChapterList 
            courseId={courseId} 
            organizationId={1}  // TODO: 从用户上下文获取机构ID
          />
        </TabsContent>

        {/* 课程教师 */}
        <TabsContent value="teacher" className="mt-6">
          <CourseTeacherList 
            courseId={courseId}
            organizationId={organizationId}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
} 