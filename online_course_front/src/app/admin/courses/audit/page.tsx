"use client";

import { useState, useEffect } from "react";
import { CourseService, type CourseBase } from "@/services/course";
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

// 课程状态选项
const STATUS_OPTIONS = [
  { label: "全部状态", value: "" },
  { label: "未发布", value: "202001" },
  { label: "已发布", value: "202002" },
  { label: "已下线", value: "202003" },
];

// 审核状态选项
const AUDIT_STATUS_OPTIONS = [
  { label: "全部", value: "" },
  { label: "待审核", value: "202301" },
  { label: "审核不通过", value: "202302" },
  { label: "审核通过", value: "202303" },
];

export default function CourseAuditPage() {
  const [courses, setCourses] = useState<CourseBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 10;
  const { toast } = useToast();
  const [selectedCourse, setSelectedCourse] = useState<CourseBase | null>(null);

  // 筛选条件
  const [filters, setFilters] = useState({
    organizationId: "",
    status: "",
    auditStatus: "",
    courseName: "",
  });

  const loadCourses = async () => {
    try {
      setLoading(true);
      const data = await CourseService.getAdminCourseList({
        pageNo: currentPage,
        pageSize,
        ...(filters.organizationId && { organizationId: Number(filters.organizationId) }),
        ...(filters.status && { status: filters.status }),
        ...(filters.auditStatus && { auditStatus: filters.auditStatus }),
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

  // 处理筛选条件变化
  const handleFilterChange = (key: string, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setCurrentPage(1); // 重置页码
  };

  // 重置筛选条件
  const handleReset = () => {
    setFilters({
      organizationId: "",
      status: "",
      auditStatus: "",
      courseName: "",
    });
    setCurrentPage(1);
  };

  useEffect(() => {
    loadCourses();
  }, [currentPage, filters]);

  return (
    <div className="container py-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">课程管理</h1>
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
            <Card key={course.id}>
              <CardContent className="flex justify-between items-center p-4">
                <div className="space-y-2">
                  <div className="flex items-center space-x-2">
                    <h3 className="font-medium">{course.name}</h3>
                    <span className="text-sm text-muted-foreground">
                      (机构ID: {course.organizationId})
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {course.brief}
                  </p>
                  <div className="text-sm space-x-4">
                    <span>分类：{course.mtName} / {course.stName}</span>
                    <span>状态：{STATUS_OPTIONS.find(s => s.value === course.status)?.label}</span>
                    <span>审核：{AUDIT_STATUS_OPTIONS.find(s => s.value === course.auditStatus)?.label}</span>
                  </div>
                </div>
                {course.auditStatus === "202301" && (
                  <Button 
                    onClick={() => setSelectedCourse(course)}
                  >
                    审核
                  </Button>
                )}
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

      {selectedCourse && (
        <CourseAuditDialog
          courseId={selectedCourse.id}
          courseName={selectedCourse.name}
          open={!!selectedCourse}
          onOpenChange={(open) => !open && setSelectedCourse(null)}
          onSuccess={loadCourses}
        />
      )}
    </div>
  );
} 