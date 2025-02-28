"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { AuditService } from "@/services/audit";
import { AuditHistory } from "./audit-history";
import { AuditStatus } from "./audit-status";
import { COURSE_AUDIT_STATUS } from "@/constants/course";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface AuditFormProps {
  courseId: number;
  status?: string;
  onSuccess?: () => void;
}

export function AuditForm({ courseId, status, onSuccess }: AuditFormProps) {
  const [loading, setLoading] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectMessage, setRejectMessage] = useState("");
  const { toast } = useToast();

  // 提交审核
  const handleSubmit = async () => {
    try {
      setLoading(true);
      await AuditService.submitAudit(courseId);
      toast({
        title: "提交成功",
        description: "课程已提交审核",
      });
      onSuccess?.();
    } catch (error) {
      toast({
        title: "提交失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 审核通过
  const handleApprove = async () => {
    try {
      setLoading(true);
      await AuditService.approveAudit(courseId);
      toast({
        title: "审核通过",
        description: "课程已通过审核",
      });
      onSuccess?.();
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

  // 审核拒绝
  const handleReject = async () => {
    if (!rejectMessage.trim()) {
      toast({
        title: "请输入拒绝原因",
        variant: "destructive",
      });
      return;
    }

    try {
      setLoading(true);
      await AuditService.rejectAudit(courseId, rejectMessage);
      setRejectDialogOpen(false);
      toast({
        title: "审核拒绝",
        description: "已拒绝课程审核",
      });
      onSuccess?.();
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
    <div className="space-y-6">
      {/* 审核状态 */}
      {status && (
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">当前状态：</span>
          <AuditStatus status={status} />
        </div>
      )}

      {/* 审核操作按钮 */}
      <div className="flex items-center gap-4">
        {/* 提交审核按钮 */}
        {(!status || status === COURSE_AUDIT_STATUS.REJECTED) && (
          <Button onClick={handleSubmit} disabled={loading}>
            提交审核
          </Button>
        )}

        {/* 审核操作按钮（运营人员） */}
        {status === COURSE_AUDIT_STATUS.SUBMITTED && (
          <>
            <Button 
              onClick={handleApprove} 
              disabled={loading} 
              variant="default"
              className="bg-green-500 hover:bg-green-600"
            >
              通过
            </Button>
            <Button
              onClick={() => setRejectDialogOpen(true)}
              disabled={loading}
              variant="destructive"
            >
              拒绝
            </Button>
          </>
        )}
      </div>

      {/* 审核历史 */}
      <AuditHistory courseId={courseId} />

      {/* 拒绝原因对话框 */}
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
                onClick={() => setRejectDialogOpen(false)}
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
    </div>
  );
} 