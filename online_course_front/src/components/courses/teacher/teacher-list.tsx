"use client";

import { useEffect, useState } from "react";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { Button } from "@/components/ui/button";
import { PlusCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import Image from "next/image";
import { useToast } from "@/hooks/use-toast";

interface TeacherListProps {
  courseId: number;
  onEdit?: (teacher: CourseTeacher) => void;
  onAdd?: () => void;
}

/**
 * 课程教师列表组件
 * 
 * 展示课程的教师列表，支持添加、编辑和删除操作
 * 
 * @example
 * <TeacherList 
 *   courseId={1}
 *   onEdit={(teacher) => setEditingTeacher(teacher)}
 *   onAdd={() => setShowTeacherForm(true)}
 * />
 */
export function TeacherList({ courseId, onEdit, onAdd }: TeacherListProps) {
  const [teachers, setTeachers] = useState<CourseTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  // 加载教师列表
  const loadTeachers = async () => {
    try {
      setLoading(true);
      const data = await TeacherService.getTeacherList(courseId);
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

  useEffect(() => {
    loadTeachers();
  }, [courseId]);

  // 删除教师
  const handleDelete = async (teacher: CourseTeacher) => {
    try {
      await TeacherService.deleteTeacher(courseId, teacher.teacherId);
      toast({
        title: "删除成功",
        description: `已删除教师 "${teacher.teacherName}"`,
      });
      loadTeachers(); // 重新加载列表
    } catch (error) {
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <div className="space-y-1">
          <h2 className="text-2xl font-bold">课程教师</h2>
          <p className="text-sm text-muted-foreground">
            管理课程的教师信息，可以添加、编辑和删除教师
          </p>
        </div>
        <Button onClick={onAdd}>
          <PlusCircle className="w-4 h-4 mr-2" />
          添加教师
        </Button>
      </div>

      {loading ? (
        <div className="text-center py-8 text-muted-foreground">加载中...</div>
      ) : teachers.length === 0 ? (
        <Card>
          <CardContent className="text-center py-8">
            <p className="text-muted-foreground">暂无教师信息</p>
            <Button 
              className="mt-4" 
              variant="outline"
              onClick={onAdd}
            >
              添加第一位教师
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {teachers.map((teacher) => (
            <Card key={teacher.id}>
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <span>{teacher.teacherName}</span>
                  <div className="space-x-2">
                    <Button 
                      variant="ghost" 
                      size="sm"
                      onClick={() => onEdit?.(teacher)}
                    >
                      编辑
                    </Button>
                    <Button 
                      variant="ghost" 
                      size="sm"
                      onClick={() => handleDelete(teacher)}
                    >
                      删除
                    </Button>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {teacher.photograph && (
                    <div className="relative w-full aspect-video rounded-lg overflow-hidden">
                      <Image
                        src={teacher.photograph}
                        alt={teacher.teacherName}
                        fill
                        className="object-cover"
                      />
                    </div>
                  )}
                  <div>
                    <p className="font-medium">{teacher.position}</p>
                    <p className="text-sm text-muted-foreground mt-1">
                      {teacher.introduction}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
} 