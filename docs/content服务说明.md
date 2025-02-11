# Content Service 实体关系说明

## 核心实体

### 1. CourseBase (课程基本信息)
- 主要字段:
  - id: 课程ID
  - name: 课程名称
  - brief: 课程简介
  - logo: 课程封面图片URL
  - mt/st: 课程分类(大类/小类)
  - status: 课程业务状态(DRAFT/PUBLISHED/OFFLINE)
  - organizationId: 机构ID
  - qq: 咨询QQ
  - valid: 课程有效性标识

- 关联关系:
  - 一对一 CourseMarket (共享主键)
  - 一对多 Teachplan
  - 多对多 CourseTeacher
  - 一对一 CoursePublish (共享主键)
  - 一对一 CoursePublishPre (共享主键)

### 2. CourseMarket (课程营销信息)
- 主要字段:
  - id: 与课程ID相同
  - charge: 收费规则
  - price: 现价
  - priceOld: 原价
  - discounts: 优惠信息
  
- 关联关系:
  - 一对一 CourseBase (课程基本信息)
  - 使用@MapsId共享主键

### 3. CourseTeacher (课程教师)
- 主要字段:
  - id: 教师ID
  - name: 教师名称
  - position: 职位
  - organizationId: 机构ID
  - description: 教师简介
  - avatar: 教师头像URL
  - createTime: 创建时间
  - updateTime: 更新时间
  
- 关联关系:
  - 多对多 CourseBase (课程基本信息)
  - 使用中间表course_teacher_relation维护关系

- 业务规则:
  1. 教师必须属于某个机构
  2. 教师可以关联多个课程，课程也可以关联多个教师
  3. 教师头像支持两步式上传
  4. 删除教师时会同时删除头像文件
  5. 机构只能操作自己的教师数据
  6. 教师创建和课程关联是分离的操作

### 4. Teachplan (课程计划)
- 主要字段:
  - id: 计划ID
  - name: 计划名称
  - parentId: 父节点ID
  - level: 层级(1:章节,2:小节)
  - orderBy: 排序号
  
- 关联关系:
  - 多对一 CourseBase (课程基本信息)
  - 一对多 TeachplanMedia (课程计划媒资)
  - 树形结构(通过parentId)

### 5. TeachplanMedia (课程计划媒资)
- 主要字段:
  - id: 主键
  - teachplanId: 课程计划ID
  - mediaId: 媒资文件ID
  
- 关联关系:
  - 多对一 Teachplan (课程计划)
  - 多对一 MediaFile (媒资文件)

### 6. CoursePublish (课程发布)
- 主要字段:
  - id: 与课程ID相同
  - name: 课程名称
  - status: 发布状态(PUBLISHED/OFFLINE)
  - publishTime: 发布时间

- 关联关系:
  - 一对一 CourseBase (共享主键)

### 7. CoursePublishPre (课程预发布)
- 主要字段:
  - id: 与课程ID相同
  - name: 课程名称
  - status: 审核状态(SUBMITTED/APPROVED/REJECTED)
  - auditMessage: 审核意见
  - previewTime: 预览时间

- 关联关系:
  - 一对一 CourseBase (共享主键)

### 8. MediaFile (媒资文件)
- 主要字段:
  - mediaFileId: 主键，与media服务的fileId对应
  - organizationId: 机构ID
  - fileName: 文件名称
  - mediaType: 媒体类型(IMAGE/VIDEO)
  - auditStatus: 审核状态
  - url: 访问地址
  - purpose: 文件用途(COVER/VIDEO)
  
- 关联关系:
  - 一对多 TeachplanMedia (课程计划媒资)
  - 与media服务通过mediaFileId关联

## 实体关系图
```mermaid
erDiagram
    CourseBase ||--|| CourseMarket : has
    CourseBase ||--|| CoursePublish : has
    CourseBase ||--|| CoursePublishPre : has
    CourseBase ||--o{ Teachplan : contains
    CourseBase }|--|| CourseTeacher : taught_by
    Teachplan ||--o{ TeachplanMedia : has
    TeachplanMedia }|--|| MediaFile : uses
    MediaFile }|--|| ExternalMediaFile : references
```

## 关键设计说明

1. 主键策略
- CourseBase: 自增主键
- CourseMarket/CoursePublish/CoursePublishPre: 与CourseBase共享主键
- 其他实体: 自增主键

2. 时间字段规范
- 统一使用 LocalDateTime 类型
- 字段命名: create_time, update_time, publish_time 等
- 使用 @Column(name = "xxx_time") 指定列名
- 实体类中添加 @PrePersist 和 @PreUpdate 自动管理
- 配置文件中设置 JPA 时区为 UTC
- 示例:
  ```java
  @Column(name = "create_time")
  private LocalDateTime createTime;

  @PrePersist
  public void prePersist() {
      if (createTime == null) {
          createTime = LocalDateTime.now();
      }
  }
  ```

3. 懒加载配置
- 所有多对一、一对多关系默认使用懒加载
- 使用@ToString.Exclude和@EqualsAndHashCode.Exclude避免循环引用

4. 级联操作
- CourseBase -> CourseMarket: 级联所有操作
- CourseBase -> Teachplan: 级联所有操作
- Teachplan -> TeachplanMedia: 级联所有操作

5. 数据完整性
- 必填字段使用@Column(nullable = false)
- 使用外键约束确保关联数据完整性
- 使用@JoinColumn指定外键列

6. 树形结构
- Teachplan通过parentId实现树形结构
- 支持两级结构:章节和小节

7. 枚举使用规范
- 所有状态字段必须使用枚举类型
- 持久化时存储枚举的code值（字符串类型）
- 示例：
  ```java
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CourseStatusEnum status;
  ```
- 前端交互使用code值，后端转换枚举处理

## Service层说明

### 1. CourseBaseService (课程基础服务)
- 主要功能：
  - 课程的CRUD操作
  - 课程状态管理
  - 课程审核流程管理
  - 课程发布管理

- 关键方法：
  ```java
  // 创建课程(初始状态为草稿)
  Long createCourse(AddCourseDTO addCourseDTO);
  
  // 提交审核(创建/更新预发布记录)
  void submitForAudit(Long courseId);
  
  // 审核课程(更新预发布记录状态)
  void auditCourse(CourseAuditDTO auditDTO);
  
  // 发布课程(创建发布记录)
  void publishCourse(Long courseId);
  
  // 下线课程
  void offlineCourse(Long courseId);
  ```

### 2. TeachplanService (课程计划服务)
- 主要功能：
  - 课程计划的CRUD操作
  - 课程计划树形结构管理
  - 课程计划排序管理

- 关键方法：
  ```java
  // 查询课程计划树
  List<TeachplanDTO> findTeachplanTree(Long courseId);
  
  // 保存课程计划
  void saveTeachplan(SaveTeachplanDTO teachplanDTO);
  
  // 课程计划排序
  void moveUp(Long teachplanId);
  void moveDown(Long teachplanId);
  ```

### 3. CourseTeacherService (课程教师服务)
- 主要功能：
  - 教师信息管理
  - 教师与课程关联管理
  - 按机构查询教师

- 关键方法：
  ```java
  // 查询课程教师
  List<CourseTeacherDTO> listByCourseId(Long courseId);
  
  // 查询机构教师
  List<CourseTeacherDTO> listByOrganizationId(Long organizationId);
  
  // 教师课程管理
  void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO);
  void deleteCourseTeacher(Long courseId, Long teacherId);
  ```

### 4. TeachplanMediaService (课程计划媒资服务)
- 主要功能：
  - 媒资绑定管理
  - 媒资关联查询

- 关键方法：
  ```java
  // 媒资绑定
  void associateMedia(TeachplanMediaDTO teachplanMediaDTO);
  
  // 解除绑定
  void dissociateMedia(Long teachplanId, Long mediaId);
  ```

### 5. MediaFileService (媒资文件服务)
- 主要功能：
  - 媒资文件信息管理
  - 媒资文件审核状态管理
  - 媒资文件访问控制

- 关键方法：
  ```java
  // 保存媒资文件信息
  MediaFile saveMediaFile(Long organizationId, MediaFileDTO mediaFileDTO);
  
  // 分页查询媒资文件
  PageResult<MediaFileDTO> queryMediaFiles(Long organizationId, String mediaType, String purpose, PageParams pageParams);
  
  // 获取媒资文件访问地址
  String getMediaFileUrl(Long organizationId, String mediaFileId);
  
  // 更新审核状态
  void updateAuditStatus(String mediaFileId, String auditStatus, String auditMessage);
  ```

## DTO说明

### 1. 课程相关DTO
- AddCourseDTO：课程创建数据传输对象
  ```java
  private String name;        // 课程名称
  private String brief;       // 课程简介
  private Long mt;           // 课程大分类
  private Long st;           // 课程小分类
  private String charge;     // 收费规则
  private BigDecimal price;  // 课程价格
  ```

- CourseBaseDTO：课程基本信息DTO
  ```java
  private Long id;           // 课程ID
  private String name;       // 课程名称
  private String status;     // 课程状态
  private String mtName;     // 大分类名称
  private String stName;     // 小分类名称
  ```

- CoursePreviewDTO：课程预览DTO
  ```java
  private CourseBaseDTO courseBase;           // 基本信息
  private List<TeachplanDTO> teachplans;      // 课程计划
  private List<CourseTeacherDTO> teachers;    // 课程教师
  ```

### 2. 教师相关DTO
- CourseTeacherDTO
```java
public class CourseTeacherDTO {
    private Long id;              // 教师ID
    private String name;          // 教师名称
    private String position;      // 职位
    private String description;   // 简介
    private String avatar;        // 头像URL
    private Set<Long> courseIds;  // 关联课程ID
}
```

- SaveCourseTeacherDTO：保存教师DTO
  ```java
  private Long organizationId;  // 机构ID
  private String name;          // 教师名称
  private Set<Long> courseIds;  // 关联课程ID
  ```

### 3. 课程计划相关DTO
- TeachplanDTO：课程计划DTO
  ```java
  private Long id;                          // 计划ID
  private String name;                      // 计划名称
  private Integer level;                    // 层级
  private List<TeachplanDTO> teachPlanTreeNodes;  // 子节点
  ```

- SaveTeachplanDTO：保存课程计划DTO
  ```java
  private Long courseId;    // 课程ID
  private String name;      // 计划名称
  private Long parentId;    // 父节点ID
  private Integer level;    // 层级
  ```

### 4. 查询相关DTO
- QueryCourseParamsDTO：课程查询参数DTO
  ```java
  private String courseName;     // 课程名称
  private String status;         // 课程状态
  private Long organizationId;   // 机构ID
  ```

- CourseCategoryTreeDTO：课程分类树DTO
  ```java
  private Long id;              // 分类ID
  private String name;          // 分类名称
  private List<CourseCategoryTreeDTO> childrenTreeNodes;  // 子节点
  ```

### 5. 审核相关DTO
- CourseAuditDTO：课程审核DTO
  ```java
  private Long courseId;     // 课程ID
  private String auditStatus; // 审核状态
  private String auditMessage;// 审核意见
  ```

## 业务流程说明

### 1. 课程发布流程
1. 创建课程基本信息
2. 添加课程营销信息
3. 设置课程计划
4. 关联课程教师
5. 提交课程审核
6. 审核通过后发布

### 2. 课程计划管理
1. 创建课程章节
2. 在章节下创建小节
3. 调整章节和小节顺序
4. 关联媒资文件

### 3. 教师管理
1. 创建教师信息
2. 关联教师与课程
3. 管理教师课程关系

### 4. 媒资文件管理流程
1. 文件上传流程
   - 用户通过media服务上传文件
   - media服务处理并返回mediaFileId
   - content服务保存媒资信息
   - 根据mediaType设置默认审核状态

2. 文件访问流程
   - 图片类型：直接返回url
   - 视频类型：需要通过media服务获取临时访问地址

3. 审核流程
   - 图片默认通过审核（状态直接设为3-审核通过）
   - 视频需要经过审核（初始状态为1-未审核）
   - 审核状态转换：
     - 提交审核 → 2（审核中）
     - 审核通过 → 3（审核通过）
     - 审核不通过 → 4（审核不通过）
   - 状态变更通过消息队列接收审核结果

## Controller层说明

### 1. CourseController (课程管理接口)
- 基础路径: `/course`
- 主要接口:
  ```
  GET  /list                    # 分页查询课程列表
  POST /                        # 创建课程
  PUT  /                        # 修改课程
  GET  /{courseId}             # 获取课程详情
  GET  /category/tree          # 获取课程分类树
  GET  /preview/{courseId}     # 课程预览
  POST /{courseId}/audit/submit # 提交课程审核
  POST /audit                  # 审核课程
  POST /{courseId}/publish     # 发布课程
  POST /{courseId}/offline     # 下架课程
  ```

- 关键接口说明:
  1. 分页查询课程列表
     - 请求参数：
       ```json
       {
         "pageNo": 1,
         "pageSize": 10,
         "courseName": "课程名称",
         "status": "课程状态",
         "organizationId": 1234
       }
       ```
     - 返回结果：
       ```json
       {
         "items": [{
           "id": 1,
           "name": "课程名称",
           "brief": "课程简介",
           "status": "202001"
         }],
         "counts": 100,
         "page": 1,
         "pageSize": 10
       }
       ```

  2. 创建课程
     - 请求体：
       ```json
       {
         "name": "课程名称",
         "brief": "课程简介",
         "mt": 1,
         "st": 2,
         "charge": "201001",
         "price": 0,
         "organizationId": 1234
       }
       ```
     - 返回结果：课程ID

### 2. TeachplanController (课程计划管理接口)
- 基础路径: `/teachplan`
- 主要接口:
  ```
  GET    /tree/{courseId}      # 查询课程计划树
  POST   /                     # 创建/修改课程计划
  DELETE /{teachplanId}        # 删除课程计划
  POST   /moveup/{teachplanId} # 上移课程计划
  POST   /movedown/{teachplanId} # 下移课程计划
  POST   /media               # 绑定媒资
  DELETE /media/{teachplanId}/{mediaId} # 解除媒资绑定
  ```

- 关键接口说明:
  1. 查询课程计划树
     - 返回结果：
       ```json
       [{
         "id": 1,
         "name": "第一章",
         "level": 1,
         "teachPlanTreeNodes": [{
           "id": 2,
           "name": "第一节",
           "level": 2
         }]
       }]
       ```

  2. 创建课程计划
     - 请求体：
       ```json
       {
         "courseId": 1,
         "parentId": 0,
         "name": "第一章",
         "level": 1,
         "orderBy": 1
       }
       ```

### 3. CourseTeacherController (课程教师管理接口)
- 基础路径: `/course-teacher`
- 主要接口:
  ```
  GET    /list/{courseId}              # 查询课程教师列表
  GET    /{organizationId}/{teacherId} # 查询教师详情
  GET    /courses/{teacherId}          # 查询教师关联的课程
  GET    /organization/{organizationId} # 查询机构教师列表
  POST   /                             # 添加/修改教师
  DELETE /{courseId}/{teacherId}       # 解除教师与课程关联
  ```

- 关键接口说明:
  1. 添加/修改教师
     - 请求体：
       ```json
       {
         "organizationId": 1234,
         "name": "教师名称",
         "position": "讲师",
         "courseIds": [1, 2, 3]
       }
       ```

  2. 查询教师详情
     - 返回结果：
       ```json
       {
         "id": 1,
         "name": "教师名称",
         "position": "讲师",
         "description": "教师简介",
         "courseIds": [1, 2, 3]
       }
       ```

### 4. TeachplanMediaController (课程计划媒资管理接口)
- 基础路径: `/teachplan-media`
- 主要接口:
  ```
  POST   /                             # 绑定媒资
  DELETE /{teachplanId}/{mediaId}      # 解除媒资绑定
  GET    /{teachplanId}                # 获取媒资列表
  ```

- 关键接口说明:
  1. 绑定媒资
     - 请求体：
       ```json
       {
         "teachplanId": 1,
         "mediaId": 1,
         "mediaFileName": "视频.mp4"
       }
       ```

### 5. MediaFileController (媒资文件管理接口)
- 基础路径: `/media-files`
- 主要接口:
  ```
  POST   /                     # 保存媒资文件信息
  GET    /list/{organizationId} # 查询媒资文件列表
  GET    /url/{organizationId}/{mediaFileId} # 获取媒资文件访问地址
  ```

- 关键接口说明:
  1. 保存媒资文件信息
     - 请求体：
       ```json
       {
         "mediaFileId": "xxx",
         "fileName": "test.mp4",
         "mediaType": "VIDEO",
         "purpose": "VIDEO",
         "url": "http://xxx",
         "organizationId": 1234
       }
       ```

  2. 查询媒资文件列表
     - 请求参数：
       - organizationId: 机构ID
       - mediaType: 媒体类型(可选)
       - purpose: 用途(可选)
       - pageNo: 页码
       - pageSize: 每页大小

## API响应格式
所有接口统一使用ContentResponse包装响应结果：
```json
{
  "code": 0,          // 响应码
  "msg": "success",   // 响应消息
  "data": {           // 响应数据
    // 具体业务数据
  }
}
```

## 错误码说明

### 通用错误码 (1xxxx)
- 10000: 系统内部错误
- 10001: 参数验证失败
- 10002: 资源不存在
- 10003: 无操作权限

### 课程相关错误码 (2xxxx)
- 20001: 课程不存在
- 20002: 课程已删除
- 20003: 课程状态不允许当前操作
- 20004: 课程计划不完整
- 20005: 未设置课程教师
- 20006: 课程价格信息不完整
- 20007: 课程审核信息不存在

### 教师相关错误码 (3xxxx)
- 30001: 教师不存在
- 30002: 教师已关联其他机构
- 30003: 教师与课程关联关系不存在

### 媒资相关错误码 (4xxxx)
- 40001: 媒资文件不存在
- 40002: 媒资文件未审核
- 40003: 媒资文件审核未通过
- 40004: 媒资绑定关系已存在

## 错误响应格式
```json
{
    "code": 0,          // 响应码
    "msg": "success",   // 响应消息
    "data": {           // 响应数据
        // 具体业务数据
    }
}
```

## 接口认证与授权
- 所有接口需要在请求头中携带token
- 使用Spring Security进行认证和授权
- 机构ID从token中获取，无需在请求中传递

## 状态码说明

### 课程状态码
- 202301: 已提交审核
- 202302: 审核不通过
- 202303: 审核通过
- 202304: 已发布
- 202305: 已下线

## 业务流程约束

### 课程审核提交条件
1. 课程基本信息完整
2. 必须添加至少一个课程计划（包含章节和小节）
3. 必须关联至少一名教师
4. 如果是收费课程，必须设置课程价格

### 课程发布流程
1. 创建课程：初始状态为未提交
2. 完善课程信息：
   - 添加课程计划
   - 关联课程教师
   - 设置营销信息
3. 提交审核：
   - 系统检查课程完整性
   - 状态变更为"已提交审核"(202301)
4. 审核处理：
   - 审核通过：状态变更为"审核通过"(202303)，生成预发布信息
   - 审核不通过：状态变更为"审核不通过"(202302)，可修改后重新提交
5. 课程发布：
   - 将预发布信息同步到发布信息
   - 状态变更为"已发布"(202304)
6. 课程下线：
   - 状态变更为"已下线"(202305)
   - 课程仍可被管理员查看，但用户端不可见

## 接口说明

### 课程管理接口

#### 1. 创建课程
- 请求方法：POST
- 请求路径：/content/course
- 请求体：
```json
{
    "name": "课程名称",
    "brief": "课程简介",
    "mt": 1,
    "st": 2,
    "charge": "201001",
    "price": 99.00,
    "organizationId": 1234
}
```
- 响应体：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "courseId": 1
    }
}
```

#### 2. 提交课程审核
- 请求方法：POST
- 请求路径：/content/course/{courseId}/audit/submit
- 响应说明：
  - 课程必须处于草稿状态
  - 课程信息必须完整(包括课程计划和教师)
  - 成功后创建预发布记录,状态为SUBMITTED

#### 3. 审核课程
- 请求方法：POST
- 请求路径：/content/course/audit
- 请求体：
```json
{
    "courseId": 1,
    "auditStatus": "pass/reject",
    "auditMessage": "审核意见"
}
```
- 响应说明：
  - 课程必须处于已提交审核状态
  - 审核不通过必须填写审核意见
  - 更新预发布记录状态为APPROVED/REJECTED

#### 4. 发布课程
- 请求方法：POST
- 请求路径：/content/course/{courseId}/publish
- 响应说明：
  - 课程必须审核通过
  - 更新课程状态为PUBLISHED
  - 创建发布记录

#### 5. 下线课程
- 请求方法：POST
- 请求路径：/content/course/{courseId}/offline
- 响应说明：
  - 课程必须处于已发布状态
  - 更新课程状态为OFFLINE
  - 更新发布记录状态

### 课程计划接口

#### 1. 获取课程计划树
- 请求方法：GET
- 请求路径：/content/teachplan/{courseId}/tree
- 响应体：
```json
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
                    "orderBy": 1,
                    "mediaInfo": null
                }
            ]
        }
    ]
}
```

#### 2. 保存课程计划
- 请求方法：POST
- 请求路径：/content/teachplan
- 请求体：
```json
{
    "courseId": 1,
    "parentId": 0,
    "name": "第一章",
    "level": 1,
    "orderBy": 1
}
```
- 响应体：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "teachplanId": 1
    }
}
```

### 课程教师接口

#### 1. 关联教师
- 请求方法：POST
- 请求路径：/content/course-teacher
- 请求体：
```json
{
    "name": "教师名称",
    "position": "讲师",
    "description": "教师简介",
    "courseIds": [1, 2]
}
```
- 响应体：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "teacherId": 1
    }
}
```

### 媒资管理接口

#### 1. 绑定媒资
- 请求方法：POST
- 请求路径：/content/teachplan-media
- 请求体：
```json
{
    "teachplanId": 1,
    "mediaId": "mediaFileId",
    "mediaType": "video"
}
```
- 响应体：
```json
{
    "code": 0,
    "message": "success"
}
```

### 机构课程管理
1. 查询机构课程列表
```http
GET /course/organization/{organizationId}
```
请求参数：
- organizationId: 机构ID（路径参数）
- status: 课程状态（可选）
- auditStatus: 审核状态（可选）
- pageNo: 页码
- pageSize: 每页大小

响应示例：
```json
{
    "code": "0",
    "message": "success",
    "data": {
        "items": [
            {
                "id": 1,
                "name": "测试课程",
                "brief": "课程简介",
                "status": "202001",
                "auditStatus": "202302",
                "organizationId": 1234
            }
        ],
        "total": 100,
        "pageNo": 1,
        "pageSize": 10
    }
}
```

2. 重新提交审核
```http
POST /course/{courseId}/resubmit
```
请求参数：
- courseId: 课程ID（路径参数）

响应示例：
```json
{
    "code": "0",
    "message": "success"
}
```

注意：
1. 所有机构相关的接口后续会加入认证和鉴权机制
2. 机构只能操作自己的课程
3. 重新提交审核仅支持审核不通过的课程

## 10. 设计决策说明

### 10.1 机构ID设计方案

#### 当前设计
- 在实体中使用 organizationId 字段标识所属机构
- 不引入 Organization 实体类
- 通过 DTO 传递机构基础信息

#### 设计理由
1. **单一职责原则**
- Content Service 专注于内容管理
- 机构管理职责由专门的 Organization Service 负责
- 避免跨服务职责重叠

2. **服务边界清晰**
- 明确的服务职责划分
- 降低服务间耦合
- 便于独立演进和维护

3. **数据一致性考虑**
- 避免跨服务数据同步问题
- 减少分布式事务场景
- 简化数据管理复杂度

4. **扩展性考虑**
- 为后续服务拆分预留空间
- 便于引入新的机构相关特性
- 支持不同的机构管理策略

#### 实现示例
```java
// 实体类中的机构ID字段
@Column(name = "organization_id", nullable = false)
private Long organizationId;

// DTO中的机构信息
public class CourseBaseDTO {
    private Long id;
    private String name;
    // ... 其他课程字段 ...
    private Long organizationId;
    private String organizationName;  // 冗余显示字段
}
```

#### 后续优化方向
1. **短期**
- 完善机构ID校验机制
- 添加机构级别的数据访问控制
- 优化机构相关查询性能

2. **中期**
- 实现机构信息缓存机制
- 添加机构数据统计功能
- 完善机构权限控制

3. **长期**
- 对接完整的机构服务
- 实现更细粒度的权限控制
- 支持多级机构架构

### 10.2 权限控制设计

#### 10.2.1 教师管理权限
1. 机构用户权限
   - 只能查看和管理自己机构的教师
   - 可以为自己机构的课程关联/解除教师
   - 可以管理教师的基本信息和头像

2. 管理员权限
   - 可以查看所有机构的教师信息
   - 可以进行跨机构的教师管理（预留）

3. 权限验证说明
   - 当前通过API路径参数传入机构ID进行验证
   - 后续将通过Token获取机构ID，实现更严格的权限控制

### 10.3 DTO设计优化

#### 10.3.1 基础DTO抽取
1. CourseBaseInfoDTO - 课程基础信息的公共字段
```java
@Data
@Schema(description = "课程基础信息DTO")
public class CourseBaseInfoDTO {
    @Schema(description = "课程ID")
    private Long id;
    
    @Schema(description = "课程名称")
    @NotEmpty(message = "课程名称不能为空") 
    private String name;
    
    // ... 其他基础字段
}
```

2. TreeNodeDTO - 树形结构的公共字段
```java
@Data
public class TreeNodeDTO<T> {
    @Schema(description = "节点ID")
    private Long id;
    
    @Schema(description = "节点名称")
    private String name;
    
    @Schema(description = "父节点ID")
    private Long parentId;
    
    @Schema(description = "子节点列表")
    private List<T> children;
}
```

#### 10.3.2 命名规范统一
1. 树形结构子节点统一命名为 children
2. 校验注解统一使用 @NotEmpty/@NotNull
3. Swagger注解统一使用 @Schema

#### 10.3.3 优化效果
1. 提高代码复用性
2. 减少重复代码
3. 统一命名规范
4. 简化维护工作
5. 提升代码质量

### 10.4 课程计划排序优化

#### 10.4.1 设计思路
1. 两阶段提交
   - 移动操作(moveUp/moveDown)只更新内存缓存
   - 用户确认后才批量更新数据库
   - 支持撤销未保存的变更

2. 缓存设计
```java
@Component
public class TeachplanOrderCache {
    private final Map<Long, Integer> orderCache = new ConcurrentHashMap<>();
    
    // 缓存排序变更
    public void cacheOrderChange(Long id1, Integer order1, Long id2, Integer order2);
    
    // 获取当前排序(优先从缓存获取)
    public Integer getCurrentOrder(Long id, Integer defaultOrder);
    
    // 批量保存变更
    public void saveAllChanges();
    
    // 丢弃未保存的变更
    public void discardChanges();
}
```

3. 接口设计
```java
@RestController
@RequestMapping("/teachplan")
public class TeachplanController {
    // 临时移动操作
    @PostMapping("/moveup/{teachplanId}")
    public ContentResponse<Void> moveUp(@PathVariable Long teachplanId);
    
    @PostMapping("/movedown/{teachplanId}")
    public ContentResponse<Void> moveDown(@PathVariable Long teachplanId);
    
    // 保存或丢弃变更
    @PostMapping("/saveorder")
    public ContentResponse<Void> saveOrderChanges();
    
    @PostMapping("/discardorder")
    public ContentResponse<Void> discardOrderChanges();
}
```

#### 10.4.2 优化效果
1. 性能提升
   - 减少数据库写操作
   - 支持批量更新
   - 避免频繁事务

2. 用户体验
   - 操作响应更快
   - 支持撤销操作
   - 符合用户习惯

3. 数据一致性
   - 事务原子性
   - 避免部分更新
   - 支持回滚

# Content Service 状态管理说明

## 1. 状态定义

### 1.1 课程业务状态 (CourseStatusEnum)
- DRAFT("202001", "草稿") - 初始状态
- PUBLISHED("202002", "已发布")
- OFFLINE("202003", "已下线")

### 1.2 课程审核状态 (CourseAuditStatusEnum)
- SUBMITTED("202301", "已提交审核")
- APPROVED("202302", "审核通过")
- REJECTED("202303", "审核不通过")

## 2. 状态流转说明

### 2.1 课程业务状态流转
1. 创建课程 -> DRAFT
2. 发布课程 -> PUBLISHED
3. 下线课程 -> OFFLINE

### 2.2 课程审核状态流转
1. 提交审核 -> SUBMITTED
2. 审核通过 -> APPROVED
3. 审核不通过 -> REJECTED

### 2.3 状态流转规则
1. 课程创建后初始状态为草稿(DRAFT)
2. 只有审核通过(APPROVED)的课程才能发布
3. 只有已发布(PUBLISHED)的课程才能下线
4. 已发布的课程不能重复发布
5. 已下线的课程不能重复下线
6. 已发布的课程不能删除

## 3. 实体关系说明

### 3.1 CourseBase (课程基本信息)
- status: 记录课程业务状态
- 一对一关联 CoursePublishPre 和 CoursePublish

### 3.2 CoursePublishPre (课程预发布)
- status: 记录课程审核状态
- 审核通过后可以进行发布操作

### 3.3 CoursePublish (课程发布)
- status: 记录课程发布状态
- 发布后的课程信息快照

## 4. 关键操作说明

### 4.1 提交审核
1. 检查课程当前状态
2. 验证课程信息完整性
3. 创建/更新 CoursePublishPre 记录
4. 设置审核状态为 SUBMITTED

### 4.2 审核操作
1. 检查课程是否处于待审核状态
2. 更新 CoursePublishPre 的审核状态
3. 记录审核意见(如果有)

### 4.3 发布课程
1. 检查课程审核状态是否为 APPROVED
2. 更新课程基本状态为 PUBLISHED
3. 创建/更新 CoursePublish 记录
4. 设置发布时间和状态

### 4.4 下线课程
1. 检查课程是否处于已发布状态
2. 更新课程基本状态为 OFFLINE
3. 更新 CoursePublish 状态为 OFFLINE

## 课程计划排序实现说明

### 1. 缓存设计
- 使用 ConcurrentHashMap 实现线程安全的内存缓存
- key 为课程计划ID，value 为临时排序号
- 支持并发访问和原子操作

### 2. 操作流程
1. 移动操作
   - 检查移动合法性
   - 计算新的排序号
   - 更新缓存
   - 不直接操作数据库

2. 查询操作
   - 优先从缓存获取排序号
   - 缓存未命中则使用数据库值
   - 确保显示最新排序状态

3. 保存操作
   - 获取所有待更新记录
   - 批量更新排序号
   - 清空缓存
   - 事务保证

4. 撤销操作
   - 清空缓存
   - 返回数据库状态
   - 无需数据库操作

### 3. 异常处理
1. 移动异常
   - 首节点上移
   - 末节点下移
   - 节点不存在

2. 保存异常
   - 事务回滚
   - 清空缓存
   - 返回错误信息

### 4. 注意事项
1. 并发控制
   - ConcurrentHashMap 保证缓存线程安全
   - 事务隔离保证数据一致性

2. 性能优化
   - 批量更新减少数据库操作
   - 缓存减少数据库访问
   - 延迟写入提升响应速度

3. 接口设计
   - 明确标注临时操作
   - 完整的操作说明
   - 统一的返回格式
