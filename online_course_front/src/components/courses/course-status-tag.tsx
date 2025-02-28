"use client";

import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useCourseStatus } from "@/hooks/use-course-status";
import { COURSE_STATUS, COURSE_AUDIT_STATUS } from '@/constants/course';
import { CourseStatus, CourseAuditStatus } from '@/constants/course';
import { COURSE_STATUS_LABELS, COURSE_AUDIT_STATUS_LABELS } from '@/constants/course';

interface CourseStatusTagProps {
  status: string;
  auditStatus?: string;
  className?: string;
}

/**
 * 课程状态标签组件
 * 
 * 用于显示课程状态或审核状态，自动从服务端获取状态信息
 * 包含加载状态和错误处理
 * 
 * @example
 * // 显示课程状态
 * <CourseStatusTag status="202001" />  // 显示"未发布"
 * 
 * // 显示审核状态
 * <CourseStatusTag status="202301" type="audit" />  // 显示"已提交审核"
 */
export function CourseStatusTag({ status, auditStatus, className }: CourseStatusTagProps) {
  const { data, isLoading } = useCourseStatus();

  if (isLoading) {
    return <Skeleton className="h-5 w-16" />;
  }

  if (!data) return null;

  const statusList = data.courseStatus;
  const statusInfo = statusList.find(s => s.code === status);

  if (!statusInfo) return null;

  return (
    <div className="flex gap-2">
      {/* 发布状态 */}
      <Badge variant={getStatusVariant(status)} className={className}>
        {COURSE_STATUS_LABELS[status]}
      </Badge>
      
      {/* 审核状态 - 只在已发布且有审核状态时显示 */}
      {status === COURSE_STATUS.PUBLISHED && auditStatus && (
        <Badge variant={getAuditStatusVariant(auditStatus)} className={className}>
          {COURSE_AUDIT_STATUS_LABELS[auditStatus]}
        </Badge>
      )}
    </div>
  );
}

function getStatusVariant(status: string) {
  switch (status) {
    case COURSE_STATUS.DRAFT:
      return "secondary";
    case COURSE_STATUS.PUBLISHED:
      return "success";
    case COURSE_STATUS.OFFLINE:
      return "destructive";
    default:
      return "default";
  }
}

function getAuditStatusVariant(status: string) {
  switch (status) {
    case COURSE_AUDIT_STATUS.SUBMITTED:
      return "secondary";
    case COURSE_AUDIT_STATUS.APPROVED:
      return "success";
    case COURSE_AUDIT_STATUS.REJECTED:
      return "destructive";
    default:
      return "default";
  }
} 