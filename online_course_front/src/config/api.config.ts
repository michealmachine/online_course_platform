/**
 * API 服务配置
 */
export const API_CONFIG = {
  // 内容服务
  CONTENT_API: {
    baseURL: '/api/content-service',
    prefix: '',
    timeout: 10000,
  },
  // 媒体服务
  MEDIA_API: {
    baseURL: '/api/media-service',
    prefix: '',
    timeout: 30000,
  }
} as const;

/**
 * API 错误码定义
 */
export const API_ERROR_CODE = {
  SUCCESS: 0,
  // 通用错误码 (1xxxx)
  SYSTEM_ERROR: 10000,
  PARAM_ERROR: 10001,
  NOT_FOUND: 10002,
  NO_PERMISSION: 10003,

  // 课程相关错误码 (2xxxx)
  COURSE_NOT_EXIST: 20001,
  COURSE_DELETED: 20002,
  COURSE_STATUS_ERROR: 20003,
  COURSE_PLAN_INCOMPLETE: 20004,
  NO_TEACHER: 20005,
  PRICE_INCOMPLETE: 20006,
  AUDIT_INFO_NOT_EXIST: 20007,

  // 媒体相关错误码 (4xxxx)
  MEDIA_NOT_EXIST: 40001,
  MEDIA_NOT_AUDIT: 40002,
  MEDIA_AUDIT_REJECT: 40003,
  MEDIA_BIND_EXIST: 40004,
} as const;

/**
 * API 接口路径定义
 */
export const API_URLS = {
  // 课程相关接口
  COURSE: {
    LIST: '/course/list',                           // 管理员审核列表
    PUBLISHED_LIST: '/course/list',                 // 公开课程列表（使用status参数过滤已发布）
    ORGANIZATION_LIST: '/course/organization/:organizationId', // 机构课程列表
    CREATE: '/course',
    UPDATE: '/course',
    DETAIL: '/course/:id',
    AUDIT: '/course/audit',
    PUBLISH: '/course/:id/publish',
    OFFLINE: '/course/:courseId/offline',
    /** 管理员获取课程列表 */
    ADMIN_LIST: '/course-audit/courses',
    
    /** 提交课程审核 */
    SUBMIT_FOR_AUDIT: '/course/:courseId/audit/submit',
    
    /** 重新提交审核 */
    RESUBMIT: '/course/:courseId/resubmit',
    
    /** 审核结果 */
    AUDIT_RESULT: '/course/audit',

    /** 删除课程 */
    DELETE: '/course/:courseId',

    /** 获取课程审核状态 */
    AUDIT_STATUS: '/course/:courseId/audit-status',

    LOGO: {
      TEMP: '/course/:courseId/logo/temp',
      CONFIRM: '/course/:courseId/logo/confirm',
      DELETE: '/course/:courseId/logo',
    },
  },

  // 课程计划相关接口
  TEACHPLAN: {
    TREE: '/teachplan/tree/:courseId',
    SAVE: '/teachplan',
    DELETE: '/teachplan/:id',
    MOVE_UP: '/teachplan/moveup/:id',
    MOVE_DOWN: '/teachplan/movedown/:id',
  },

  // 课程教师相关接口
  TEACHER: {
    /** 获取机构教师列表 */
    LIST: '/course-teacher/organization/:organizationId/teachers',
    
    /** 创建/更新教师 */
    CREATE: '/course-teacher/organization/:organizationId/teachers',
    
    /** 获取教师详情 */
    DETAIL: '/course-teacher/organization/:organizationId/teachers/:teacherId',
    
    /** 删除教师 */
    DELETE: '/course-teacher/organization/:organizationId/teachers/:teacherId',
    
    /** 上传教师头像(临时) */
    UPLOAD_AVATAR_TEMP: '/course-teacher/organization/:organizationId/teachers/:teacherId/avatar/temp',
    
    /** 确认教师头像 */
    CONFIRM_AVATAR: '/course-teacher/organization/:organizationId/teachers/:teacherId/avatar/confirm',
    
    /** 删除教师头像 */
    DELETE_AVATAR: '/course-teacher/organization/:organizationId/teachers/:teacherId/avatar',
    
    /** 获取课程关联的教师列表 */
    COURSE_TEACHERS: '/course-teacher/courses/:courseId/teachers',
    
    /** 关联教师到课程 */
    ASSOCIATE_COURSE: '/course-teacher/organization/:organizationId/courses/:courseId/teachers/:teacherId',
    
    /** 解除教师与课程的关联 */
    DISSOCIATE_COURSE: '/course-teacher/organization/:organizationId/courses/:courseId/teachers/:teacherId',

    /** 获取教师关联的课程列表 */
    TEACHER_COURSES: '/course-teacher/teachers/:teacherId/courses',

    /** 教师头像相关接口 */
    AVATAR: {
      /** 上传教师头像(临时) */
      TEMP: '/course-teacher/:teacherId/avatar/temp',
      /** 确认教师头像 */
      CONFIRM: '/course-teacher/:teacherId/avatar/confirm',
      /** 删除教师头像 */
      DELETE: '/course-teacher/:teacherId/avatar'
    },
  },

  // 媒体相关接口
  MEDIA: {
    UPLOAD_LOGO: '/media/files/course/:courseId/logo',
    DELETE: '/media/files/:url',
  },

  COURSE_AUDIT: {
    // 提交审核
    SUBMIT: '/course-audit/submit/:courseId',
    // 审核历史
    HISTORY: '/course-audit/history/:courseId',
    // 待审核列表
    PENDING: '/course-audit/pending',
    // 审核操作
    APPROVE: '/course-audit/approve',
    // 审核课程详情
    DETAIL: '/course-audit/detail/:courseId',
    // 获取课程审核状态
    STATUS: '/course-audit/status/:courseId',
    LIST: '/course-audit/courses',
  },

  // 媒体文件上传相关接口
  MEDIA_UPLOAD: {
    /** 初始化分片上传 */
    INITIATE: '/api/media/upload/initiate',
    
    /** 获取分片上传预签名URL */
    PRESIGNED_URL: '/api/media/upload/presigned-url',
    
    /** 完成分片上传 */
    COMPLETE: '/api/media/upload/complete',

    /** 取消分片上传 */
    ABORT: '/api/media/upload/abort',

    /** 查询上传状态 */
    STATUS: '/api/media/upload/status',
  },

  // 媒资文件管理相关接口
  MEDIA_FILE: {
    /** 上传图片到临时存储 */
    UPLOAD_IMAGE_TEMP: '/api/media/images/temp',
    
    /** 更新临时存储的图片 */
    UPDATE_TEMP: '/api/media/temp/:tempKey',
    
    /** 保存临时文件到永久存储 */
    SAVE_TEMP: '/api/media/temp/save',
    
    /** 删除媒体文件 */
    DELETE: '/api/media/files',
  }
} as const; 