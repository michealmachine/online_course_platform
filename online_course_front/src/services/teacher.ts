import { contentRequest } from './index';
import { API_URLS } from '@/config/api.config';
import { PageParams, PageResult, CourseBase } from './course';

/**
 * 教师信息接口
 */
export interface CourseTeacher {
  id: number;
  name: string;
  position: string;
  description: string;
  avatar?: string;
  organizationId?: number;
}

/**
 * 教师查询参数
 */
export interface QueryTeacherParams {
  name?: string;
  position?: string;
}

/**
 * 创建教师参数
 */
export interface CreateTeacherParams {
  name: string;
  position: string;
  description: string;
  organizationId: number;
  avatar?: string;
}

/**
 * 教师服务类
 */
export class TeacherService {
  /**
   * 获取课程关联的教师列表
   */
  static async getTeachersByCourse(courseId: number): Promise<CourseTeacher[]> {
    const url = API_URLS.TEACHER.COURSE_TEACHERS
      .replace(':courseId', BigInt(courseId).toString());
    const data = await contentRequest.get(url);
    return data.items;
  }

  /**
   * 获取机构教师列表
   */
  static async getTeacherList(
    organizationId: number,
    params?: PageParams
  ): Promise<PageResult<CourseTeacher>> {
    const url = API_URLS.TEACHER.LIST.replace(':organizationId', String(organizationId));
    return contentRequest.get(url, { params });
  }

  /**
   * 创建教师
   */
  static async createTeacher(data: CreateTeacherParams): Promise<number> {
    const url = API_URLS.TEACHER.CREATE
      .replace(':organizationId', String(data.organizationId));
    return contentRequest.post(url, data);
  }

  /**
   * 更新教师
   */
  static async updateTeacher(data: CourseTeacher): Promise<void> {
    const url = API_URLS.TEACHER.CREATE
      .replace(':organizationId', String(data.organizationId));
    return contentRequest.post(url, data);
  }

  /**
   * 获取教师详情
   */
  static async getTeacherDetail(organizationId: number, teacherId: number): Promise<CourseTeacher> {
    const url = API_URLS.TEACHER.DETAIL
      .replace(':organizationId', String(organizationId))
      .replace(':teacherId', String(teacherId));
    return contentRequest.get(url);
  }

  /**
   * 删除教师
   */
  static async deleteTeacher(organizationId: number, teacherId: number): Promise<void> {
    const url = API_URLS.TEACHER.DELETE
      .replace(':organizationId', String(organizationId))
      .replace(':teacherId', String(teacherId));
    return contentRequest.delete(url);
  }

  /**
   * 上传教师头像(临时)
   */
  static async uploadTeacherAvatarTemp(organizationId: number, teacherId: number, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    const url = API_URLS.TEACHER.UPLOAD_AVATAR_TEMP
      .replace(':organizationId', String(organizationId))
      .replace(':teacherId', String(teacherId));
    return contentRequest.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  }

  /**
   * 确认教师头像
   */
  static async confirmTeacherAvatar(organizationId: number, teacherId: number, tempKey: string): Promise<void> {
    const url = API_URLS.TEACHER.CONFIRM_AVATAR
      .replace(':organizationId', String(organizationId))
      .replace(':teacherId', String(teacherId));
    return contentRequest.post(url, { tempKey });
  }

  /**
   * 删除教师头像
   */
  static async deleteTeacherAvatar(organizationId: number, teacherId: number): Promise<void> {
    const url = API_URLS.TEACHER.DELETE_AVATAR
      .replace(':organizationId', String(organizationId))
      .replace(':teacherId', String(teacherId));
    return contentRequest.delete(url);
  }

  /**
   * 获取教师关联的课程列表
   */
  static async getTeacherCourses(
    teacherId: number,
    params: PageParams
  ): Promise<PageResult<CourseBase>> {
    const url = API_URLS.TEACHER.TEACHER_COURSES
      .replace(':teacherId', BigInt(teacherId).toString());
    return contentRequest.get(url, { params });
  }

  /**
   * 关联教师到课程
   */
  static async associateTeacherToCourse(
    organizationId: number,
    courseId: number,
    teacherId: number
  ): Promise<void> {
    const url = API_URLS.TEACHER.ASSOCIATE_COURSE
      .replace(':organizationId', BigInt(organizationId).toString())
      .replace(':courseId', BigInt(courseId).toString())
      .replace(':teacherId', BigInt(teacherId).toString());
    return contentRequest.post(url);
  }

  /**
   * 解除教师与课程的关联
   */
  static async dissociateTeacherFromCourse(
    organizationId: number,
    courseId: number,
    teacherId: number
  ): Promise<void> {
    const url = API_URLS.TEACHER.DISSOCIATE_COURSE
      .replace(':organizationId', BigInt(organizationId).toString())
      .replace(':courseId', BigInt(courseId).toString())
      .replace(':teacherId', BigInt(teacherId).toString());
    return contentRequest.delete(url);
  }
} 