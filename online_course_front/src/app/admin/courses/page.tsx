"use client";

import { useState, useEffect } from "react";
import { CourseService, type CourseBase, COURSE_STATUS, COURSE_STATUS_LABELS, COURSE_AUDIT_STATUS, COURSE_AUDIT_STATUS_LABELS } from "@/services/course";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { Pagination } from "@/components/common/pagination";
import { CourseAuditDialog } from "@/components/admin/course-audit-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { format } from "date-fns";

// 状态选项
const STATUS_OPTIONS = [
  { label: COURSE_STATUS_LABELS.ALL, value: "ALL" },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.DRAFT], value: COURSE_STATUS.DRAFT },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.PUBLISHED], value: COURSE_STATUS.PUBLISHED },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.OFFLINE], value: COURSE_STATUS.OFFLINE },
];

// 审核状态选项
const AUDIT_STATUS_OPTIONS = [
  { label: "全部", value: "ALL" },
  { label: "待审核", value: COURSE_AUDIT_STATUS.SUBMITTED },
  { label: "审核通过", value: COURSE_AUDIT_STATUS.APPROVED },
  { label: "审核不通过", value: COURSE_AUDIT_STATUS.REJECTED },
];

export default function AdminCoursesPage() {
  const [courses, setCourses] = useState<CourseBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 10;
  const { toast } = useToast();
  const [selectedCourse, setSelectedCourse] = useState<CourseBase | null>(null);
  const [pendingCourses, setPendingCourses] = useState<CourseBase[]>([]);
  const [pendingTotal, setPendingTotal] = useState(0);
  const [pendingDialogOpen, setPendingDialogOpen] = useState(false);
  const [pendingPage, setPendingPage] = useState(1);
  const pendingPageSize = 5;

  // 筛选条件
  const [filters, setFilters] = useState({
    organizationId: "",
    status: "ALL",
    auditStatus: "ALL",
    courseName: "",
  });

  const loadCourses = async () => {
    try {
      setLoading(true);
      const data = await CourseService.getAdminCourseList({
        pageNo: currentPage,
        pageSize,
        ...(filters.organizationId && { organizationId: Number(filters.organizationId) }),
        ...(filters.status !== "ALL" && { status: filters.status }),
        ...(filters.auditStatus !== "ALL" && { auditStatus: filters.auditStatus }),
        ...(filters.courseName && { courseName: filters.courseName }),
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

  // 加载待审核课程
  const loadPendingCourses = async () => {
    try {
      const data = await CourseService.getPendingAuditCourses({
        pageNo: pendingPage,
        pageSize: pendingPageSize,
      });
      setPendingCourses(data.items);
      setPendingTotal(data.counts);
    } catch (error) {
      toast({
        title: "加载失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  // 处理筛选条件变化
  const handleFilterChange = (key: string, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setCurrentPage(1); // 重置页码
  };

  // 重置筛选条件
  const handleReset = () => {
    setFilters({
      organizationId: "",
      status: "ALL",
      auditStatus: "ALL",
      courseName: "",
    });
    setCurrentPage(1);
  };

  useEffect(() => {
    loadCourses();
  }, [currentPage, filters]);

  // 添加待审核课程对话框
  const PendingAuditDialog = () => (
    <Dialog open={pendingDialogOpen} onOpenChange={setPendingDialogOpen}>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle>待审核课程</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          {pendingCourses.map(course => (
            <Card key={course.id}>
              <CardContent className="flex justify-between items-center p-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium">{course.name}</h3>
                    <span className="text-sm px-2 py-1 rounded-full bg-muted">
                      {COURSE_AUDIT_STATUS_LABELS[course.auditStatus] || '待审核'}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{course.brief}</p>
                  <div className="text-sm text-muted-foreground mt-1">
                    机构ID: {course.organizationId}
                  </div>
                </div>
                {course.auditStatus === COURSE_AUDIT_STATUS.SUBMITTED && (
                  <Button onClick={() => setSelectedCourse(course)}>
                    审核
                  </Button>
                )}
              </CardContent>
            </Card>
          ))}
          {pendingCourses.length === 0 && (
            <div className="text-center py-4 text-muted-foreground">
              暂无待审核课程
            </div>
          )}
          <Pagination
            total={pendingTotal}
            page={pendingPage}
            pageSize={pendingPageSize}
            onChange={setPendingPage}
          />
        </div>
      </DialogContent>
    </Dialog>
  );

  return (
    <div className="container py-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">课程管理</h1>
        <Button 
          onClick={() => {
            setPendingDialogOpen(true);
            loadPendingCourses();
          }}
        >
          待审核课程
        </Button>
      </div>

      {/* 筛选区域 */}
      <Card>
        <CardContent className="p-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label>机构ID</Label>
              <Input
                placeholder="请输入机构ID"
                value={filters.organizationId}
                onChange={e => handleFilterChange("organizationId", e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label>课程名称</Label>
              <Input
                placeholder="请输入课程名称"
                value={filters.courseName}
                onChange={e => handleFilterChange("courseName", e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label>课程状态</Label>
              <Select
                value={filters.status}
                onValueChange={value => handleFilterChange("status", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="选择课程状态" />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map(option => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>审核状态</Label>
              <Select
                value={filters.auditStatus}
                onValueChange={value => handleFilterChange("auditStatus", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="选择审核状态" />
                </SelectTrigger>
                <SelectContent>
                  {AUDIT_STATUS_OPTIONS.map(option => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="flex justify-end space-x-2">
            <Button variant="outline" onClick={handleReset}>
              重置
            </Button>
            <Button onClick={loadCourses}>
              查询
            </Button>
          </div>
        </CardContent>
      </Card>
      
      {/* 课程列表 */}
      {loading ? (
        <div className="text-center py-8 text-muted-foreground">加载中...</div>
      ) : courses.length === 0 ? (
        <Card>
          <CardContent className="text-center py-6">
            暂无课程数据
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {courses.map(course => (
            <Card key={course.courseBase.id}>
              <CardContent className="flex justify-between items-center p-4">
                <div className="space-y-2">
                  <div className="flex items-center space-x-2">
                    <h3 className="font-medium">{course.courseBase.name}</h3>
                    <span className="text-sm text-muted-foreground">
                      (机构ID: {course.courseBase.organizationId})
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {course.courseBase.brief}
                  </p>
                  <div className="text-sm space-x-4">
                    <span>分类：{course.courseBase.mtName} / {course.courseBase.stName}</span>
                    <span>状态：{COURSE_STATUS_LABELS[course.courseBase.status]}</span>
                    <span className={`px-2 py-1 rounded-full ${
                      course.auditStatus === COURSE_AUDIT_STATUS.APPROVED 
                        ? 'bg-green-100 text-green-800'
                        : course.auditStatus === COURSE_AUDIT_STATUS.REJECTED
                        ? 'bg-red-100 text-red-800'
                        : course.auditStatus === COURSE_AUDIT_STATUS.SUBMITTED
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}>
                      审核状态：{COURSE_AUDIT_STATUS_LABELS[course.auditStatus] || '未提交'}
                    </span>
                    {course.lastAuditTime && (
                      <span>审核时间：{format(new Date(course.lastAuditTime), 'yyyy-MM-dd HH:mm:ss')}</span>
                    )}
                    {course.auditMessage && (
                      <span>审核意见：{course.auditMessage}</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {course.auditStatus === COURSE_AUDIT_STATUS.SUBMITTED && (
                    <Button onClick={() => setSelectedCourse(course.courseBase)}>
                      审核
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}

          <Pagination
            total={total}
            page={currentPage}
            pageSize={pageSize}
            onChange={setCurrentPage}
          />
        </div>
      )}

      {/* 添加待审核对话框 */}
      <PendingAuditDialog />

      {/* 审核操作对话框 */}
      {selectedCourse && (
        <CourseAuditDialog
          courseId={selectedCourse.id}
          courseName={selectedCourse.name}
          open={!!selectedCourse}
          onOpenChange={(open) => {
            if (!open) {
              setSelectedCourse(null);
              // 刷新待审核列表
              loadPendingCourses();
            }
          }}
          onSuccess={() => {
            // 同时刷新两个列表
            loadCourses();
            loadPendingCourses();
          }}
        />
      )}
    </div>
  );
} 