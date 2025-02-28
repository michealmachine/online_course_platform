"use client";

import { useState, useEffect } from "react";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { AssociateTeacherDialog } from "./associate-teacher-dialog";

interface Props {
  courseId: number;
  organizationId: number;
}

export function CourseTeacherList({ courseId, organizationId }: Props) {
  console.log('CourseTeacherList - organizationId:', organizationId); // 添加日志

  // 如果没有 organizationId，显示错误状态
  if (!organizationId) {
    return (
      <Card>
        <CardContent className="text-center py-6">
          <p className="text-muted-foreground">无法加载教师列表：缺少机构ID</p>
        </CardContent>
      </Card>
    );
  }

  const [teachers, setTeachers] = useState<CourseTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const [showAssociateDialog, setShowAssociateDialog] = useState(false);

  // 加载已关联的教师
  const loadTeachers = async () => {
    try {
      setLoading(true);
      const data = await TeacherService.getTeachersByCourse(courseId);
      setTeachers(data);
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

  // 解除关联
  const handleDissociate = async (teacherId: number) => {
    try {
      await TeacherService.dissociateTeacherFromCourse(
        organizationId,
        courseId,
        teacherId
      );
      toast({
        title: "解除关联成功",
        description: "已解除教师与课程的关联",
      });
      loadTeachers(); // 重新加载列表
    } catch (error) {
      toast({
        title: "解除关联失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  useEffect(() => {
    loadTeachers();
  }, [courseId]);

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={() => setShowAssociateDialog(true)}>
          关联教师
        </Button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">加载中...</div>
      ) : teachers.length === 0 ? (
        <Card>
          <CardContent className="text-center py-6">
            暂无关联教师
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {teachers.map(teacher => (
            <Card key={teacher.id}>
              <CardContent className="flex justify-between items-center p-4">
                <div>
                  <h3 className="font-medium">{teacher.name}</h3>
                  <p className="text-sm text-muted-foreground">
                    {teacher.position}
                  </p>
                </div>
                <Button 
                  variant="destructive" 
                  size="sm"
                  onClick={() => handleDissociate(teacher.id)}
                >
                  解除关联
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <AssociateTeacherDialog
        open={showAssociateDialog}
        onOpenChange={setShowAssociateDialog}
        courseId={courseId}
        organizationId={organizationId}
        currentTeachers={teachers}
        onSuccess={() => {
          loadTeachers();
          setShowAssociateDialog(false);
        }}
      />
    </div>
  );
} 