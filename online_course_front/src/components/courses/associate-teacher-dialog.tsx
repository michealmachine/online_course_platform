"use client";

import { useState, useEffect } from "react";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { useToast } from "@/hooks/use-toast";
import { Card, CardContent } from "@/components/ui/card";

interface Props {
  courseId: number;
  organizationId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
  currentTeachers: CourseTeacher[];
}

export function AssociateTeacherDialog({
  courseId,
  organizationId,
  open,
  onOpenChange,
  onSuccess,
  currentTeachers
}: Props) {
  const [teachers, setTeachers] = useState<CourseTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  // 加载机构的所有教师
  const loadTeachers = async () => {
    try {
      setLoading(true);
      const data = await TeacherService.getTeacherList(organizationId);
      setTeachers(data.items);
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

  // 关联教师
  const handleAssociate = async (teacherId: number) => {
    try {
      await TeacherService.associateTeacherToCourse(
        organizationId,
        courseId,
        teacherId
      );
      toast({
        title: "关联成功",
        description: "已将教师关联到课程",
      });
      onSuccess?.();
    } catch (error) {
      console.error('关联教师失败:', error);
      toast({
        title: "关联失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  // 对话框打开时加载教师列表
  useEffect(() => {
    if (open) {
      loadTeachers();
    }
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>关联教师</DialogTitle>
          <DialogDescription>
            选择要关联到课程的教师
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="text-center py-8 text-muted-foreground">加载中...</div>
        ) : teachers.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            暂无可关联的教师
          </div>
        ) : (
          <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
            {teachers.map(teacher => {
              // 检查教师是否已关联
              const isAssociated = currentTeachers.some(t => t.id === teacher.id);

              return (
                <Card key={teacher.id}>
                  <CardContent className="flex justify-between items-center p-4">
                    <div>
                      <h3 className="font-medium">{teacher.name}</h3>
                      <p className="text-sm text-muted-foreground">
                        {teacher.position}
                      </p>
                    </div>
                    <Button 
                      size="sm"
                      variant={isAssociated ? "outline" : "default"}
                      disabled={isAssociated}
                      onClick={() => handleAssociate(teacher.id)}
                    >
                      {isAssociated ? "已关联" : "关联"}
                    </Button>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
} 