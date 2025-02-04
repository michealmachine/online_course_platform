# 内容管理服务(Content Service)文档

## 1. 功能概述

### 1.1 已实现功能
- 课程基本信息管理（CRUD）
  - 课程创建（包含基本信息和营销信息）
  - 课程修改
  - 分页查询（支持多条件筛选）
  - 课程预览
- 机构课程管理
  - 查询机构课程列表（支持状态筛选）
  - 重新提交审核
  - 课程状态管理（发布、下架）
- 课程分类管理
  - 树形结构查询
  - 支持两级分类（大类和小类）
- 课程计划管理
  - 章节管理（一级）
  - 小节管理（二级）
  - 树形结构展示
  - 排序功能
  - 章节上下移动
- 课程教师管理
  - 教师信息的增删改查
  - 支持多个教师
- 课程审核流程
  - 提交审核
  - 审核状态变更
  - 审核意见管理
- 媒资关联管理
  - 媒资绑定到课程计划
  - 支持解除绑定
  - 媒资信息查询
- 课程封面管理
  - 更新课程封面
  - 删除课程封面
  - 与媒资服务集成
  - 跨服务文件管理
- API文档
  - 集成Swagger
  - 详细的接口说明
  - 完整的参数描述

### 1.2 待实现功能
- 课程发布功能
- 课程统计功能
- 课程内容版本控制
- 课程评价管理

### 1.2 API 接口说明
#### 机构课程管理接口
- GET /course/organization/{organizationId} - 查询机构课程列表
  - 支持按课程状态筛选
  - 支持按审核状态筛选
  - 分页查询
- POST /course/{courseId}/resubmit - 重新提交审核
  - 仅支持审核不通过的课程重新提交
  - 后续会加入认证和鉴权机制

## 2. 技术架构

### 2.1 核心技术栈
- Spring Boot 3.x：基础框架
- Spring Data JPA：数据访问层
- ModelMapper：对象映射工具
- MySQL：数据存储
- JUnit 5：单元测试
- Validation：参数校验
- Swagger/OpenAPI：接口文档
- GitHub Actions：CI/CD

### 2.2 项目结构
```angular2html
content_service/
├── controller/ # 控制层，处理HTTP请求
│ ├── CourseController.java
│ ├── TeachplanController.java
│ └── CourseTeacherController.java
├── service/ # 业务层，实现核心逻辑
│ ├── CourseBaseService.java
│ ├── TeachplanService.java
│ └── CourseTeacherService.java
├── repository/ # 数据访问层，与数据库交互
│ ├── CourseBaseRepository.java
│ ├── TeachplanRepository.java
│ └── CourseTeacherRepository.java
├── entity/ # 实体类，映射数据库表
│ ├── CourseBase.java # 课程基本信息
│ ├── CourseMarket.java # 课程营销信息
│ ├── CourseTeacher.java # 课程教师信息
│ └── Teachplan.java # 课程计划
├── dto/ # 数据传输对象
│ ├── request/ # 请求DTO
│ └── response/ # 响应DTO
└── config/ # 配置类
```


## 3. 数据模型设计

### 3.1 核心实体关系
#### CourseBase (课程基本信息)
- 主要属性：
  - id: 课程ID（主键）
  - name: 课程名称
  - brief: 课程简介
  - logo: 课程封面图片URL
  - mt/st: 课程分类（大类/小类）
  - status: 课程状态
  - valid: 是否有效

- 关联关系：
  1. CourseMarket (课程营销信息) - 一对一关系
     - 共享主键，使用课程ID作为关联
     - 级联保存和删除
     - 包含价格、收费方式等营销信息
  
  2. CourseTeacher (课程教师) - 一对多关系
     - 通过course_id外键关联
     - 一个课程可以有多个教师
     - 教师删除不影响课程
  
  3. Teachplan (课程计划) - 一对多关系
     - 通过course_id外键关联
     - 使用parent_id实现树形结构
     - 支持两级结构（章节-小节）
  
  4. CoursePublish (课程发布) - 一对一关系
     - 共享主键，使用课程ID
     - 包含发布状态、发布时间等信息
  
  5. CoursePublishPre (课程预发布) - 一对一关系
     - 共享主键，使用课程ID
     - 用于课程审核流程
     - 包含审核状态、审核意见等信息

#### Teachplan (课程计划)
- 主要属性：
  - id: 计划ID（主键）
  - name: 章节/小节名称
  - courseId: 所属课程ID
  - parentId: 父节点ID
  - level: 层级（1:章节，2:小节）
  - orderBy: 排序号

- 关联关系：
  1. CourseBase - 多对一关系
     - 通过course_id关联到课程
  
  2. TeachplanMedia (媒资关联) - 一对一关系
     - 只有小节（level=2）可以关联媒资
     - 级联删除关系

#### CourseTeacher (课程教师)
- 主要属性：
  - id: 教师ID（主键）
  - courseId: 所属课程ID
  - name: 教师名称
  - position: 职位
  - description: 简介

- 关联关系：
  1. CourseBase - 多对一关系
     - 通过course_id关联到课程
     - 删除课程时级联删除教师

### 3.2 状态定义
- 课程状态：
    - 202001：未发布
    - 202002：已发布
    - 202003：已下线
对应枚举类：CourseStatusEnum

- 审核状态：
    - 202301：已提交审核
    - 202302：审核不通过
    - 202303：审核通过
对应枚举类：CourseAuditStatusEnum

- 收费规则：
    - 201001：免费
    - 201002：收费
对应枚举类：CourseChargeEnum

媒资相关状态：
审核状态（MediaAuditStatusEnum）：
  - 1：未审核
  - 2：审核中
  - 3：审核通过
  - 4：审核不通过

文件状态（MediaStatusEnum）：
  - 1：上传中
  - 2：上传完成
  - 3：上传失败
  - 4：处理中
  - 5：处理成功
  - 6：处理失败

## 4. API接口设计

### 4.1 课程管理接口
#### 4.1.1 查询课程列表
```http
GET /content/course/list
```

**请求参数:**
- organizationId: 机构ID (必填)
- courseName: 课程名称 (可选)
- status: 课程状态 (可选)
- pageNo: 页码，从1开始 (必填)
- pageSize: 每页记录数 (必填)

**响应格式:**
```json
{
    "items": [
        {
            "id": 1,
            "name": "课程名称",
            // ... 其他课程属性
        }
    ],
    "counts": 100,
    "page": 1,
    "pageSize": 10
}
```

#### 4.1.2 创建课程
ttp
POST /course
请求体：
{
"name": "课程名称",
"brief": "课程简介",
"mt": 1,
"st": 2,
"charge": "201001",
"price": 0,
"valid": true
}

#### 4.1.4 课程封面管理接口
```http
# 上传课程封面到临时存储
POST /course/{courseId}/logo/temp
Content-Type: multipart/form-data

请求参数：
- file: 封面图片文件

响应：
{
  "code": 0,
  "message": "success",
  "data": "temp-key-123" // 临时存储key
}

# 确认并保存临时课程封面
POST /course/{courseId}/logo/confirm
Content-Type: application/json

请求参数：
- tempKey: 临时存储key

响应：
{
  "code": 0,
  "message": "success"
}

# 删除课程封面
DELETE /course/{courseId}/logo

响应：
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.5 教师头像管理接口
```http
# 上传教师头像到临时存储
POST /course-teacher/{teacherId}/avatar/temp
Content-Type: multipart/form-data

请求参数：
- file: 头像图片文件

响应：
{
  "code": 0,
  "message": "success",
  "data": "temp-key-123" // 临时存储key
}

# 确认并保存临时头像
POST /course-teacher/{teacherId}/avatar/confirm
Content-Type: application/json

请求参数：
- tempKey: 临时存储key

响应：
{
  "code": 0,
  "message": "success"
}

# 删除教师头像
DELETE /course-teacher/{teacherId}/avatar

响应：
{
  "code": 0,
  "message": "success"
}
```

#### 4.1.8 课程封面管理
```http
# 上传课程封面到临时存储
POST /course/{courseId}/logo/temp
Content-Type: multipart/form-data

# 确认并保存临时课程封面
POST /course/{courseId}/logo/confirm

# 删除课程封面
DELETE /course/{courseId}/logo
```

#### 4.1.9 课程状态管理
```http
# 获取所有课程状态
GET /course/status/all

响应：
{
    "code": "0",
    "message": "success",
    "data": {
        "courseStatus": {
            "202001": "未发布",
            "202002": "已发布",
            "202003": "已下架"
        },
        "auditStatus": {
            "202301": "未提交",
            "202302": "已提交",
            "202303": "审核通过",
            "202304": "审核不通过"
        }
    }
}
```

#### 4.1.10 管理员接口
```http
# 管理员查询所有课程列表（需要管理员权限）
GET /course/admin/list

请求参数：
- organizationId: 机构ID（可选）
- status: 课程状态（可选）
- auditStatus: 审核状态（可选）
- courseName: 课程名称（可选）
- pageNo: 页码
- pageSize: 每页大小

响应：
{
    "code": "0",
    "message": "success",
    "data": {
        "items": [
            {
                "id": 1,
                "name": "课程名称",
                "organizationId": 1234,
                "organizationName": "测试机构",
                "status": "202001",
                "auditStatus": "202301"
            }
        ],
        "total": 100,
        "pageNo": 1,
        "pageSize": 10
    }
}
```

### 4.2 课程计划接口
#### 4.2.1 查询课程计划树
http
GET /teachplan/tree/{courseId}

#### 4.2.2 保存课程计划
http
POST /teachplan
请求体：
{
"courseId": 1,
"parentId": 0,
"name": "章节名称",
"level": 1,
"orderBy": 1
}

### 4.3 教师管理接口

#### 4.3.1 教师基本信息管理
```http
# 创建/更新教师
POST /course-teacher/organization/{organizationId}/teachers
请求体：
{
    "name": "张老师",
    "position": "高级讲师",
    "description": "教师简介"
}
响应：
{
    "code": "0",
    "message": "success",
    "data": 123  // 教师ID
}

# 删除教师
DELETE /course-teacher/organization/{organizationId}/teachers/{teacherId}

# 查询教师详情
GET /course-teacher/organization/{organizationId}/teachers/{teacherId}

# 查询机构教师列表
GET /course-teacher/organization/{organizationId}/teachers
```

#### 4.3.2 教师课程关联管理
```http
# 关联教师到课程
POST /course-teacher/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}

# 解除教师与课程的关联
DELETE /course-teacher/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}

# 查询课程的教师列表
GET /course-teacher/courses/{courseId}/teachers

# 查询教师关联的课程列表
GET /course-teacher/teachers/{teacherId}/courses
```

#### 4.3.3 教师头像管理
```http
# 上传教师头像（两步式上传）
POST /course-teacher/teachers/{teacherId}/avatar/temp
Content-Type: multipart/form-data

# 确认保存头像
POST /course-teacher/teachers/{teacherId}/avatar/confirm
请求体：
{
    "tempKey": "xxx"
}

# 删除教师头像
DELETE /course-teacher/teachers/{teacherId}/avatar
```

## 5. 业务实现细节

### 5.1 课程创建流程
1. 接收AddCourseDTO
2. 验证必要字段
3. 创建CourseBase实体
4. 创建关联的CourseMarket实体
5. 设置初始状态和时间戳
6. 保存到数据库并返回课程ID

### 5.2 课程计划树形结构实现
1. 查询所有课程计划记录
2. 使用Map存储节点引用
3. 遍历记录构建父子关系
4. 返回顶层节点列表

### 5.3 审核流程实现
1. 提交审核
    - 验证课程信息完整性
    - 创建预发布记录
    - 更新审核状态
2. 审核操作
    - 更新审核状态
    - 记录审核意见
    - 触发后续流程

### 5.4 课程封面管理流程

```mermaid
sequenceDiagram
    participant Client as 前端
    participant Content as Content Service
    participant Media as Media Service
    participant Redis as Redis临时存储
    participant MinIO as MinIO永久存储
    participant DB as Database

    Client->>Content: 上传课程封面请求
    Content->>Media: Feign调用临时上传接口
    Media->>Media: 校验文件
    Media->>Redis: 存储临时文件
    Media-->>Content: 返回临时key
    Content-->>Client: 返回临时key
    
    Client->>Content: 确认保存请求
    Content->>Media: Feign调用永久保存接口
    Media->>Redis: 获取临时文件
    Media->>MinIO: 保存到永久存储
    Media->>DB: 保存媒资记录
    Media-->>Content: 返回访问URL
    Content->>DB: 更新课程封面URL
    Content-->>Client: 返回处理结果
```

### 5.5 教师头像管理流程

```mermaid
sequenceDiagram
    participant Client as 前端
    participant Content as Content Service
    participant Media as Media Service
    participant Redis as Redis临时存储
    participant MinIO as MinIO永久存储
    participant DB as Database

    Client->>Content: 上传教师头像请求
    Content->>Media: Feign调用临时上传接口
    Media->>Media: 校验文件
    Media->>Redis: 存储临时文件
    Media-->>Content: 返回临时key
    Content-->>Client: 返回临时key
    
    Client->>Content: 确认保存请求
    Content->>Media: Feign调用永久保存接口
    Media->>Redis: 获取临时文件
    Media->>MinIO: 保存到永久存储
    Media->>DB: 保存媒资记录
    Media-->>Content: 返回访问URL
    Content->>DB: 更新教师头像URL
    Content-->>Client: 返回处理结果
```

### 服务调用关系

```mermaid
graph LR
    Content[Content Service]
    Media[Media Service]
    MinIO[MinIO Storage]
    Redis[Redis]
    DB[(Database)]

    Content -->|Feign| Media
    Media -->|Store| MinIO
    Media -->|Temp Storage| Redis
    Media -->|Save Metadata| DB
    Content -->|Update Course| DB
```

## 6. 测试覆盖

### 6.1 单元测试
- CourseBaseServiceTests
    - 课程CRUD完整流程测试
    - 分类树查询测试
    - 课程预览测试
- TeachplanServiceTests
    - 课程计划CRUD测试
    - 树形结构测试
- CourseTeacherServiceTests
    - 教师管理完整流程测试

### 6.2 HTTP接口测试
- course_test.http
- teachplan_test.http
- teacher_test.http

## 7. 开发规范

### 7.1 代码规范
- 使用构造器注入依赖
- Service层添加@Transactional注解
- Controller层使用统一的参数校验
- 使用ModelMapper进行对象转换
- 统一的日志记录方式

### 7.2 异常处理
- 业务异常统一抛出RuntimeException
- 后续需要完善异常处理机制

### 7.3 日志规范
- 使用@Slf4j注解
- 关键业务操作添加INFO级别日志
- 异常情况记录ERROR级别日志

### 7.4 异常处理规范
- 使用全局异常处理器`GlobalExceptionHandler`
- 业务异常使用`ContentException`统一抛出
- 异常类型包括：
  - 参数校验异常（400）
  - 数据完整性异常（400） 
  - 业务逻辑异常（20001系列）
  - 系统内部异常（500）

## 8. 业务流程说明

### 8.1 课程生命周期管理
```mermaid
graph TD
    A[创建课程] --> B[完善基本信息]
    B --> C[添加课程计划]
    B --> D[关联教师]
    C --> E[关联媒资]
    B --> F[设置营销信息]
    F --> G[提交审核]
    G --> H{审核结果}
    H -->|通过| I[发布课程]
    H -->|不通过| B
    I --> J[课程上线]
    J --> K[课程下线]
```

1. 课程创建阶段
   - 基本信息录入（名称、简介、分类等）
   - 营销信息设置（收费规则、价格等）
   - 封面图片上传（与媒体服务交互）
   - 数据合法性校验（@Valid注解校验）

2. 课程内容管理
   - 课程计划编排（两级章节结构）
   - 教师关联（支持多个教师）
   - 媒资绑定（图片、视频等）
   - 内容预览（课程预览功能）

3. 课程审核流程
   ```mermaid
   stateDiagram-v2
       [*] --> 未提交: 创建课程
       未提交 --> 已提交: 提交审核
       已提交 --> 审核通过: 审核通过
       已提交 --> 审核不通过: 审核拒绝
       审核不通过 --> 已提交: 修改重提
       审核通过 --> 已发布: 发布课程
       已发布 --> 已下线: 下架课程
   ```

4. 课程发布管理
   - 审核通过后可发布
   - 支持课程上下线
   - 发布信息同步机制
   - 基于版本的内容控制

### 8.2 数据一致性保证

1. 跨服务调用处理
```mermaid
sequenceDiagram
    participant Content as Content Service
    participant Media as Media Service
    participant MinIO as MinIO Storage
    participant DB as Database

    Content->>Media: 1. 上传文件请求
    Media->>Media: 2. 校验文件
    Media->>MinIO: 3. 存储文件
    Media->>DB: 4. 保存媒资记录
    Media-->>Content: 5. 返回媒资信息
    Content->>DB: 6. 更新关联关系
```

2. 事务管理策略
   - 本地事务：@Transactional
   - 跨服务一致性：最终一致性
   - 补偿机制：失败重试
   - 回滚策略：部分回滚支持

3. 并发控制机制
   - 乐观锁：版本号控制
   - 状态机制：状态流转控制
   - 并发冲突：冲突检测和处理
   - 分布式锁：关键操作保护

### 8.3 业务校验规则

1. 课程创建校验
   - 必填字段：名称、简介、分类等
   - 业务规则：价格范围、图片格式等
   - 数据格式：时间、金额等
   - 权限校验：机构和用户权限

2. 课程发布条件
   - 基本信息完整性
   - 至少一个课程计划
   - 至少一名教师关联
   - 收费课程必须设置价格

### 8.4 异常处理机制

1. 业务异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ContentException.class)
    public CommonResponse<?> handleContentException(ContentException e) {
        return CommonResponse.error(e.getCode(), e.getMessage());
    }
}
```

2. 服务降级策略
```java
@Component
public class MediaFeignClientFallback implements MediaFeignClient {
    @Override
    public CommonResponse<?> uploadCourseLogo(...) {
        return CommonResponse.error("500", "媒体服务不可用");
    }
}
```

### 8.5 关键监控指标

1. 业务指标监控
   - 课程创建成功率
   - 审核通过率
   - 发布成功率
   - 媒资处理成功率

2. 性能指标监控
   - 接口响应时间
   - 服务调用成功率
   - 资源使用情况
   - 并发处理能力

3. 告警阈值设置
   - 错误率超过5%
   - 响应时间超过1s
   - 服务不可用
   - 存储空间不足

## 9. 微服务架构规划

### 9.1 事件机制
- 课程状态变更事件
  - 发布状态变更
  - 审核状态变更
  - 为消息队列集成做准备
- 操作日志事件
  - 课程操作记录
  - 审核操作记录
- 统计数据事件
  - 访问量统计
  - 学习人数统计

### 9.2 分布式功能
- 分布式锁
  - 课程审核锁
  - 课程发布锁
  - 防止并发操作冲突
- 接口幂等性
  - 重复提交保护
  - 操作日志记录
  - 分布式事务支持
- 媒资服务集成
  - 课程封面上传
  - 封面文件管理
  - 跨服务文件删除

### 9.3 微服务集成
- 媒资服务集成
  - MinIO存储集成
  - 媒资上传接口
  - 媒资处理回调
- 搜索服务集成
  - 课程索引创建
  - 搜索条件优化
  - 数据同步机制
- 任务调度集成
  - 定时统计任务
  - 数据同步任务
  - 缓存更新任务

### 9.4 性能优化规划
- 缓存策略
  - 热门课程缓存
  - 分类数据缓存
  - 统计数据缓存
- 数据库优化
  - 读写分离
  - 分库分表预案
  - 索引优化
- 接口优化
  - 批量操作接口
  - 异步处理机制
  - 限流降级策略

### 9.5 监控告警
- 业务监控
  - 课程发布监控
  - 审核流程监控
  - 关键指标监控
- 性能监控
  - 接口响应时间
  - 资源使用情况
  - 并发处理能力
- 告警机制
  - 错误率告警
  - 响应时间告警
  - 容量预警

## 10. API使用指南

### 10.1 课程管理接口
#### 10.1.1 课程列表查询
GET /course/list
**请求参数：**
- pageNo: 页码（从1开始）
- pageSize: 每页大小
- courseName: 课程名称（可选）
- status: 课程状态（可选）
- mt: 课程大分类（可选）
- st: 课程小分类（可选）

**响应示例：**
json
{
"code": 0,
"message": "success",
"data": {
"items": [
{
"id": 1,
"name": "测试课程",
"brief": "课程简介",
"mtName": "后端开发",
"stName": "Java开发",
"charge": "201001",
"price": 0.00
}
],
"counts": 100,
"page": 1,
"pageSize": 10
}
}
#### 10.1.2 创建课程
http
POST /course

**请求体：**
json
{
"name": "课程名称",
"brief": "课程简介",
"mt": 1,
"st": 2,
"charge": "201001",
"price": 0,
"valid": true
}

**响应示例：**
json
{
"code": 0,
"message": "success",
"data": 1 // 返回课程ID
}

### 10.2 课程计划接口
#### 10.2.1 查询课程计划树
http
GET /teachplan/tree/{courseId}

**响应示例：**
json
{
"code": 0,
"message": "success",
"data": [
{
"id": 1,
"name": "第一章",
"level": 1,
"orderBy": 1,
"teachPlanTreeNodes": [
{
"id": 2,
"name": "第一节",
"level": 2,
"orderBy": 1
}
]
}
]
}
#### 10.2.2 章节移动
http
POST /teachplan/{teachplanId}/moveup // 向上移动
POST /teachplan/{teachplanId}/movedown // 向下移动

**响应示例：**
son
{
"code": 0,
"message": "success",
"data": null
}

### 10.3 课程教师接口
#### 10.3.1 教师列表查询
http
GET /course-teacher/list/{courseId}

**响应示例：**
json
{
"code": 0,
"message": "success",
"data": [
{
"id": 1,
"name": "张老师",
"position": "高级讲师",
"description": "教师简介"
}
]
}
#### 10.3.2 添加/修改教师
**请求体：**
http
POST /course-teacher
json
{
"courseId": 1,
"name": "张老师",
"position": "高级讲师",
"description": "教师简介"
}

### 10.4 课程审核接口
#### 10.4.1 提交审核
http
POST /course/{courseId}/audit/submit
#### 10.4.2 审核课程
http
POST /course/audit
**请求体：**
json
{
"courseId": 1,
"auditStatus": "202303",
"auditMind": "审核通过"
}

### 10.5 媒资关联接口
#### 10.5.1 绑定媒资
http
POST /teachplan-media

**请求体：**
json
{
"teachplanId": 1,
"mediaId": 1,
"mediaFileName": "示例视频.mp4"
}
#### 10.5.2 解除绑定
http
DELETE /teachplan-media/{teachplanId}/{mediaId}
### 10.6 错误码说明
| 错误码 | 说明 |
|--------|------|
| 100101 | 课程不存在 |
| 100102 | 课程名称不能为空 |
| 100103 | 课程分类不存在 |
| 100104 | 课程审核状态错误 |
| 100105 | 课程状态错误 |
| 100106 | 课程发布失败 |
| 100201 | 课程计划不存在 |
| 100202 | 课程计划层级错误 |
| 100203 | 课程计划包含子节点，无法删除 |
| 100204 | 课程计划移动失败 |
| 100301 | 教师不存在 |
| 100302 | 教师与课程不匹配 |
| 100401 | 媒资文件不存在 |
| 100402 | 媒资绑定失败 |
| 100403 | 媒资文件不属于该机构 |
| 100404 | 不支持的媒体类型 |
| 100405 | 媒资文件已存在 |
| 100406 | 删除媒资文件失败 |
| 100407 | 媒体服务不可用 |
| 100501 | 上传课程封面失败 |
| 100502 | 删除课程封面失败 |
| 100503 | 课程封面不存在 |
| 199999 | 系统内部错误 |

## 4. 容错机制设计

### 4.1 Resilience4j 集成
内容服务使用 Resilience4j 实现服务容错,主要用于处理与媒体服务的交互。

#### 4.1.1 配置说明
```yaml
resilience4j:
  circuitbreaker:
    instances:
      backendA:  # 断路器实例名称
        slidingWindowType: COUNT_BASED  # 滑动窗口类型:基于计数
        slidingWindowSize: 10  # 滑动窗口大小
        minimumNumberOfCalls: 5  # 最小调用次数
        failureRateThreshold: 50  # 失败率阈值
        waitDurationInOpenState: 10s  # 断路器打开状态持续时间
        permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许的调用次数
```

#### 4.1.2 使用示例
```java
@FeignClient(name = "media-service")
public interface MediaFeignClient {

    @PostMapping("/media/files/course/{courseId}/logo")
    @CircuitBreaker(name = "backendA", fallbackMethod = "uploadCourseLogoFallback")
    CommonResponse<MediaFileDTO> uploadCourseLogo(
            @PathVariable("courseId") Long courseId,
            @RequestParam("organizationId") Long organizationId,
            @RequestPart("file") MultipartFile file);

    /**
     * 上传课程封面的降级方法
     */
    default CommonResponse<MediaFileDTO> uploadCourseLogoFallback(
            Long courseId, Long organizationId, MultipartFile file, Throwable throwable) {
        return CommonResponse.error(
            String.valueOf(ContentErrorCode.UPLOAD_LOGO_FAILED.getCode()),
            ContentErrorCode.UPLOAD_LOGO_FAILED.getMessage()
        );
    }
}
```

### 4.2 容错策略

1. 断路器模式
- 使用滑动窗口统计失败率
- 当失败率超过阈值时断路器打开
- 等待一定时间后进入半开状态
- 在半开状态下允许部分请求通过以探测服务是否恢复

2. 降级处理
- 为关键接口提供 fallback 方法
- 降级时返回预定义的错误码和消息
- 确保系统可以优雅降级

3. 监控指标
- 断路器状态变化
- 请求成功/失败率
- 响应时间统计
- 降级方法调用次数

### 4.3 错误处理
1. 业务异常
- 使用 ContentErrorCode 定义错误码
- 统一异常处理和响应格式
- 详细的错误信息记录

2. 系统监控
- 记录异常日志
- 统计异常发生频率
- 关键指标监控告警

### 8.3 权限控制说明

#### 8.3.1 接口权限分类

1. **管理员权限接口**
- `/course/admin/*` - 管理员专用接口
- 可以查看和操作所有机构的课程信息
- 示例：查询所有课程列表

2. **审核人员权限接口**
- `/course/audit` - 课程审核
- `/course/{courseId}/audit/result` - 审核结果处理
- 可以查看待审核的课程并进行审核操作

3. **机构权限接口**
- `/course/organization/*` - 机构相关操作
- `/content/teacher/*` - 教师管理操作
- 只能操作自己机构的课程和教师
- 示例：查询机构课程列表、管理教师信息

#### 8.3.2 审核流程权限控制
```mermaid
graph TD
    A[机构用户] -->|提交审核| B[待审核]
    B -->|审核人员审核| C{审核结果}
    C -->|通过| D[待发布]
    C -->|不通过| E[审核失败]
    D -->|机构用户发布| F[已发布]
```

1. 提交审核：机构权限
2. 审核操作：审核人员权限
3. 发布操作：机构权限
4. 查看审核进度：机构权限（仅自己机构）
5. 查看所有审核：管理员权限
```

# 内容服务 API 文档

## 1. 课程管理接口
(保持原有课程管理接口文档不变)

## 2. 课程教师管理接口

### 2.1 分页查询机构教师列表

```http
GET /content/teacher/list
```

**请求参数:**
- `organizationId`: 机构ID (必填)
- `pageNo`: 页码，从1开始 (必填)
- `pageSize`: 每页记录数 (必填)

**响应示例:**
```json
{
    "items": [
        {
            "id": 1,
            "name": "张老师",
            "position": "高级讲师",
            "description": "资深Java讲师",
            "avatar": "http://example.com/avatar.jpg",
            "organizationId": 1234,
            "courseIds": [1, 2, 3]
        }
    ],
    "counts": 10,
    "page": 1,
    "pageSize": 10
}
```

### 2.2 查询课程教师列表

```http
GET /content/teacher/course/{courseId}
```

**路径参数:**
- `courseId`: 课程ID

**响应示例:**
```json
[
    {
        "id": 1,
        "name": "张老师",
        "position": "高级讲师",
        "description": "资深Java讲师",
        "avatar": "http://example.com/avatar.jpg",
        "organizationId": 1234,
        "courseIds": [1, 2, 3]
    }
]
```

### 2.3 保存教师信息

```http
POST /content/teacher/save
```

**请求体:**
```json
{
    "id": null,  // 新增时为null，修改时传ID
    "name": "张老师",
    "position": "高级讲师",
    "description": "资深Java讲师",
    "organizationId": 1234
}
```

**响应示例:**
```json
{
    "id": 1  // 返回教师ID
}
```

### 2.4 关联教师到课程

```http
POST /content/teacher/course/{courseId}/associate/{teacherId}
```

**路径参数:**
- `courseId`: 课程ID
- `teacherId`: 教师ID
- `organizationId`: 机构ID (请求参数)

### 2.5 解除教师与课程的关联

```http
POST /content/teacher/course/{courseId}/dissociate/{teacherId}
```

**路径参数:**
- `courseId`: 课程ID
- `teacherId`: 教师ID
- `organizationId`: 机构ID (请求参数)

### 2.6 删除教师

```http
DELETE /content/teacher/{teacherId}
```

**路径参数:**
- `teacherId`: 教师ID
- `organizationId`: 机构ID (请求参数)

### 2.7 上传教师头像

```http
POST /content/teacher/{teacherId}/avatar/upload
```

**路径参数:**
- `teacherId`: 教师ID

**请求体:**
- `file`: 图片文件 (multipart/form-data)

**响应示例:**
```json
{
    "tempKey": "temp_123456"  // 临时存储key
}
```

### 2.8 确认教师头像

```http
POST /content/teacher/{teacherId}/avatar/confirm
```

**路径参数:**
- `teacherId`: 教师ID

**请求体:**
```json
{
    "tempKey": "temp_123456"
}
```

## 3. 系统设计

### 3.1 数据模型

#### CourseBase
(原有的 CourseBase 模型)

#### CourseTeacher
```java
public class CourseTeacher {
    private Long id;
    private String name;
    private String position;
    private String description;
    private String avatar;
    private Long organizationId;
    private Date createTime;
    private Date updateTime;
    private Set<CourseBase> courses;
}
```

### 3.2 错误码说明

课程管理错误码:
(原有的错误码)

教师管理错误码:
- `404001`: 教师不存在
- `403001`: 无权操作其他机构的教师
- `400001`: 教师信息不完整
- `400002`: 头像文件格式不正确
- `500001`: 头像上传失败
- `500002`: 头像确认保存失败

### 3.3 权限控制说明

#### 3.3.1 接口权限分类

1. **管理员权限接口**
(原有的管理员权限说明)

2. **机构权限接口**
- `/course/organization/*` - 机构相关操作
- `/content/teacher/*` - 教师管理操作
- 只能操作自己机构的课程和教师
- 示例：查询机构课程列表、管理教师信息

### 3.4 注意事项

课程管理注意事项:
(原有的注意事项)

教师管理注意事项:
1. 所有涉及教师操作的接口都需要验证操作者所属机构ID与教师所属机构ID是否一致
2. 教师头像上传采用两阶段提交：先上传到临时存储获取tempKey，确认后再保存到永久存储
3. 删除教师时会自动解除与所有课程的关联关系
4. 教师与课程是多对多关系，通过中间表course_teacher_relation维护

### 4. 容错设计

### 4.1 断路器配置
(保持原有容错设计部分不变)

## 3.5 分页查询说明

### 3.5.1 分页参数

所有分页查询接口统一使用以下参数：

```java
public class PageParams {
    private Long pageNo = 1L;     // 页码，从1开始
    private Long pageSize = 10L;  // 每页记录数
}
```

### 3.5.2 分页结果

分页查询统一返回以下格式：

```java
public class PageResult<T> {
    private List<T> items;        // 当前页数据列表
    private long counts;          // 总记录数
    private long page;            // 当前页码
    private long pageSize;        // 每页记录数
}
```

### 3.5.3 分页接口示例

1. 查询机构教师列表
```http
GET /content/teacher/list?pageNo=1&pageSize=10&organizationId=1234
```

2. 查询课程列表
```http
GET /content/course/list?pageNo=1&pageSize=10&organizationId=1234
```

### 3.5.4 分页查询注意事项

1. 参数校验
- pageNo 必须大于等于1
- pageSize 必须大于0且小于等于100
- 超出范围将返回400错误

2. 性能优化
- 添加适当的索引支持分页查询
- 避免使用 count(*) 导致的全表扫描
- 大数据量场景建议使用游标分页

3. 数据一致性
- 分页期间的数据变化可能导致重复或遗漏
- 建议在UI上提示数据可能不是实时的
- 关键业务场景考虑添加时间戳或版本号

4. 空结果处理
- 当没有数据时返回空列表而不是null
- counts为0，items为空数组
- 前端需要正确处理空结果的显示

### 3.6 统一响应格式

所有接口统一使用以下响应格式：

```java
public class ContentResponse<T> {
    private String code;      // 响应码，"0"表示成功
    private String message;   // 响应消息
    private T data;          // 响应数据
}
```

#### 3.6.1 分页查询响应示例

```http
GET /content/course/list?pageNo=1&pageSize=10&organizationId=1234
```

```json
{
    "code": "0",
    "message": "success",
    "data": {
        "items": [
            {
                "id": 1,
                "name": "课程名称",
                "brief": "课程简介"
                // ... 其他课程属性
            }
        ],
        "counts": 100,
        "page": 1,
        "pageSize": 10
    }
}
```

#### 3.6.2 常见响应码说明

基础响应码：
- `0`: 成功
- `4001`: 参数错误
- `4003`: 权限不足
- `4004`: 资源不存在
- `5000`: 系统错误

业务响应码：
- `404001`: 教师不存在
- `403001`: 无权操作其他机构的教师
- `400001`: 教师信息不完整
- `400002`: 头像文件格式不正确
- `500001`: 头像上传失败
- `500002`: 头像确认保存失败

#### 3.6.3 注意事项

1. 所有接口必须使用 ContentResponse 包装返回结果
2. 分页查询接口的数据部分使用 PageResult 封装
3. 错误码应当具有明确的业务含义
4. 响应消息应当清晰描述操作结果或错误原因
5. 敏感信息不应在响应中返回