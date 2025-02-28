"use client";

import { useEffect, useState } from "react";
import { TeacherService, type CourseTeacher } from "@/services/teacher";
import { Card, CardContent } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/common/pagination";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MoreHorizontal, PlusCircle } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { TeacherForm } from "./teacher-form";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { TeacherCourses } from "./teacher-courses";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TeacherCourseList } from "./teacher-course-list";

interface Props {
  organizationId: number;
}

// 使用在线图片作为默认头像
const DEFAULT_TEACHER_AVATAR = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjI0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyYWQiIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMwZWE1ZTk7c3RvcC1vcGFjaXR5OjEiIC8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdHlsZT0ic3RvcC1jb2xvcjojMGQ5NDg4O3N0b3Atb3BhY2l0eToxIiAvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iMjQwIiBmaWxsPSJ1cmwoI2dyYWQpIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMjQiIGZpbGw9IiNmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+VGVhY2hlcjwvdGV4dD48L3N2Zz4=';

export function TeacherList({ organizationId }: Props) {
  const [teachers, setTeachers] = useState<CourseTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  
  // 添加表单相关状态
  const [showForm, setShowForm] = useState(false);
  const [editingTeacher, setEditingTeacher] = useState<CourseTeacher | undefined>();

  // 添加删除相关状态
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [teacherToDelete, setTeacherToDelete] = useState<CourseTeacher | null>(null);

  const [selectedTeacherId, setSelectedTeacherId] = useState<number | null>(null);

  // 处理页码变化
  const handlePageChange = async (newPage: number) => {
    setCurrentPage(newPage);  // 先更新页码
    try {
      setLoading(true);
      const data = await TeacherService.getTeacherList(organizationId, {
        pageNo: newPage,  // 使用新的页码
        pageSize
      });
      setTeachers(data.items);
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

  // 处理分页大小变化
  const handlePageSizeChange = (value: string) => {
    const newSize = Number(value);
    setPageSize(newSize);
    handlePageChange(1);  // 改变分页大小时，重置到第一页
  };

  // 初始加载
  useEffect(() => {
    handlePageChange(1);  // 初始加载第一页
  }, [organizationId]);

  // 处理编辑
  const handleEdit = (teacher: CourseTeacher) => {
    setEditingTeacher(teacher);
    setShowForm(true);
  };

  // 处理删除
  const handleDelete = async (teacher: CourseTeacher) => {
    setTeacherToDelete(teacher);
    setDeleteDialogOpen(true);
  };

  // 确认删除
  const confirmDelete = async () => {
    if (!teacherToDelete) return;

    try {
      await TeacherService.deleteTeacher(organizationId, teacherToDelete.id);
      toast({
        title: "删除成功",
        description: `教师 "${teacherToDelete.name}" 已删除`,
      });
      handlePageChange(1); // 重新加载第一页
    } catch (error) {
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setDeleteDialogOpen(false);
      setTeacherToDelete(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* 操作栏 */}
      <div className="flex justify-end">
        <Button onClick={() => {
          setEditingTeacher(undefined); // 清空编辑状态
          setShowForm(true);
        }}>
          <PlusCircle className="w-4 h-4 mr-2" />
          添加教师
        </Button>
      </div>

      {/* 加载状态和列表 */}
      {loading ? (
        <div className="text-center py-8 text-muted-foreground">加载中...</div>
      ) : !teachers.length ? (
        <Card>
          <CardContent className="text-center py-8">
            <p className="text-muted-foreground">暂无教师数据</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {teachers.map((teacher) => (
            <Card key={teacher.id}>
              <CardContent className="p-4">
                <div className="flex items-start gap-4">
                  {/* 教师头像 */}
                  <div className="relative w-40 h-24 bg-muted rounded-lg overflow-hidden">
                    <Image
                      src={teacher.avatar || DEFAULT_TEACHER_AVATAR}
                      alt={teacher.name}
                      fill
                      className="object-cover"
                      priority
                      onError={(e) => {
                        const target = e.target as HTMLImageElement;
                        target.src = DEFAULT_TEACHER_AVATAR;
                      }}
                    />
                  </div>
                  
                  <div className="flex-1">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold">{teacher.name}</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          {teacher.position}
                        </p>
                        {teacher.description && (
                          <p className="text-sm mt-2">{teacher.description}</p>
                        )}
                      </div>
                      
                      {/* 操作菜单 */}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <span className="sr-only">打开菜单</span>
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleEdit(teacher)}>
                            编辑
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={() => handleDelete(teacher)}
                            className="text-red-600"
                          >
                            删除
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={() => setSelectedTeacherId(teacher.id)}
                          >
                            查看课程
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>

                {/* 关联课程列表 */}
                {selectedTeacherId === teacher.id && (
                  <div className="mt-4">
                    <TeacherCourseList 
                      teacherId={teacher.id}
                      organizationId={organizationId}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* 分页控制 */}
      <div className="flex items-center justify-between">
        <Select
          value={String(pageSize)}
          onValueChange={handlePageSizeChange}
        >
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="选择每页显示数量" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="5">5条/页</SelectItem>
            <SelectItem value="10">10条/页</SelectItem>
            <SelectItem value="20">20条/页</SelectItem>
          </SelectContent>
        </Select>

        <Pagination
          total={total}
          page={currentPage}
          pageSize={pageSize}
          onChange={handlePageChange}
        />
      </div>

      {/* 表单和对话框保持不变 */}
      <TeacherForm
        organizationId={organizationId}
        open={showForm}
        onOpenChange={setShowForm}
        initialData={editingTeacher}
        onSuccess={() => {
          handlePageChange(1);
          setShowForm(false);
          setEditingTeacher(undefined);
        }}
      />

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除教师 "{teacherToDelete?.name}" 吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction 
              onClick={confirmDelete}
              className="bg-red-600 hover:bg-red-700"
            >
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
} 