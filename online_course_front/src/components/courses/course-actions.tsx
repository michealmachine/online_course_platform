"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MoreHorizontal } from "lucide-react";
import { CourseService } from "@/services/course";
import { useToast } from "@/hooks/use-toast";
import {
  COURSE_STATUS,
  COURSE_AUDIT_STATUS,
  shouldShowPublishButton,
  shouldShowSubmitAuditButton,
  shouldShowOfflineButton,
  type CourseStatus,
  type CourseAuditStatus,
} from "@/constants/course";
import Link from "next/link";

interface CourseActionsProps {
  courseId: number;
  status: CourseStatus;
  auditStatus?: CourseAuditStatus;
  onSuccess?: () => void;
}

/**
 * 课程操作组件
 * 
 * 根据课程状态显示不同的操作选项：
 * - 未发布状态：可以提交审核
 * - 已发布状态：可以下线
 * - 审核不通过状态：可以重新提交审核
 * 
 * @example
 * <CourseActions 
 *   courseId={1}
 *   status={COURSE_STATUS.UNPUBLISHED}
 *   onSuccess={() => refetch()}
 * />
 */
export function CourseActions({ courseId, status, auditStatus, onSuccess }: CourseActionsProps) {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);

  const handleAction = async (action: () => Promise<void>) => {
    try {
      setLoading(true);
      await action();
      onSuccess?.();
      toast({
        title: "操作成功",
      });
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

  return (
    <div className="flex gap-2">
      {/* 发布按钮 - 草稿状态可见 */}
      {shouldShowPublishButton(status) && (
        <Button
          size="sm"
          onClick={() => handleAction(() => CourseService.publishCourse(courseId))}
          disabled={loading}
        >
          发布
        </Button>
      )}

      {/* 提交审核按钮 - 已发布且未提交审核或审核被拒绝时可见 */}
      {shouldShowSubmitAuditButton(status, auditStatus) && (
        <Button
          size="sm"
          onClick={() => handleAction(() => CourseService.submitForAudit(courseId))}
          disabled={loading}
        >
          {auditStatus === COURSE_AUDIT_STATUS.REJECTED ? "重新提交审核" : "提交审核"}
        </Button>
      )}

      {/* 下线按钮 - 已发布且审核通过时可见 */}
      {shouldShowOfflineButton(status, auditStatus) && (
        <Button
          size="sm"
          variant="destructive"
          onClick={() => handleAction(() => CourseService.offlineCourse(courseId))}
          disabled={loading}
        >
          下线
        </Button>
      )}
    </div>
  );
} 