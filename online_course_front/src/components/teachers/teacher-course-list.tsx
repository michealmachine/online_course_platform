"use client";

import { useState, useEffect } from "react";
import { TeacherService } from "@/services/teacher";
import { Card, CardContent } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/common/pagination";
import Link from "next/link";

interface Props {
  teacherId: number;
  organizationId: number;
}

export function TeacherCourseList({ teacherId, organizationId }: Props) {
  const [courses, setCourses] = useState<any[]>([]);
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
  }, [teacherId, currentPage]);

  if (loading) {
    return <div className="text-center py-4">加载中...</div>;
  }

  return (
    <div className="space-y-4">
      {courses.length === 0 ? (
        <Card>
          <CardContent className="text-center py-6">
            暂无关联课程
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="space-y-4">
            {courses.map(course => (
              <Card key={course.id}>
                <CardContent className="flex justify-between items-center p-4">
                  <div>
                    <h3 className="font-medium">{course.name}</h3>
                    <p className="text-sm text-muted-foreground">
                      {course.brief}
                    </p>
                  </div>
                  <Link 
                    href={`/courses/${course.id}/edit?organizationId=${organizationId}`}
                  >
                    <Button size="sm">
                      查看课程
                    </Button>
                  </Link>
                </CardContent>
              </Card>
            ))}
          </div>

          <Pagination
            total={total}
            page={currentPage}
            pageSize={pageSize}
            onChange={setCurrentPage}
          />
        </>
      )}
    </div>
  );
} 