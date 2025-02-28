"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { CourseService, type CourseBase } from "@/services/course";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/hooks/use-toast";
import { Badge } from "@/components/ui/badge";
import { COURSE_CHARGE_TYPE } from "@/constants/course";

export default function CourseDetailPage() {
  const params = useParams();
  const courseId = params.id as string;
  const [course, setCourse] = useState<CourseBase | null>(null);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    async function loadCourse() {
      try {
        setLoading(true);
        const data = await CourseService.getCourseDetail(Number(courseId));
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
    }

    loadCourse();
  }, [courseId, toast]);

  if (loading) {
    return (
      <div className="container py-6 space-y-6">
        <Skeleton className="h-8 w-48" />
        <Card>
          <CardContent className="p-6">
            <div className="space-y-2">
              <Skeleton className="h-4 w-1/4" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="p-6">
            <div className="text-center text-muted-foreground">
              课程不存在或已被删除
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{course.name}</h1>
        <div className="flex items-center gap-2">
          {course.mtName && (
            <Badge variant="outline">{course.mtName}</Badge>
          )}
          {course.stName && (
            <Badge variant="outline">{course.stName}</Badge>
          )}
          <Badge variant="outline">
            {course.charge === COURSE_CHARGE_TYPE.FREE ? '免费' : `¥${course.price}`}
          </Badge>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>课程简介</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">{course.brief}</p>
        </CardContent>
      </Card>

      {/* 这里可以添加更多课程相关信息，比如：
          - 课程大纲
          - 课程评价
          - 讲师信息
          等等
      */}
    </div>
  );
} 