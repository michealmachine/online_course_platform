"use client";

import { useState, useEffect } from "react";
import { TeacherService } from "@/services/teacher";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Pagination } from "@/components/common/pagination";

// 定义课程类型
interface Course {
  id: number;
  name: string;
  brief: string;
  // ... 其他课程属性
}

interface TeacherCoursesProps {
  teacherId: number;
  organizationId: number;
}

export function TeacherCourses({ teacherId, organizationId }: TeacherCoursesProps) {
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const [currentPage, setCurrentPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 5;

  const loadCourses = async () => {
    try {
      setLoading(true);
      const data = await TeacherService.getTeacherCourses(teacherId, {
        pageNo: currentPage,
        pageSize
      });
      setCourses(data.items);
      setTotal(data.counts);
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

  useEffect(() => {
    loadCourses();
  }, [teacherId, currentPage]); // 当页码变化时重新加载

  if (loading) {
    return <div className="text-center py-4">加载中...</div>;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>关联课程</CardTitle>
      </CardHeader>
      <CardContent>
        {courses.length > 0 ? (
          <>
            <div className="space-y-4">
              {courses.map(course => (
                <div key={course.id} className="flex justify-between items-center">
                  <div>
                    <h3 className="font-medium">{course.name}</h3>
                    <p className="text-sm text-muted-foreground">{course.brief}</p>
                  </div>
                  <Button 
                    variant="destructive" 
                    size="sm"
                    onClick={() => {/* TODO: 实现解除关联 */}}
                  >
                    解除关联
                  </Button>
                </div>
              ))}
            </div>
            {/* 只有当总数大于每页数量时才显示分页 */}
            {total > pageSize && (
              <div className="mt-4">
                <Pagination
                  total={total}
                  page={currentPage}
                  pageSize={pageSize}
                  onChange={(page) => setCurrentPage(page)}
                />
              </div>
            )}
          </>
        ) : (
          <div className="text-center text-muted-foreground py-4">
            暂无关联课程
          </div>
        )}
      </CardContent>
    </Card>
  );
} 