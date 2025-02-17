# 媒体服务(Media Service)文档

## 1. 功能概述

### 1.1 已实现功能
- 图片管理
  - 图片上传到临时存储(Redis)
  - 图片临时存储更新
  - 图片永久存储(MinIO)
  - 图片格式校验
  - 图片去重(基于MD5)
  - 图片大小控制(1KB~2MB)
- 文件用途
  - 课程封面
  - 教师头像
  - 其他图片资源
- 文件存储
  - MinIO对象存储集成
  - Redis临时存储
  - 文件元信息管理
- 课程封面管理
  - 封面图片上传
  - 封面图片更新
  - 封面图片删除
  - 图片格式校验
  - 文件去重

### 1.2 待实现功能
- 视频处理
  - 视频上传
  - 视频转码
  - 视频分片上传
- 文件处理
  - 文件预处理
  - 病毒扫描
  - 文件分类
- CDN集成
  - CDN推送
  - 缓存刷新
  - 访问控制

### 1.3 文件校验规则
```yaml
media:
  image:
    max-size: 2097152          # 图片最大2MB
    min-size: 1024             # 图片最小1KB
    allowed-types:             # 允许的图片类型
      - image/jpeg
      - image/jpg
      - image/png
      - image/gif
```

### 1.4 业务流程

1. 图片上传流程
```mermaid
sequenceDiagram
    Client->>Service: 上传图片
    Service->>Service: 校验文件(大小/类型)
    Service->>Redis: 临时存储(30分钟)
    Service-->>Client: 返回临时key
    Client->>Service: 确认保存
    Service->>MinIO: 永久存储
    Service->>MySQL: 保存元数据
    Service-->>Client: 返回访问URL
```

2. 图片更新流程
```mermaid
sequenceDiagram
    Client->>Service: 上传新图片
    Service->>Service: 校验文件
    Service->>MinIO: 删除旧文件
    Service->>MinIO: 上传新文件
    Service->>MySQL: 更新元数据
    Service-->>Client: 返回新URL
```

## 2. 技术架构

### 2.1 核心技术栈
- Spring Boot 3.x：基础框架
- Spring Data JPA：数据访问层
- MinIO：对象存储
- Redis：临时存储
- MySQL：元数据存储
- JUnit 5：单元测试
- Validation：参数校验
- Swagger/OpenAPI：接口文档

### 2.2 项目结构
```
media/
├── controller/          # 控制层
│   └── ImageController.java
├── service/            # 业务层
│   ├── ImageService.java
│   └── impl/
├── repository/         # 数据访问层
│   └── MediaFileRepository.java
├── entity/            # 实体类
│   └── MediaFile.java
├── dto/               # 数据传输对象
│   ├── TempFileDTO.java
│   └── UploadFileDTO.java
├── utils/             # 工具类
│   └── FileTypeUtils.java
└── config/            # 配置类
    ├── MinioConfig.java
    └── RedisConfig.java
```

## 3. 数据模型设计

### 3.1 核心实体
#### MediaFile (媒资文件)
- 主要属性：
  - id: 主键
  - fileId: 文件唯一标识
  - fileName: 文件名
  - filePath: 存储路径
  - fileSize: 文件大小
  - fileType: 文件类型
  - fileMd5: 文件MD5值
  - status: 文件状态
  - url: 访问地址

#### TempFileDTO (临时文件)
- 主要属性：
  - fileName: 文件名
  - contentType: 内容类型
  - fileData: 文件数据
  - fileSize: 文件大小

### 3.2 状态定义
- 文件状态：
  - UPLOADING: 上传中
  - UPLOADED: 已上传
  - FAILED: 上传失败
- 审核状态：
  - PENDING: 待审核
  - APPROVED: 已通过
  - REJECTED: 已拒绝

## 4. API接口设计

### 4.1 图片管理接口
#### 4.1.1 上传图片到临时存储
```http
POST /media/images/temp
Content-Type: multipart/form-data

请求参数：
- file: 图片文件

响应：
{
  "code": 0,
  "message": "success",
  "data": "temp-key-123" // 临时存储key
}
```

#### 4.1.2 保存临时图片到永久存储
```http
POST /media/temp/save
Content-Type: application/json

请求体：
{
  "tempKey": "temp-key-123"
}

响应：
{
  "code": 0,
  "message": "success",
  "data": {
    "mediaFileId": "xxx",
    "fileName": "image.jpg",
    "url": "http://minio/bucket/xxx.jpg"
  }
}
```

#### 4.1.3 删除媒体文件
```http
DELETE /media/files/{url}

响应：
{
  "code": 0,
  "message": "success"
}
```

## 5. 业务实现细节

### 5.1 图片处理流程

```mermaid
sequenceDiagram
    participant Client
    participant Service
    participant Redis
    participant MinIO
    participant DB

    Client->>Service: 上传图片
    Service->>Service: 校验文件
    Service->>Redis: 临时存储(30分钟)
    Service-->>Client: 返回临时key
    
    Client->>Service: 确认保存
    Service->>Redis: 获取临时文件
    Service->>MinIO: 永久存储
    Service->>DB: 保存元数据
    Service-->>Client: 返回访问URL
```

1. 文件校验
   ```java
   public class FileTypeUtils {
       private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
           "image/jpeg", "image/jpg", "image/png", "image/gif"
       );
       
       public static boolean isAllowedImage(MultipartFile file) {
           return ALLOWED_IMAGE_TYPES.contains(file.getContentType());
       }
   }
   ```

2. 文件标识生成
   ```java
   private String generateMediaFileId(Long organizationId, Long courseId, String fileName) {
       String simpleFileName = new File(fileName).getName();
       return String.format("course_%d_%d_%s", 
           organizationId, courseId,
           DigestUtils.md5DigestAsHex(simpleFileName.getBytes())
       );
   }
   ```

### 5.2 存储架构设计

```mermaid
graph LR
    Client[客户端]
    Redis[(Redis临时存储)]
    MinIO[(MinIO对象存储)]
    MySQL[(MySQL元数据)]
    
    Client -->|上传| Redis
    Client -->|确认| MinIO
    MinIO -->|存储| MySQL
    Redis -->|30分钟过期| Clean[自动清理]
```

1. 临时存储(Redis)
   - Key格式: media:temp:image:{uuid}
   - 有效期: 30分钟
   - 数据结构: TempFileDTO序列化

2. 永久存储(MinIO)
   - 存储路径: course/logo/{mediaFileId}
   - 文件去重: 基于mediaFileId
   - 访问URL: /{bucketName}/{filePath}

3. 元数据管理(MySQL)
   - 实体类: MediaFile
   - 索引: mediaFileId(唯一索引)
   - 关联关系: 与课程计划多对多

### 5.3 异常处理机制

```mermaid
graph TD
    A[异常发生] --> B{异常类型}
    B -->|业务异常| C[MediaException]
    B -->|系统异常| D[全局处理]
    C --> E[返回错误码]
    D --> F[记录日志]
    F --> E
```

1. 业务异常
   ```java
   public class MediaException extends RuntimeException {
       private final MediaErrorCode errorCode;
       private final String message;
   }
   ```

2. 全局处理
   ```java
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(MediaException.class)
       public MediaResponse<Void> handleMediaException(MediaException e) {
           return MediaResponse.error(e.getCode(), e.getMessage());
       }
   }
   ```

### 5.4 文件处理流程

1. 上传流程
   ```mermaid
   sequenceDiagram
       participant Client
       participant Service
       participant MinIO
       participant DB
       
       Client->>Service: 上传文件
       Service->>Service: 校验文件
       Service->>MinIO: 存储文件
       MinIO-->>Service: 返回结果
       Service->>DB: 保存元数据
       Service-->>Client: 返回URL
   ```

2. 删除流程
   ```mermaid
   sequenceDiagram
       participant Client
       participant Service
       participant MinIO
       participant DB
       
       Client->>Service: 删除请求
       Service->>DB: 查询文件
       Service->>MinIO: 删除文件
       Service->>DB: 删除记录
       Service-->>Client: 返回结果
   ```

### 5.5 性能优化

1. 文件缓存策略
   - Redis缓存临时文件
   - MinIO对象缓存
   - 文件URL缓存

2. 数据库优化
   - mediaFileId索引
   - url索引
   - 组合索引优化

3. 并发处理
   - 文件操作原子性
   - 数据一致性保证
   - 重复上传处理

### 5.6 监控指标

1. 业务指标
   - 上传成功率
   - 文件处理时长
   - 存储空间使用率
   - 文件访问频率

2. 系统指标
   - MinIO连接状态
   - Redis连接状态
   - 服务响应时间
   - 错误率统计

### 5.7 安全措施

1. 文件安全
   - 类型校验
   - 大小限制
   - 内容检测
   - 访问控制

2. 存储安全
   - MinIO访问控制
   - Redis访问控制
   - 数据加密存储
   - 备份机制

3. 接口安全
   - 参数校验
   - 权限控制
   - 防盗链措施
   - 访问频率限制

## 6. 测试覆盖

### 6.1 单元测试
- ImageServiceTest
  - 临时存储测试
  - 永久存储测试
  - 图片更新测试
  - 格式校验测试

### 6.2 HTTP接口测试
- image_test.http
  - 完整上传流程测试
  - 更新操作测试
  - 错误处理测试

## 7. 开发规范

### 7.1 代码规范
- 统一的异常处理
- 统一的返回格式
- 参数校验
- 日志记录

### 7.2 文件命名规范
- 临时文件key: media:temp:image:{uuid}
- MinIO对象名: images/{uuid}.{extension}
- 课程封面文件:
  - mediaFileId格式: course_{organizationId}_{courseId}_{fileMd5}
  - MinIO存储路径: course/logo/{mediaFileId}

## 8. 后续优化建议
1. 添加文件预处理机制
2. 实现视频处理功能
3. 集成CDN服务
4. 完善文件安全检查
5. 添加文件分类功能
6. 优化文件存储结构
7. 实现文件分片上传

## 9. 监控告警
1. 文件上传成功率监控
2. 存储空间使用监控
3. Redis键过期监控
4. 接口响应时间监控
5. 异常情况告警

## 10. 安全措施
1. 文件类型校验
2. 文件大小限制
3. 临时文件过期清理
4. 访问权限控制
5. 防盗链措施

## 11. 异常处理

### 11.1 业务异常
- FILE_EMPTY: 文件为空
- FILE_TOO_LARGE: 超过大小限制(2MB)
- FILE_TOO_SMALL: 小于最小限制(1KB)
- MEDIA_TYPE_NOT_SUPPORT: 不支持的文件类型
- FILE_NOT_EXISTS: 文件不存在
- UPLOAD_ERROR: 上传失败

### 11.2 异常处理示例
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MediaException.class)
    public MediaResponse<?> handleMediaException(MediaException e) {
        return MediaResponse.error(e.getCode(), e.getMessage());
    }
}
```

## 12. 测试用例示例
```java
@Test
void testUploadCourseLogo_FileTooLarge() {
    // 准备超大文件
    byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
    MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", largeContent);

    // 验证异常
    MediaException exception = assertThrows(MediaException.class,
            () -> imageService.uploadCourseLogo(1L, 1L, file));
    assertEquals(MediaErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
} 
```

## 6. 错误码说明

#### 6.1 媒体文件相关错误 (2001xx)
| 错误码 | 说明 |
|--------|------|
| 200101 | 文件不存在 |
| 200102 | 不支持的媒体类型 |
| 200103 | 上传失败 |
| 200104 | 删除失败 |
| 200105 | 文件大小超过限制 |
| 200106 | 文件大小过小 |
| 200107 | 文件为空 |
| 200108 | 文件类型错误 |
| 200109 | 文件上传失败 |

#### 6.2 处理相关错误 (2002xx)
| 错误码 | 说明 |
|--------|------|
| 200201 | 文件处理失败 |
| 200202 | 处理状态错误 |

#### 6.3 MinIO相关错误 (2003xx)
| 错误码 | 说明 |
|--------|------|
| 200301 | MinIO连接失败 |
| 200302 | 存储桶操作失败 |
| 200303 | MinIO上传失败 |

#### 6.4 系统错误 (2999xx)
| 错误码 | 说明 |
|--------|------|
| 299901 | 参数错误 |
| 299999 | 系统内部错误 |


媒体文件分片上传完整流程 (文字说明):
客户端发起 "初始化分片上传" 请求 (Initiate Multipart Upload Request):
客户端 (例如浏览器或 App) 向后端 API /api/media/upload/initiate 发送 POST 请求。
请求体中包含文件元数据信息，例如：fileName (文件名), fileSize (文件大小), mediaType (媒体类型), mimeType (MIME 类型), purpose (文件用途), organizationId (机构 ID) 等。
服务端处理 "初始化分片上传" 请求 (Initiate Multipart Upload):
MediaUploadController 接收到请求，并调用 MediaUploadService.initiateMultipartUpload() 方法。
MediaUploadServiceImpl.initiateMultipartUpload() 方法执行以下操作：
生成唯一的 uploadId (分片上传 ID) 和 mediaFileId (媒体文件 ID)。
确定文件在 MinIO 存储桶中的存储路径 (bucket 和 filePath)。
计算分片大小 (chunkSize) 和总分片数 (totalChunks)。
创建 MultipartUploadRecord 实体对象，记录分片上传会话信息，并将状态设置为 "UPLOADING"。
将 MultipartUploadRecord 实体对象保存到数据库。
构建 InitiateMultipartUploadResponseDTO 响应对象，包含 uploadId, mediaFileId, bucket, filePath, chunkSize, totalChunks 等信息。
MediaUploadController 将 InitiateMultipartUploadResponseDTO 封装在 CommonResponse 中，并返回给客户端。
客户端接收 "初始化分片上传" 响应 (Initiate Multipart Upload Response):
客户端接收到 /api/media/upload/initiate 接口的响应，从中获取 uploadId, chunkSize 等关键信息，并保存起来，用于后续的分片上传操作。
客户端分片上传文件 (Upload Chunks):
客户端将要上传的文件按照 chunkSize 分割成多个分片 (chunk)。
循环上传每个分片: 对于每个分片，客户端执行以下操作：
获取 "分片预签名 URL" (Get Presigned URL for Upload Chunk): 客户端向后端 API /api/media/upload/presigned-url 发送 GET 请求，请求参数包含 uploadId (分片上传 ID) 和 chunkIndex (分片索引，从 1 开始)。 服务端会验证 uploadId 的有效性，并为当前分片生成一个带有上传权限的预签名 URL (Presigned PUT URL)，并返回给客户端。 （这是我们下一步要开发的 API 接口）
使用预签名 URL 上传分片 (Upload Chunk to MinIO): 客户端使用上一步获取的预签名 URL，向 MinIO 对象存储服务发送 PUT 请求，将当前分片的数据作为请求体上传到 MinIO。 客户端直接与 MinIO 服务交互，无需经过后端服务。
后端服务更新已上传分片记录 (Update Uploaded Chunks): （可选步骤，可以优化上传进度展示） 客户端可以异步地向后端 API 发送请求，告知服务端某个分片已经上传完成。 服务端可以更新 MultipartUploadRecord 记录中的 uploadedChunks 字段，用于跟踪上传进度。
客户端发起 "完成分片上传" 请求 (Complete Multipart Upload Request):
当所有分片都上传完成后，客户端向后端 API /api/media/upload/complete 发送 POST 请求。
请求体中包含 uploadId (分片上传 ID)。
服务端处理 "完成分片上传" 请求 (Complete Multipart Upload):
MediaUploadController 接收到请求，并调用 MediaUploadService.completeMultipartUpload() 方法。
MediaUploadServiceImpl.completeMultipartUpload() 方法执行以下操作：
根据 uploadId 从数据库中查询 MultipartUploadRecord 记录。
合并 MinIO 中的所有分片文件，生成最终的完整文件。 （MinIO 服务端合并分片）
验证文件完整性 (可选，例如校验 MD5 或 SHA-256)。
更新 MultipartUploadRecord 记录的状态为 "COMPLETED"，并记录完成时间。
创建 MediaFile 实体对象，记录最终上传完成的媒体文件信息 (例如文件名, 文件大小, 存储路径, 媒体类型, MIME 类型, 用途等)，并保存到数据库。
构建 CompleteMultipartUploadResponseDTO 响应对象，包含最终的 mediaFileId 和文件访问 URL 等信息。
MediaUploadController 将 CompleteMultipartUploadResponseDTO 封装在 CommonResponse 中，并返回给客户端。
客户端接收 "完成分片上传" 响应 (Complete Multipart Upload Response):
客户端接收到 /api/media/upload/complete 接口的响应，从中获取最终的 mediaFileId 和文件访问 URL 等信息。
客户端可以使用文件访问 URL 下载或播放已上传的媒体文件。



需要开发的内容详细描述:
1. 客户端 (Client-side Development):
核心上传功能:
初始化上传请求: 实现调用后端 /initiate-upload API 的功能，发送文件名、文件大小等信息，并接收后端返回的 uploadId、chunkSize 等信息。
获取预签名 URL 请求: 实现循环调用后端 /presigned-url API 的功能，根据 uploadId 和 chunkIndex 获取每个分片的预签名 URL。
分片上传逻辑: 实现使用 fetch API 或选定的上传库，根据预签名 URL 将文件分片上传到 MinIO 的逻辑。需要处理文件分片、循环上传、请求头设置等细节。
完成上传请求: 在所有分片上传完成后，实现调用后端 /complete-upload API 的功能，发送 uploadId 通知后端合并分片。
上传进度显示: 在用户界面上显示上传进度条和上传百分比，实时反馈上传状态。
用户交互和体验:
文件选择: 提供文件选择控件 (例如 <input type="file">)，允许用户选择要上传的视频文件。
上传状态展示: 清晰地展示上传状态，包括 "准备上传"、"上传中"、"上传完成"、"上传失败" 等状态。
错误提示: 当上传过程中发生错误时 (例如网络错误、服务器错误)，向用户显示友好的错误提示信息。
可选功能 (后续迭代):
断点续传: 实现断点续传功能，允许用户在网络中断或关闭浏览器后，下次继续上传未完成的文件。
暂停/取消上传: 提供暂停和取消上传的功能。

```mermaid
sequence
sequenceDiagram
participant Client
participant Backend Service
participant MinIO
participant DB
Client->>Backend Service: 1. 初始化分片上传 (fileName, fileSize, mimeType, purpose, organizationId, fileSha256Hex)
Backend Service->>Backend Service: 生成 Upload ID, 预签名 URL 策略
Backend Service->>DB: 创建 MultipartUploadRecord (INITIATED)
Backend Service-->>Client: 返回 Upload ID, 预签名 URL 策略
loop 分片上传
Client->>Backend Service: 请求分片预签名 URL (Upload ID, 分片序号)
Backend Service->>MinIO: 生成分片预签名 URL (Upload ID, 分片序号)
Backend Service-->>Client: 返回分片预签名 URL
Client->>MinIO: 2. 上传分片数据 (PUT 预签名 URL)
MinIO-->>Client: 返回 OK
Client->>Backend Service: 更新分片上传进度 (Upload ID, uploadedChunks)
Backend Service->>DB: 更新 MultipartUploadRecord (uploadedChunks)
end
Client->>Backend Service: 3. 完成分片上传 (Upload ID)
Backend Service->>DB: 查询 MultipartUploadRecord (Upload ID)
Backend Service->>MinIO: 请求合并分片 (Upload ID)
MinIO->>MinIO: 服务端合并分片
MinIO-->>Backend Service: 返回 OK
Backend Service->>Backend Service: 文件大小校验, SHA-256 校验
alt 校验成功
Backend Service->>DB: 更新 MultipartUploadRecord (COMPLETED), 创建 MediaFile, MediaProcessHistory 记录
Backend Service->>Message Queue: 发送 异步元信息提取任务
Backend Service-->>Client: 4. 上传成功响应
else 校验失败
Backend Service->>Backend Service: 清理已上传分片和合并文件
Backend Service->>DB: 更新 MultipartUploadRecord (FAILED)
Backend Service-->>Client: 4. 上传失败响应 (Error)
end
Message Queue-->>Backend Service: 异步元信息提取任务
Backend Service->>MinIO: 读取完整视频文件
Backend Service->>Backend Service: 提取元信息, 转码等处理
Backend Service->>DB: 更新 MediaFile, MediaProcessHistory 记录
```

**流程图更新说明:**

*   **步骤 1 "初始化分片上传"**:  在请求参数中增加了 `fileSha256Hex`，表示客户端会在初始化上传时提供文件的 SHA-256 哈希值。
*   **循环 "分片上传"**:  在每次分片上传后，客户端会向后端发送 "更新分片上传进度" 请求，后端会更新 `MultipartUploadRecord` 中的 `uploadedChunks` 字段。
*   **步骤 3 "完成分片上传"**:
    *   后端在合并分片后，增加了 "**文件大小校验, SHA-256 校验**" 步骤。
    *   增加了一个 `alt 校验成功/校验失败` 分支，明确表示了校验成功和失败两种情况下的后续流程：
        *   **校验成功**:  继续创建 `MediaFile` 记录，发送消息队列任务，并返回上传成功响应。
        *   **校验失败**:  清理已上传的文件，更新 `MultipartUploadRecord` 状态为 `FAILED`，并返回上传失败响应。

**第一步完成后的测试:**

*   **视觉检查:**  打开 `docs/media.md` 文件，查看流程图是否已经更新，新的步骤和分支是否清晰地显示出来。
*   **理解验证:**  仔细阅读更新后的流程图，确认新的流程是否准确地反映了包含 SHA-256 校验的文件上传流程。

**完成第一步后，请告诉我，我们就可以开始进行第二步，修改 `MultipartUploadRecord.java` 实体类了。**