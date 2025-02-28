/**
 * 课程状态定义
 */
export const COURSE_STATUS = {
  DRAFT: "202001",     // 草稿
  PUBLISHED: "202002", // 已发布
  OFFLINE: "202003",   // 已下线
} as const;

/**
 * 课程审核状态定义
 */
export const COURSE_AUDIT_STATUS = {
  SUBMITTED: "202004", // 已提交审核
  APPROVED: "202005",  // 审核通过
  REJECTED: "202006",  // 审核不通过
} as const;

/**
 * 课程收费类型常量
 * 用于定义课程的收费模式
 */
export const COURSE_CHARGE_TYPE = {
  FREE: '201001',         // 免费 - 学习者可以免费学习
  PAID: '201002',         // 收费 - 需要付费才能学习
} as const;

/**
 * 课程状态类型
 */
export type CourseStatus = typeof COURSE_STATUS[keyof typeof COURSE_STATUS];

/**
 * 课程审核状态类型
 */
export type CourseAuditStatus = typeof COURSE_AUDIT_STATUS[keyof typeof COURSE_AUDIT_STATUS];

/**
 * 课程收费类型
 */
export type CourseChargeType = typeof COURSE_CHARGE_TYPE[keyof typeof COURSE_CHARGE_TYPE];

export const COURSE_STATUS_LABELS: Record<string, string> = {
  [COURSE_STATUS.DRAFT]: "草稿",
  [COURSE_STATUS.PUBLISHED]: "已发布",
  [COURSE_STATUS.OFFLINE]: "已下线",
  "ALL": "全部状态",
} as const;

export const COURSE_AUDIT_STATUS_LABELS: Record<string, string> = {
  [COURSE_AUDIT_STATUS.SUBMITTED]: "已提交审核",
  [COURSE_AUDIT_STATUS.APPROVED]: "审核通过",
  [COURSE_AUDIT_STATUS.REJECTED]: "审核不通过",
  "ALL": "全部审核状态",
} as const;

/**
 * 课程操作按钮显示逻辑
 */
export function shouldShowPublishButton(status: CourseStatus): boolean {
  return status === COURSE_STATUS.DRAFT;
}

export function shouldShowSubmitAuditButton(status: CourseStatus, auditStatus?: CourseAuditStatus): boolean {
  return status === COURSE_STATUS.PUBLISHED && (!auditStatus || auditStatus === COURSE_AUDIT_STATUS.REJECTED);
}

export function shouldShowOfflineButton(status: CourseStatus, auditStatus?: CourseAuditStatus): boolean {
  return status === COURSE_STATUS.PUBLISHED && auditStatus === COURSE_AUDIT_STATUS.APPROVED;
} 