/**
 * 测试机构数据
 * TODO: 后续需要替换为真实的机构管理系统
 */
export const TEST_ORGANIZATIONS = [
  { id: 1, name: "测试机构1" },
  { id: 2, name: "测试机构2" },
] as const;

/**
 * 测试用户数据
 * TODO: 后续需要替换为真实的用户认证系统
 */
export const TEST_USERS = [
  { id: 1, name: "平台管理员", role: "admin" },
  { id: 2, name: "机构管理员", role: "org_admin", organizationId: 1 },
  { id: 3, name: "教师", role: "teacher", organizationId: 1 },
] as const;

/**
 * 课程状态
 */
export const COURSE_STATUS = {
  DRAFT: "202001",     // 未发布
  PUBLISHED: "202002", // 已发布
  OFFLINE: "202003",   // 已下线
} as const;

/**
 * 收费类型
 */
export const CHARGE_TYPE = {
  FREE: "201001",      // 免费
  PAID: "201002",      // 收费
} as const;

/**
 * 角色类型
 */
export const ROLE_TYPE = {
  ADMIN: "admin",           // 平台管理员
  ORG_ADMIN: "org_admin",   // 机构管理员
  TEACHER: "teacher",       // 教师
} as const; 