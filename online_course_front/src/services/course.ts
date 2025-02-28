import { contentRequest } from './index';
import { API_URLS } from '@/config/api.config';
import { ApiError, ApiResponse } from '@/lib/request';

/**
 * 分页参数
 */
export interface PageParams {
  pageNo: number;
  pageSize: number;
}

/**
 * 课程查询参数
 */
export interface QueryCourseParams {
  courseName?: string;
  status?: string;
  mt?: string;
  st?: string;
}

/**
 * 课程基本信息接口
 */
export interface CourseBase {
  id: number;
  name: string;
  brief: string;
  organizationId: number;
  mtName: string;
  stName: string;
  status: string;
  auditStatus: string;
}

/**
 * 创建课程参数接口
 */
export interface CreateCourseParams {
  name: string;
  brief: string;
  mt: string;
  st: string;
  charge: string;
  price?: number;
}

/**
 * 分页响应接口
 */
export interface PageResult<T> {
  items: T[];
  counts: number;
  page: number;
  pageSize: number;
}

// 课程状态
export const COURSE_STATUS = {
  DRAFT: "202001",     // 草稿
  PUBLISHED: "202002", // 已发布
  OFFLINE: "202003",   // 已下线
} as const;

// 课程状态标签
export const COURSE_STATUS_LABELS: Record<string, string> = {
  [COURSE_STATUS.DRAFT]: "草稿",
  [COURSE_STATUS.PUBLISHED]: "已发布", 
  [COURSE_STATUS.OFFLINE]: "已下线",
  "ALL": "全部状态",
} as const;

// 审核状态
export const AUDIT_STATUS = {
  PENDING: 'PENDING',    // 待审核
  APPROVED: 'APPROVED',  // 审核通过
  REJECTED: 'REJECTED'   // 审核不通过
} as const;

// 审核状态标签
export const COURSE_AUDIT_STATUS_LABELS: Record<string, string> = {
  [AUDIT_STATUS.PENDING]: '待审核',
  [AUDIT_STATUS.APPROVED]: '审核通过',
  [AUDIT_STATUS.REJECTED]: '审核不通过',
};

// 添加收费规则常量
export const COURSE_CHARGE = {
  FREE: "201001",   // 免费
  CHARGE: "201002", // 收费
} as const;

// 添加收费规则标签
export const COURSE_CHARGE_LABELS: Record<string, string> = {
  [COURSE_CHARGE.FREE]: "免费",
  [COURSE_CHARGE.CHARGE]: "收费",
} as const;

/**
 * 添加审核相关的接口
 */
export interface CourseAuditDTO {
  courseId: number;
  auditStatus: string;
  auditMessage?: string;
}

// 课程审核信息接口
export interface CourseAuditInfo {
  courseBase: CourseBase;
  auditStatus: string;
  auditMessage?: string;
  lastAuditTime?: string;
}

/**
 * 课程预览信息DTO
 */
export interface CoursePreviewDTO {
  courseBase: CourseBase;
  teachplans: TeachplanDTO[];
  teachers: CourseTeacherDTO[];
}

/**
 * 课程教师DTO
 */
export interface CourseTeacherDTO {
  id: number;
  organizationId: number;
  name: string;
  position: string;
  description?: string;
  courseIds?: number[];
  avatar?: string;
}

/**
 * 课程服务类
 */
export class CourseService {
  /**
   * 获取公开课程列表（已发布的课程）
   */
  static async getPublicCourseList(pageParams: PageParams, queryParams?: QueryCourseParams): Promise<PageResult<CourseBase>> {
    return contentRequest.get(API_URLS.COURSE.LIST, {
      params: {
        ...pageParams,
        ...queryParams,
        status: '202002' // 已发布状态
      }
    });
  }

  /**
   * 获取机构课程列表
   */
  static async getOrganizationCourseList(
    organizationId: number,
    pageParams: PageParams,
    status?: string,
    auditStatus?: string
  ): Promise<PageResult<CourseBase>> {
    const url = API_URLS.COURSE.ORGANIZATION_LIST.replace(':organizationId', String(organizationId));
    return contentRequest.get(url, {
      params: {
        ...pageParams,
        status,
        auditStatus
      }
    });
  }

  /**
   * 获取课程列表（管理员）
   */
  static async getAdminCourseList(params: {
    pageNo: number;
    pageSize: number;
    organizationId?: number;
    status?: string;
    auditStatus?: string;
    courseName?: string;
  }): Promise<PageResult<CourseAuditInfo>> {
    return contentRequest.get(API_URLS.COURSE.ADMIN_LIST, { 
      params: {
        ...params,
      }
    });
  }

  /**
   * 创建课程
   * @param data 课程数据
   */
  static async createCourse(data: CreateCourseParams): Promise<number> {
    return contentRequest.post(API_URLS.COURSE.CREATE, data);
  }

  /**
   * 更新课程
   * @param data 课程数据
   */
  static async updateCourse(data: CourseBase): Promise<void> {
    return contentRequest.put(API_URLS.COURSE.UPDATE, data);
  }

  /**
   * 获取课程详情
   * @param id 课程ID
   */
  static async getCourseDetail(id: number): Promise<CourseBase> {
    const url = API_URLS.COURSE.DETAIL.replace(':id', String(id));
    return contentRequest.get(url);
  }

  /**
   * 提交课程审核
   */
  static async submitForAudit(courseId: number): Promise<void> {
    const url = API_URLS.COURSE_AUDIT.SUBMIT.replace(':courseId', String(courseId));
    return contentRequest.post(url);
  }

  /**
   * 重新提交审核
   */
  static async resubmitForAudit(courseId: number): Promise<void> {
    const url = API_URLS.COURSE.RESUBMIT.replace(':courseId', courseId.toString());
    return contentRequest.post(url);
  }

  /**
   * 发布课程
   * @param id 课程ID
   */
  static async publishCourse(id: number): Promise<void> {
    const url = API_URLS.COURSE.PUBLISH.replace(':id', String(id));
    return contentRequest.post(url);
  }

  /**
   * 下线课程
   */
  static async offlineCourse(courseId: number): Promise<void> {
    const url = API_URLS.COURSE.OFFLINE.replace(':courseId', courseId.toString());
    return contentRequest.post(url);
  }

  /**
   * 获取待审核课程列表
   */
  static async getPendingAuditCourses(pageParams: PageParams): Promise<PageResult<CourseBase>> {
    return contentRequest.get(API_URLS.COURSE_AUDIT.PENDING, { 
      params: pageParams 
    });
  }

  /**
   * 获取课程审核历史
   */
  static async getAuditHistory(courseId: number, pageParams: PageParams): Promise<PageResult<CourseAuditHistory>> {
    const url = API_URLS.COURSE_AUDIT.HISTORY.replace(':courseId', String(courseId));
    return contentRequest.get(url, { params: pageParams });
  }

  /**
   * 审核课程
   */
  static async auditCourse(params: CourseAuditDTO): Promise<void> {
    return contentRequest.post(API_URLS.COURSE_AUDIT.APPROVE, params);
  }

  /**
   * 删除课程
   */
  static async deleteCourse(courseId: number): Promise<void> {
    const url = API_URLS.COURSE.DELETE.replace(':courseId', courseId.toString());
    return contentRequest.delete(url);
  }

  /**
   * 获取课程计划树
   */
  static async getTeachplanTree(courseId: number): Promise<TeachplanDTO[]> {
    return contentRequest.get(`/teachplan/tree/${courseId}`);
  }

  /**
   * 保存课程计划
   */
  static async saveTeachplan(teachplan: {
    id?: number;
    courseId: number;
    parentId: number;
    name: string;
    level: number;
    orderBy?: number;
  }): Promise<number> {
    return contentRequest.post('/teachplan', teachplan);
  }

  /**
   * 上移课程计划(临时)
   */
  static async moveUpTeachplan(teachplanId: number): Promise<void> {
    return contentRequest.post(`/teachplan/moveup/${teachplanId}`);
  }

  /**
   * 下移课程计划(临时)
   */
  static async moveDownTeachplan(teachplanId: number): Promise<void> {
    return contentRequest.post(`/teachplan/movedown/${teachplanId}`);
  }

  /**
   * 保存排序变更
   */
  static async saveTeachplanOrder(): Promise<void> {
    return contentRequest.post('/teachplan/saveorder');
  }

  /**
   * 丢弃排序变更
   */
  static async discardTeachplanOrder(): Promise<void> {
    return contentRequest.post('/teachplan/discardorder');
  }

  /**
   * 删除课程计划
   */
  static async deleteTeachplan(teachplanId: number): Promise<void> {
    return contentRequest.delete(`/teachplan/${teachplanId}`);
  }

  /**
   * 绑定媒资
   */
  static async associateMedia(params: {
    teachplanId: number;
    mediaId: string;
    mediaFileName: string;
    mediaType: string;
  }): Promise<void> {
    return contentRequest.post('/teachplan/media', params);
  }

  /**
   * 解除媒资绑定
   */
  static async dissociateMedia(teachplanId: number, mediaId: number): Promise<void> {
    return contentRequest.delete(`/teachplan/media/${teachplanId}/${mediaId}`);
  }

  /**
   * 获取课程审核状态
   */
  static async getAuditStatus(courseId: number, organizationId: number): Promise<string> {
    const url = API_URLS.COURSE_AUDIT.STATUS.replace(':courseId', String(courseId));
    return contentRequest.get(url, {
      params: {
        organizationId
      }
    });
  }

  /**
   * 获取审核课程详情
   */
  static async getAuditCourseDetail(courseId: number): Promise<CoursePreviewDTO> {
    const url = API_URLS.COURSE_AUDIT.DETAIL.replace(':courseId', String(courseId));
    return contentRequest.get(url);
  }

  /**
   * 获取审核课程列表
   */
  static async getAuditCourseList(params: {
    pageParams: PageParams;
    organizationId?: number;
    status?: string;
    auditStatus?: string;
    courseName?: string;
  }): Promise<PageResult<CourseBase>> {
    return contentRequest.get(API_URLS.COURSE_AUDIT.LIST, {
      params: {
        ...params.pageParams,
        ...(params.organizationId && { organizationId: params.organizationId }),
        ...(params.status && { status: params.status }),
        ...(params.auditStatus && { auditStatus: params.auditStatus }),
        ...(params.courseName && { courseName: params.courseName }),
      },
    });
  }

  /**
   * 上传课程封面到临时存储
   */
  static async uploadCourseLogo(courseId: number, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const url = API_URLS.COURSE.LOGO.TEMP.replace(':courseId', String(courseId));
      const response = await contentRequest.post<ApiResponse<string>>(url, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      // 返回临时文件标识
      return response.data;  // 如果使用了响应拦截器，这里应该已经是 string 类型

    } catch (error) {
      console.error('Upload error:', error);
      if (error instanceof ApiError) {
        throw new Error(`上传失败：${error.message}`);
      } else if (error instanceof Error) {
        throw new Error(`上传失败：${error.message}`);
      }
      throw new Error('上传失败：网络错误');
    }
  }

  /**
   * 确认保存课程封面
   */
  static async confirmCourseLogo(courseId: number, tempKey: string): Promise<void> {
    try {
      const url = API_URLS.COURSE.LOGO.CONFIRM.replace(':courseId', String(courseId));
      await contentRequest.post(url, null, {
        params: { tempKey }
      });
    } catch (error) {
      console.error('Confirm error:', error);
      if (error instanceof ApiError) {
        throw new Error(`确认保存失败：${error.message}`);
      } else if (error instanceof Error) {
        throw new Error(`确认保存失败：${error.message}`);
      }
      throw new Error('确认保存失败：网络错误');
    }
  }

  /**
   * 删除课程封面
   */
  static async deleteCourseLogo(courseId: number): Promise<void> {
    const url = API_URLS.COURSE.LOGO.DELETE.replace(':courseId', String(courseId));
    return contentRequest.delete(url);
  }
}

// 添加审核历史记录类型
export interface CourseAuditHistory {
  id: number;
  courseId: number;
  courseName: string;
  auditorId: number;
  auditorName: string;
  auditStatus: string;
  auditMessage?: string;
  auditTime: string;
}

// 修改 TeachplanDTO 接口定义
export interface TeachplanDTO {
  id: number;
  name: string;
  parentId: number;
  courseId: number;
  level: number;
  orderBy: number;
  children?: TeachplanDTO[] | null;
} 