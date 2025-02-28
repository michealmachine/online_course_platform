import { contentRequest } from './index';
import { API_URLS } from '@/config/api.config';
import { AUDIT_STATUS } from './course';

/**
 * 审核历史记录
 */
export interface AuditHistory {
  id: number;
  courseId: number;
  courseName: string;
  auditorId: number;
  auditorName: string;
  auditStatus: string;
  auditMessage?: string;
  auditTime: string;
}

/**
 * 审核服务
 */
export class AuditService {
  /**
   * 提交课程审核
   */
  static async submitAudit(courseId: number): Promise<void> {
    const url = API_URLS.COURSE_AUDIT.SUBMIT.replace(':courseId', String(courseId));
    return contentRequest.post(url);
  }

  /**
   * 获取审核历史
   */
  static async getAuditHistory(courseId: number): Promise<AuditHistory[]> {
    const url = API_URLS.COURSE_AUDIT.HISTORY.replace(':courseId', String(courseId));
    return contentRequest.get(url);
  }

  /**
   * 审核通过
   */
  static async approveAudit(courseId: number, message?: string): Promise<void> {
    return contentRequest.post(API_URLS.COURSE_AUDIT.APPROVE, {
      courseId,
      auditStatus: AUDIT_STATUS.APPROVED,
      auditMessage: message
    });
  }

  /**
   * 审核拒绝
   */
  static async rejectAudit(courseId: number, message: string): Promise<void> {
    return contentRequest.post(API_URLS.COURSE_AUDIT.APPROVE, {
      courseId,
      auditStatus: AUDIT_STATUS.REJECTED,
      auditMessage: message
    });
  }

  /**
   * 获取课程审核状态
   */
  static async getAuditStatus(courseId: number, organizationId: number): Promise<string> {
    const url = API_URLS.COURSE_AUDIT.STATUS.replace(':courseId', String(courseId));
    return contentRequest.get(url, {
      params: { organizationId }
    });
  }

  /**
   * 获取待审核课程列表
   */
  static async getPendingList(pageNo: number, pageSize: number) {
    return contentRequest.get(API_URLS.COURSE_AUDIT.PENDING, {
      params: { pageNo, pageSize }
    });
  }
} 