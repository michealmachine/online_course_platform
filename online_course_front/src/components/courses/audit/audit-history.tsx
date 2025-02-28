"use client";

import { useEffect, useState } from "react";
import { AuditService, type AuditHistory } from "@/services/audit";
import { AuditStatus } from "./audit-status";
import { ScrollArea } from "@/components/ui/scroll-area";
import { format } from "date-fns";

interface AuditHistoryProps {
  courseId: number;
}

export function AuditHistory({ courseId }: AuditHistoryProps) {
  const [history, setHistory] = useState<AuditHistory[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadHistory = async () => {
      try {
        const data = await AuditService.getAuditHistory(courseId);
        setHistory(data);
      } catch (error) {
        console.error("Failed to load audit history:", error);
      } finally {
        setLoading(false);
      }
    };

    loadHistory();
  }, [courseId]);

  if (loading) {
    return <div>加载中...</div>;
  }

  return (
    <ScrollArea className="h-[300px] w-full rounded-md border p-4">
      <div className="space-y-4">
        {history.map((record) => (
          <div key={record.id} className="flex items-center justify-between">
            <div className="space-y-1">
              <AuditStatus status={record.status} />
              {record.message && (
                <p className="text-sm text-muted-foreground">{record.message}</p>
              )}
            </div>
            <time className="text-sm text-muted-foreground">
              {format(new Date(record.createTime), "yyyy-MM-dd HH:mm:ss")}
            </time>
          </div>
        ))}
      </div>
    </ScrollArea>
  );
} 