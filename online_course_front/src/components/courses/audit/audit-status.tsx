"use client";

import { Badge } from "@/components/ui/badge";
import { COURSE_AUDIT_STATUS } from "@/constants/course";

interface AuditStatusProps {
  status: string;
  className?: string;
}

export function AuditStatus({ status, className }: AuditStatusProps) {
  const getVariant = () => {
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
  };

  return (
    <Badge variant={getVariant()} className={className}>
      {status === COURSE_AUDIT_STATUS.SUBMITTED && "审核中"}
      {status === COURSE_AUDIT_STATUS.APPROVED && "审核通过"}
      {status === COURSE_AUDIT_STATUS.REJECTED && "审核不通过"}
    </Badge>
  );
} 