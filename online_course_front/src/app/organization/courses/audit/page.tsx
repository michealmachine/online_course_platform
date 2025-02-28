"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CourseService, type CourseBase } from "@/services/course";
import { CourseStatusFilter } from "@/components/courses/course-status-filter";
import { AuditStatus } from "@/components/courses/audit/audit-status";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Skeleton } from "@/components/ui/skeleton";
import { COURSE_AUDIT_STATUS, type CourseAuditStatus } from "@/constants/course";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { AuditService } from "@/services/audit";
import { AuditHistory } from "@/components/courses/audit/audit-history";

export default function CourseAuditPage() {
  const searchParams = useSearchParams();
  const organizationId = searchParams.get("organizationId");
  const [courses, setCourses] = useState<CourseBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [auditStatus, setAuditStatus] = useState<CourseAuditStatus | "">("");
  const { toast } = useToast();
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<CourseBase | null>(null);
  const [rejectMessage, setRejectMessage] = useState("");
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);
  const [currentCourseId, setCurrentCourseId] = useState<number | null>(null);

  // 提取加载课程的函数
  const loadCourses = async () => {
    if (!organizationId) return;
    
    try {
      setLoading(true);
      const response = await CourseService.getOrganizationCourseList(
        Number(organizationId),
        {
          auditStatus: auditStatus || undefined,
        }
      );
      setCourses(response.items);
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

  // 使用提取的函数
  useEffect(() => {
    loadCourses();
  }, [organizationId, auditStatus]);  // 移除 toast 依赖

  // 处理审核通过
  const handleApprove = async (courseId: number) => {
    try {
      setLoading(true);
      await AuditService.approveAudit(courseId);
      toast({
        title: "审核成功",
        description: "课程已通过审核",
      });
      // 重新加载列表
      loadCourses();
    } catch (error) {
      toast({
        title: "操作失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 处理审核拒绝
  const handleReject = async () => {
    if (!selectedCourse) return;
    if (!rejectMessage.trim()) {
      toast({
        title: "请输入拒绝原因",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      await AuditService.rejectAudit(selectedCourse.id, rejectMessage);
      setRejectDialogOpen(false);
      setSelectedCourse(null);
      setRejectMessage("");
      toast({
        title: "审核完成",
        description: "已拒绝课程审核",
      });
      // 重新加载列表
      loadCourses();
    } catch (error) {
      toast({
        title: "操作失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 处理查看历史
  const handleViewHistory = (courseId: number) => {
    setCurrentCourseId(courseId);
    setHistoryDialogOpen(true);
  };

  // 加载状态UI
  if (loading) {
    return (
      <div className="container py-6 space-y-6">
        <div className="flex items-center justify-between">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-10 w-32" />
        </div>
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <div className="space-y-2">
                <Skeleton className="h-4 w-1/4" />
                <Skeleton className="h-4 w-1/2" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <main className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">课程审核管理</h1>
        <CourseStatusFilter
          value={auditStatus}
          onChange={setAuditStatus}
          type="audit"
        />
      </div>

      <div className="grid gap-6">
        {courses.map((course) => (
          <Card key={course.id}>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <h2 className="text-xl font-semibold">{course.name}</h2>
                  <p className="text-sm text-muted-foreground">{course.brief}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <AuditStatus status={course.auditStatus || ""} />
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    onClick={() => handleViewHistory(course.id)}
                  >
                    查看历史
                  </Button>
                  {course.auditStatus === COURSE_AUDIT_STATUS.SUBMITTED && (
                    <>
                      <Button
                        variant="default"
                        className="bg-green-500 hover:bg-green-600"
                        onClick={() => handleApprove(course.id)}
                        disabled={loading}
                      >
                        通过
                      </Button>
                      <Button
                        variant="destructive"
                        onClick={() => {
                          setSelectedCourse(course);
                          setRejectDialogOpen(true);
                        }}
                        disabled={loading}
                      >
                        拒绝
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}

        {courses.length === 0 && (
          <Card>
            <CardContent className="p-6">
              <div className="text-center text-muted-foreground">
                暂无待审核课程
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* 添加拒绝对话框 */}
      <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>审核拒绝原因</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <Textarea
              placeholder="请输入拒绝原因"
              value={rejectMessage}
              onChange={(e) => setRejectMessage(e.target.value)}
            />
            <div className="flex justify-end gap-4">
              <Button
                variant="outline"
                onClick={() => {
                  setRejectDialogOpen(false);
                  setSelectedCourse(null);
                  setRejectMessage("");
                }}
                disabled={loading}
              >
                取消
              </Button>
              <Button
                variant="destructive"
                onClick={handleReject}
                disabled={loading}
              >
                确认拒绝
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* 审核历史对话框 */}
      <Dialog open={historyDialogOpen} onOpenChange={setHistoryDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>审核历史记录</DialogTitle>
          </DialogHeader>
          {currentCourseId && (
            <AuditHistory courseId={currentCourseId} />
          )}
        </DialogContent>
      </Dialog>
    </main>
  );
} 