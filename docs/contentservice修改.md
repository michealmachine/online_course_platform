# Content Service 服务设计文档 (更新)

## 一、前期鉴权方案 (临时方案 - 后期迁移到 OAuth2)

在认证服务器搭建之前，可以考虑以下临时鉴权方案：

### 1. Spring Security 集成
- 在各微服务中配置 Spring Security，为静态资源和接口加入基本保护
- 先开发简单的用户名密码和角色校验，后期再迁移到 OAuth2 认证

### 2. JWT 临时方案
- 利用 JWT 搭建轻量级鉴权机制，保持服务无状态
- JWT 存储基本用户信息和权限，后续可平滑迁移到 OAuth2

### 3. Spring Cloud Security
- 开发初期使用 Spring Cloud Security 确保微服务间安全通信
- 后续整合 OAuth2 时注意对 JWT 的处理调整

### 4. 权限策略
- 提前定义各模块角色权限（管理员、审核人员、机构用户等）
- 配置 Spring Security 拦截器实现基于角色的访问控制

## 二、角色权限设计 (RBAC)

### 1. 管理员 (Admin)

*   **角色标识 (Spring Security Role):** `ROLE_ADMIN`
*   **主要职责:** 平台全局管理和维护，拥有最高权限，可以管理所有机构和平台数据。
*   **权限范围:**
    *   **课程管理 (Course Management):**
        *   **所有 `/course/**` 接口:**  拥有所有课程管理相关接口的权限，包括课程基本信息、营销信息、状态管理、分类管理、机构课程管理、审核管理、发布管理等。
        *   例如：
            *   `POST /course`: 创建课程
            *   `PUT /course/{id}`: 修改课程
            *   `DELETE /course/{id}`: 删除课程
            *   `GET /course/list`: 查询课程列表
            *   `POST /course/category`: 创建课程分类
            *   `PUT /course/category/{id}`: 修改课程分类
            *   `GET /course/category/list`: 查询课程分类列表
            *   `POST /course/organization/bind`: 机构绑定课程
            *   `GET /course/audit/list`: 查询待审核课程列表
            *   `POST /course/publish/{id}`: 发布课程 (待实现)
    *   **课程计划管理 (Teachplan Management):**
        *   **所有 `/teachplan/**` 接口:** 拥有所有课程计划管理相关接口的权限，包括课程章节和小节的管理。
        *   例如：
            *   `POST /teachplan`: 创建课程计划
            *   `PUT /teachplan/{id}`: 修改课程计划
            *   `DELETE /teachplan/{id}`: 删除课程计划
            *   `GET /teachplan/tree/{courseId}`: 查询课程计划树
    *   **课程教师管理 (Course Teacher Management):**
        *   **所有 `/course/teacher/**` 接口:** 拥有所有课程教师管理相关接口的权限。
        *   例如：
            *   `POST /course/teacher`: 添加课程教师
            *   `DELETE /course/teacher/{courseId}/{teacherId}`: 移除课程教师
            *   `GET /course/teacher/list/{courseId}`: 查询课程教师列表
    *   **媒资管理 (Media Management):**
        *   **部分 `/media/**` 接口 (平台级别):**  拥有平台级别媒资管理接口的权限 (具体接口需要进一步细化)。
        *   例如：
            *   `POST /media/upload/platform`: 平台级别媒资上传 (假设)
            *   `DELETE /media/platform/{mediaId}`: 删除平台级别媒资 (假设)
    *   **系统管理 (System Management):**
        *   **所有 `/system/**` 接口:** 拥有系统管理相关接口的权限，例如系统配置管理。
        *   例如：
            *   `GET /system/config`: 获取系统配置
            *   `PUT /system/config`: 修改系统配置
    *   **监控和日志 (Monitoring and Logging):**
        *   **`/monitor/**`, `/log/**` 等接口 (假设):**  拥有查看监控信息和日志的权限 (具体接口需要根据实际情况设计)。

### 2. 审核人员 (Auditor)

*   **角色标识 (Spring Security Role):** `ROLE_AUDITOR`
*   **主要职责:** 负责课程内容的审核，确保课程内容符合平台规范和质量标准。
*   **权限范围:**
    *   **课程审核管理 (Course Audit Management):**
        *   **`/course/audit/**` 接口:**  拥有课程审核相关接口的权限。
        *   例如：
            *   `GET /course/audit/list`: 查询待审核课程列表
            *   `GET /course/audit/{courseId}`: 查看课程审核详情
            *   `POST /course/audit/pass/{courseId}`: 审核通过
            *   `POST /course/audit/reject/{courseId}`: 审核拒绝
    *   **课程信息查看 (Course Information View):**
        *   **部分 `/course/**` 接口 (只读权限):**  拥有部分课程信息查看接口的权限，用于审核时查看课程内容。
        *   例如：
            *   `GET /course/{id}`: 查看课程详情 (基本信息、计划、教师等)
    *   **限制:**
        *   **不能创建、修改、删除课程。**
        *   **不能管理课程计划和教师信息。**
        *   **不能进行媒资管理。**
        *   **不能进行系统配置和平台级别媒资管理。**

### 3. 机构用户 (Organization User)

*   **角色标识 (Spring Security Role):** `ROLE_ORG_USER`
*   **主要职责:** 管理自己所属机构的课程、教师等内容，负责机构的日常内容运营。
*   **权限范围:**
    *   **课程管理 (Course Management - 机构级别):**
        *   **机构级别 `/course/**` 接口:**  可以管理自己机构的课程，例如创建、修改、删除课程，发布、下架课程等。
        *   例如：
            *   `POST /course`: 创建课程 (机构级别)
            *   `PUT /course/{id}`: 修改课程 (机构级别)
            *   `DELETE /course/{id}`: 删除课程 (机构级别)
            *   `POST /course/{courseId}/publish`: 发布课程 (机构级别)
            *   `POST /course/{courseId}/offline`: 下架课程 (机构级别)
            *   `POST /course/{courseId}/logo/temp`: 上传课程封面 (机构级别)
            *   `POST /course/{courseId}/logo/confirm`: 确认课程封面 (机构级别)
            *   `DELETE /course/{courseId}/logo`: 删除课程封面 (机构级别)
    *   **课程计划管理 (Teachplan Management - 机构级别):**
        *   **机构级别 `/teachplan/**` 接口:** 可以管理自己机构课程的课程计划。
        *   例如：
            *   `POST /teachplan`: 创建课程计划 (机构级别)
            *   `PUT /teachplan/{id}`: 修改课程计划 (机构级别)
            *   `DELETE /teachplan/{id}`: 删除课程计划 (机构级别)
            *   `POST /teachplan/moveup/{teachplanId}`: 上移课程计划 (机构级别)
            *   `POST /teachplan/movedown/{teachplanId}`: 下移课程计划 (机构级别)
            *   `POST /teachplan/saveorder`: 保存排序 (机构级别)
            *   `POST /teachplan/discardorder`: 丢弃排序 (机构级别)
    *   **课程教师管理 (Course Teacher Management - 机构级别):**
        *   **机构级别 `/course-teacher/**` 接口:** 可以管理自己机构的教师信息和课程教师关联关系。
        *   例如：
            *   `GET /course-teacher/organization/{organizationId}/teachers`: 查询机构教师 (机构级别)
            *   `GET /course-teacher/organization/{organizationId}/teachers/{teacherId}`: 查询教师详情 (机构级别)
            *   `POST /course-teacher/organization/{organizationId}/teachers`: 保存教师信息 (机构级别)
            *   `DELETE /course-teacher/organization/{organizationId}/teachers/{teacherId}`: 删除教师 (机构级别)
            *   `POST /course-teacher/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}`: 关联教师到课程 (机构级别)
            *   `DELETE /course-teacher/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}`: 解除教师课程关联 (机构级别)
            *   `POST /course-teacher/{teacherId}/avatar/temp`: 上传教师头像 (机构级别)
            *   `POST /course-teacher/{teacherId}/avatar/confirm`: 确认教师头像 (机构级别)
            *   `DELETE /course-teacher/{teacherId}/avatar`: 删除教师头像 (机构级别)
    *   **课程计划媒资管理 (Teachplan Media Management - 机构级别):**
        *   **机构级别 `/teachplan-media/**` 接口:** 可以管理课程计划和媒资的关联关系。
        *   例如：
            *   `POST /teachplan-media`: 绑定课程计划与媒资 (机构级别)
            *   `DELETE /teachplan-media/{teachplanId}/{mediaId}`: 解除媒资绑定 (机构级别)
    *   **媒资文件管理 (Media File Management - 机构级别):**
        *   **机构级别 `/media-files/**` 接口:** 可以管理自己机构的媒资文件。
        *   例如：
            *   `POST /media-files`: 保存媒资文件 (机构级别)
            *   `GET /media-files/list/{organizationId}`: 查询媒资文件列表 (机构级别)
    *   **限制:**
        *   **只能管理自己机构的数据，不能跨机构操作。**
        *   **不能进行课程审核。**
        *   **不能进行平台全局管理。**

### 4. 普通用户 (User)

*   **角色标识 (Spring Security Role):** `ROLE_USER` (或者可以考虑不分配角色，使用匿名访问 para 公开接口)
*   **主要职责:** 浏览和学习课程内容 (如果需要用户角色才能访问公开课程)。
*   **权限范围:**
    *   **课程浏览 (Course Browsing - 公开接口):**
        *   **公开 `/course/**` 接口 (只读):**  可以访问公开的课程浏览接口，例如查询课程列表、课程详情、课程分类等。
        *   例如：
            *   `GET /course/list`: 查询课程列表 (公开)
            *   `GET /course/{courseId}`: 查看课程详情 (公开)
            *   `GET /course/category/tree`: 查询课程分类列表 (公开)
            *   `GET /course/preview/{courseId}`: 课程预览 (公开)
    *   **课程教师信息查看 (Course Teacher Information - 公开接口):**
        *   **公开 `/course-teacher/**` 接口 (只读):** 可以访问公开的课程教师信息接口。
        *   例如：
            *   `GET /course-teacher/courses/{courseId}/teachers`: 查询课程教师列表 (公开)
            *   `GET /course-teacher/teachers/{teacherId}/courses`: 查询教师关联课程 (公开)
    *   **课程计划媒资信息查看 (Teachplan Media Information - 公开接口):**
        *   **公开 `/teachplan-media/**` 接口 (只读):** 可以访问公开的课程计划媒资信息接口。
        *   例如：
            *   `GET /teachplan-media/{teachplanId}`: 获取课程计划媒资列表 (公开)
    *   **媒资文件访问 (Media File Access - 公开接口 - 需谨慎评估):**
        *   **公开 `/media-files/url/**` 接口 (只读 - 需评估):**  是否公开媒资文件访问地址需要谨慎评估，如果公开，则普通用户可以访问。
        *   例如：
            *   `GET /media-files/url/{organizationId}/{mediaFileId}`: 获取媒资文件 URL (是否公开需评估)
    *   **限制:**
        *   **权限非常有限，主要为只读的公开接口。**
        *   **不能进行任何课程、计划、教师、媒资的管理操作。**
        *   **不能进行课程审核和系统管理。**

## 三、API 接口权限分配 (详细)

### 1. 课程审核接口 (CourseAuditController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /course-audit/submit/{courseId} | POST | ROLE_ORG_USER | 机构用户提交课程审核 |
| /course-audit/approve | POST | ROLE_AUDITOR | 审核人员审核课程 |
| /course-audit/pending | GET | ROLE_AUDITOR, ROLE_ADMIN | 审核人员和管理员查看待审核课程 |
| /course-audit/history/{courseId} | GET | ROLE_AUDITOR, ROLE_ADMIN, ROLE_ORG_USER | 审核人员、管理员、机构用户查看审核历史 |
| /course-audit/history/auditor/{auditorId} | GET | ROLE_ADMIN | 管理员查看审核人员的审核历史 |

### 2. 课程管理接口 (CourseController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /course | POST | ROLE_ORG_USER | 机构用户创建课程 |
| /course | PUT | ROLE_ORG_USER | 机构用户修改课程 |
| /course/category/tree | GET | 公开 | 公开查询课程分类 |
| /course/preview/{courseId} | GET | 公开 | 公开课程预览 |
| /course/{courseId} | GET | 公开 | 公开查询课程详情 |
| /course/{courseId} | DELETE | ROLE_ORG_USER | 机构用户删除课程 |
| /course/{courseId}/publish | POST | ROLE_ORG_USER | 机构用户发布课程 |
| /course/{courseId}/offline | POST | ROLE_ORG_USER, ROLE_ADMIN | 机构用户和管理员下架课程 |
| /course/organization/{organizationId} | GET | ROLE_ORG_USER, ROLE_ADMIN | 机构用户和管理员查询机构课程列表 |
| /course/{courseId}/logo/temp | POST | ROLE_ORG_USER | 机构用户上传课程封面 |
| /course/{courseId}/logo/confirm | POST | ROLE_ORG_USER | 机构用户确认课程封面 |
| /course/{courseId}/logo | DELETE | ROLE_ORG_USER | 机构用户删除课程封面 |
| /course/admin/list | GET | ROLE_ADMIN | 管理员查询所有课程列表 |
| /course/list | GET | 公开 | 公开查询已审核课程列表 |

### 3. 课程教师接口 (CourseTeacherController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /course-teacher/organization/{orgId}/teachers | GET | ROLE_ORG_USER, ROLE_ADMIN | 机构用户和管理员查询机构教师列表 |
| /course-teacher/organization/{orgId}/teachers/{teacherId} | GET | ROLE_ORG_USER, ROLE_ADMIN | 机构用户和管理员查询教师详情 |
| /course-teacher/organization/{orgId}/teachers | POST | ROLE_ORG_USER | 机构用户保存教师信息 |
| /course-teacher/organization/{orgId}/teachers/{teacherId} | DELETE | ROLE_ORG_USER | 机构用户删除教师 |
| /course-teacher/organization/{orgId}/courses/{courseId}/teachers/{teacherId} | POST | ROLE_ORG_USER | 机构用户关联教师到课程 |
| /course-teacher/organization/{orgId}/courses/{courseId}/teachers/{teacherId} | DELETE | ROLE_ORG_USER | 机构用户解除教师课程关联 |
| /course-teacher/courses/{courseId}/teachers | GET | 公开 | 公开查询课程教师列表 |
| /course-teacher/teachers/{teacherId}/courses | GET | 公开 | 公开查询教师关联课程 |
| /course-teacher/{teacherId}/avatar/temp | POST | ROLE_ORG_USER | 机构用户上传教师头像 |
| /course-teacher/{teacherId}/avatar/confirm | POST | ROLE_ORG_USER | 机构用户确认教师头像 |
| /course-teacher/{teacherId}/avatar | DELETE | ROLE_ORG_USER | 机构用户删除教师头像 |

### 4. 课程计划媒资接口 (TeachplanMediaController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /teachplan-media | POST | ROLE_ORG_USER | 机构用户绑定课程计划媒资 |
| /teachplan-media/{teachplanId}/{mediaId} | DELETE | ROLE_ORG_USER | 机构用户解绑课程计划媒资 |
| /teachplan-media/{teachplanId} | GET | 公开 | 公开查询课程计划媒资列表 |

### 5. 媒资文件接口 (MediaFileController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /media-files | POST | ROLE_ORG_USER | 机构用户保存媒资文件 |
| /media-files/list/{organizationId} | GET | ROLE_ORG_USER, ROLE_ADMIN | 机构用户和管理员查询媒资文件列表 |
| /media-files/url/{organizationId}/{mediaFileId} | GET | 公开 (需评估) | 公开查询媒资文件URL (需评估是否需要权限控制) |

### 6. 课程计划接口 (TeachplanController)
| 接口路径 | 方法 | 权限角色 | 说明 |
|---------|------|---------|------|
| /teachplan/tree/{courseId} | GET | 公开 | 公开查询课程计划树 |
| /teachplan | POST | ROLE_ORG_USER | 机构用户保存课程计划 |
| /teachplan/{teachplanId} | DELETE | ROLE_ORG_USER | 机构用户删除课程计划 |
| /teachplan/moveup/{teachplanId} | POST | ROLE_ORG_USER | 机构用户上移课程计划 |
| /teachplan/movedown/{teachplanId} | POST | ROLE_ORG_USER | 机构用户下移课程计划 |
| /teachplan/saveorder | POST | ROLE_ORG_USER | 机构用户保存课程计划排序 |
| /teachplan/discardorder | POST | ROLE_ORG_USER | 机构用户丢弃课程计划排序 |
| /teachplan/media | POST | ROLE_ORG_USER | 机构用户绑定课程计划媒资(重复接口) |
| /teachplan/media/{teachplanId}/{mediaId} | DELETE | ROLE_ORG_USER | 机构用户解绑课程计划媒资(重复接口) |

## 四、鉴权引入计划 (设计到实践)

1.  **环境准备:**
    *   添加 Spring Security 和 JWT 依赖到 `pom.xml`。
    *   创建 `JwtUtils` 工具类，用于 JWT 的生成、解析和验证。

2.  **用户认证 (Login):**
    *   设计并实现登录 API (`POST /auth/login`)，用于用户身份验证和 JWT Token 生成。
    *   定义用户角色信息 (Admin, Auditor, Org User, User)，可以先使用简单的内存用户数据或模拟实现。

3.  **Spring Security 配置:**
    *   创建 `SecurityConfig` 类，配置 Spring Security 框架：
        *   定义需要认证的 API 接口 (大部分 `content_service` 接口，除了公开接口如课程浏览、分类查询等)。
        *   配置角色权限映射，将 API 接口与角色关联起来 (参考上面的 API 权限分配)。
        *   配置 JWT 认证过滤器 (`JwtAuthenticationFilter`)，并将其添加到 Spring Security 过滤器链中。

4.  **JWT 认证过滤器 (`JwtAuthenticationFilter`) 实现:**
    *   在 `JwtAuthenticationFilter` 中，从请求头 (例如 `Authorization: Bearer <token>`) 中获取 JWT Token。
    *   使用 `JwtUtils` 解析和验证 Token。
    *   如果 Token 有效，从 Token 中提取用户信息和角色信息，并将其设置到 `SecurityContextHolder`，以便后续权限判断使用。

5.  **权限控制实现:**
    *   **方法级别权限控制:** 在 Controller 方法上使用 Spring Security 的 `@PreAuthorize` 注解，根据 API 权限分配表格，配置相应的角色要求。
        *   例如：`@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")`, `@PreAuthorize("hasRole('ORG_USER')")`。
    *   **细粒度权限控制 (可选):**  对于需要更细粒度权限控制的场景 (例如机构用户只能操作自己机构的数据)，可以结合 SpEL 表达式和自定义的权限判断组件 (例如 `OrganizationSecurity`) 来实现。

6.  **异常处理:**
    *   配置全局异常处理，处理认证失败 (`AuthenticationException`) 和权限不足 (`AccessDeniedException`) 异常，返回统一的、友好的错误响应。

7.  **测试和验证:**
    *   进行单元测试，测试 JWT 工具类、过滤器、权限控制逻辑的正确性。
    *   进行集成测试，验证 API 接口鉴权是否生效，不同角色访问不同接口是否符合预期权限。

## 五、实施建议

*   **优先实现核心功能鉴权:**  先为课程管理、课程审核、课程计划等核心功能模块的 API 接口添加权限控制，确保核心业务流程的安全性。
*   **逐步完善权限控制:**  在核心功能鉴权完成后，再逐步为其他模块 (例如教师管理、媒资管理) 的 API 接口添加权限控制。
*   **充分测试:**  在每个阶段的鉴权改造完成后，都进行充分的测试，包括单元测试和集成测试，确保鉴权逻辑的正确性和稳定性。
*   **考虑性能影响:**  在引入 Spring Security 和 JWT 鉴权后，需要关注对系统性能的影响，特别是 JWT 的解析和验证过程。可以考虑使用缓存等优化手段来提升性能。
*   **API 文档更新:**  在完成鉴权改造后，及时更新 API 文档，明确标示每个 API 接口所需的角色权限，方便前端开发和第三方集成。
*   **重复 API 接口处理:**  注意 `TeachplanController` 和 `TeachplanMediaController` 中重复的媒资绑定/解绑 API 接口 (`/teachplan/media` 和 `/teachplan-media`)，需要评估是否需要合并或移除重复接口，避免 API 设计的冗余和混乱。
