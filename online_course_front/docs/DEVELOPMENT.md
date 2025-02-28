# 开发文档

## 项目说明

在线课程管理系统的前端项目，基于 Next.js 14 开发。

## 功能模块

### 1. 课程管理

#### 1.1 课程基本信息
- 课程创建
- 课程编辑
- 课程列表
- 课程状态管理

#### 1.2 课程计划管理
- 章节管理
  - 创建/编辑章节
  - 章节排序
  - 章节删除
- 小节管理
  - 创建/编辑小节
  - 小节排序
  - 小节删除

### 2. 路由说明

#### 2.1 课程管理路由
- `/courses` - 公开课程列表页（已审核通过的课程）
- `/organization/courses` - 机构课程管理页面
  - 显示机构所有课程（所有状态）
  - 包含创建课程入口
  - 提供课程管理操作
- `/organization/courses/create` - 课程创建页
- `/organization/courses/{courseId}/edit` - 课程编辑页
  - 默认显示基本信息
  - 切换标签页显示不同内容：
    - 课程计划
    - 课程教师（开发中）
    - 课程资源（开发中）

### 3. 组件说明

#### 3.1 课程相关组件
- `CourseList` - 公开课程列表组件（已审核通过的课程）
- `OrganizationCourseList` - 机构课程列表组件（所有状态）
- `CourseForm` - 课程表单组件
- `CourseFilter` - 课程筛选组件（支持机构ID筛选）
- `CourseChapterList` - 课程章节列表组件
- `ChapterForm` - 章节表单组件
- `SectionForm` - 小节表单组件

### 4. API 接口

#### 4.1 内容服务接口
```typescript
// 课程相关
COURSE: {
  LIST: '/course/list',               // 公开课程列表
  ORGANIZATION_LIST: '/course/list',  // 机构课程列表（需要传入organizationId）
  CREATE: '/course',
  UPDATE: '/course',
  DETAIL: '/course/:id',
  AUDIT: '/course/:id/audit',
  PUBLISH: '/course/:id/publish',
  OFFLINE: '/course/:id/offline',
}

// 课程计划相关
TEACHPLAN: {
  TREE: '/teachplan/tree/:courseId',
  SAVE: '/teachplan',
  DELETE: '/teachplan/:id',
  MOVE_UP: '/teachplan/moveup/:id',
  MOVE_DOWN: '/teachplan/movedown/:id',
}
```

### 5. 开发规范

#### 5.1 组件开发规范
1. 组件文件命名采用 kebab-case
2. 组件名称采用 PascalCase
3. Props 类型必须定义清晰
4. 组件内部状态使用 hooks 管理
5. 复杂逻辑抽离为自定义 hooks

#### 5.2 样式开发规范
1. 使用 Tailwind CSS 类名
2. 遵循移动优先原则
3. 组件样式保持独立
4. 使用 shadcn/ui 提供的基础组件

#### 5.3 状态管理规范
1. 组件内部状态使用 useState
2. 表单状态使用 React Hook Form
3. 共享状态使用 Context API
4. API 请求使用自定义 hooks

#### 5.4 错误处理规范
1. API 错误统一处理
2. 表单验证错误友好提示
3. 加载状态明确展示
4. 用户操作结果及时反馈

#### 5.5 数据隔离规范
1. 公开课程列表
   - 只显示已审核通过的课程
   - 不需要机构ID筛选
   - 用于课程展示和选课

2. 机构课程管理
   - 必须传入机构ID
   - 显示该机构的所有状态课程
   - 提供完整的课程管理功能

### 6. 待开发功能

1. 课程教师管理
   - 教师列表
   - 添加/删除教师
   - 教师排序

2. 课程资源管理
   - 课程图片上传
   - 课程资料管理
   - 资源预览

3. 课程审核流程
   - 提交审核
   - 审核状态
   - 审核记录

### 7. 注意事项

1. 代码提交前必须：
   - 通过 TypeScript 检查
   - 通过 ESLint 检查
   - 完成代码格式化
   - 更新相关文档

2. 新功能开发流程：
   - 创建功能分支
   - 编写组件和功能
   - 编写/更新测试
   - 提交代码审查
   - 合并到主分支

3. 文档维护要求：
   - 及时更新开发文档
   - 添加代码注释
   - 更新 API 文档
   - 记录重要决策

## 项目设置

### 环境要求
- Node.js >= 18
- npm >= 9

### 开发环境设置
1. 安装依赖
```bash
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

## 开发规范

### 1. 组件开发规范

#### 1.1 客户端组件 vs 服务器组件
- 默认所有组件都是服务器组件
- 需要使用客户端特性的组件（如 hooks、浏览器 API 等）必须在文件顶部添加 `"use client"` 指令
- 常见的需要 `"use client"` 的场景：
  - 使用 React hooks (useState, useEffect 等)
  - 使用浏览器 API
  - 使用事件处理器
  - 使用 Next.js 的客户端 hooks (useRouter, useSearchParams 等)

#### 1.2 组件文件结构
```typescript
// 1. "use client" 指令（如果需要）
"use client";

// 2. 导入语句
import { useState } from 'react';

// 3. 类型定义
interface Props {
  // ...
}

// 4. 组件定义
export function MyComponent({ prop1, prop2 }: Props) {
  // ...
}
```

### 2. UI 组件库使用规范

#### 2.1 shadcn/ui 组件安装
- 使用 `npx shadcn add [component-name]` 安装组件
- 不要使用 `shadcn-ui` 或其他过时的命令
- 示例：
```bash
# 安装 button 组件
npx shadcn add button

# 安装多个组件
npx shadcn add button card dialog
```

#### 2.2 自定义组件样式
- 在 `src/components/ui` 目录下修改组件样式
- 使用 Tailwind CSS 类名自定义样式
- 保持组件的基础功能不变

### 3. API 集成规范

#### 3.1 API 配置
- 所有 API 配置都在 `src/config/api.config.ts` 中定义
- 包括：
  - 服务配置（baseURL、超时时间等）
  - 错误码定义
  - API 路径定义

#### 3.2 请求工具使用
- 使用封装的 `request` 工具发送请求
- 定义完整的类型
- 处理错误情况
- 示例：
```typescript
// 定义响应类型
interface CourseResponse {
  id: number;
  name: string;
  // ...
}

// 发送请求
const course = await contentRequest.get<CourseResponse>('/course/1');
```

#### 3.3 代理配置
- 在 `next.config.js` 中配置代理
- 开发环境代理规则：
  - `/api/content/*` -> `http://localhost:8080/*`
  - `/api/media/*` -> `http://localhost:8081/*`

### 4. 错误处理规范

#### 4.1 API 错误
- 使用 `ApiError` 类处理业务错误
- 在组件中捕获并显示错误信息
- 示例：
```typescript
try {
  await courseService.createCourse(data);
} catch (error) {
  if (error instanceof ApiError) {
    // 处理业务错误
    toast.error(error.message);
  } else {
    // 处理其他错误
    toast.error('系统错误，请稍后重试');
  }
}
```

#### 4.2 表单验证
- 使用 zod 进行表单验证
- 定义清晰的验证规则
- 显示友好的错误信息

### 5. 状态管理规范

#### 5.1 本地状态
- 使用 React hooks 管理组件内部状态
- 避免过度使用全局状态

#### 5.2 全局状态
- 使用 Zustand 管理全局状态
- 按功能模块拆分 store
- 定义清晰的 action 和 state

### 6. 代码注释规范

#### 6.1 必须添加注释的场景
- 组件的 props 定义
- 复杂的业务逻辑
- 工具函数
- API 调用
- 状态管理
- 示例：
```typescript
/**
 * 课程列表组件
 * @param props.page 当前页码
 * @param props.pageSize 每页数量
 */
interface CourseListProps {
  page: number;
  pageSize: number;
}

/**
 * 获取课程列表
 * @param params 查询参数
 * @returns 课程列表和分页信息
 */
async function getCourses(params: CourseQueryParams) {
  // ...
}
```

#### 6.2 注释格式
- 使用 JSDoc 格式
- 包含参数说明
- 包含返回值说明
- 包含示例代码（如果需要）

### 7. 文件组织规范

```
src/
├── app/                # 页面目录
├── components/         # 组件目录
│   ├── ui/            # UI 基础组件
│   ├── common/        # 通用业务组件
│   └── [feature]/     # 特性相关组件
├── config/            # 配置文件
├── lib/              # 工具库
├── services/         # API 服务
└── types/            # 类型定义
```

## 常见问题

### 1. 客户端组件错误
问题：使用 hooks 报错  
解决：添加 `"use client"` 指令

### 2. API 请求错误
问题：Invalid URL  
解决：检查 API 配置和代理设置

### 3. 类型错误
问题：TS 类型不匹配  
解决：确保定义了完整的类型，并正确使用

### 4. 水合（Hydration）错误
问题：服务端渲染的 HTML 与客户端不匹配  
解决方案：
1. 检查组件是否包含动态内容（如 `Date.now()`、`Math.random()`）
2. 使用 `useState` 和 `useEffect` 管理动态数据
3. 添加适当的加载状态
4. 确保条件渲染的一致性
5. 禁用可能影响渲染的浏览器插件（如 Dark Reader）

示例：
```typescript
// ❌ 可能导致水合错误
export async function Component() {
  const data = await fetchData();
  return <div>{data}</div>;
}

// ✅ 正确处理
export function Component() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData().then(result => {
      setData(result);
      setLoading(false);
    });
  }, []);

  if (loading) return <div>加载中...</div>;
  return <div>{data}</div>;
}
```

### 5. 图片加载错误
问题：图片无法加载或显示  
解决方案：
1. 检查 `next.config.js` 中的 `images` 配置
2. 为重要图片添加 `priority` 属性
3. 提供合适的占位图
4. 使用 `onError` 处理加载失败的情况 

## 临时开发说明

### 1. 认证与授权
在用户认证系统完成之前，我们采用以下临时方案：

#### 1.1 机构隔离
- 开发阶段手动填写机构ID
```typescript
// src/components/courses/course-form.tsx
interface CourseFormData {
  organizationId: number; // 临时字段：手动填写机构ID
  name: string;
  brief: string;
  // ... 其他字段
}

// src/components/courses/course-filter.tsx
interface CourseFilterData {
  organizationId?: number; // 临时字段：按机构ID筛选
  courseName?: string;
  status?: string;
}
```

#### 1.2 后续完整方案
1. 用户认证
   - 实现登录/注册接口
   - 获取用户所属机构信息
   - 移除手动填写机构ID的字段
   - 从用户会话中获取机构ID

2. 权限控制
   - 基于角色的访问控制（RBAC）
   - 数据权限（按机构隔离）
   - 操作权限
   - 菜单权限

### 2. 课程管理临时方案

#### 2.1 创建课程
- 临时使用固定的机构ID和教师ID
```typescript
// src/services/course.ts
interface CreateCourseParams {
  // ... 其他字段 ...
  organizationId: number; // 临时固定为 1
  teacherId: number;      // 临时固定为 1
}
```

#### 2.2 课程操作
- 临时跳过权限检查
- 所有用户都可以执行所有操作
- 后续需要根据用户角色和权限限制操作

### 3. 媒体服务临时方案

#### 3.1 文件上传
- 临时使用固定的上传参数
```typescript
// src/services/media.ts
interface UploadParams {
  file: File;
  organizationId: number; // 临时固定为 1
  userId: number;         // 临时固定为 1
  purpose: string;
}
```

## 注意事项

1. 所有临时方案都应该：
   - 在代码中使用 `TODO` 注释标记
   - 集中管理临时配置
   - 方便后续替换
   - 记录在文档中

2. 临时值的使用：
   - 使用常量定义
   - 统一管理位置
   - 明确标记用途
   - 便于全局替换

3. 后续替换计划：
   - 完整的用户认证系统
   - 基于角色的权限控制
   - 多机构支持
   - 数据隔离

4. 安全考虑：
   - 临时方案仅用于开发环境
   - 不要在生产环境使用
   - 及时移除测试数据
   - 做好数据备份 

### 2. 页面结构
```
src/
└── app/
    └── courses/
        ├── page.tsx             # 课程列表页
        ├── create/
        │   └── page.tsx         # 课程创建页
        └── [id]/
            └── edit/
                └── page.tsx     # 课程编辑页
    └── organization/
        └── courses/
            └── page.tsx         # 机构管理页面
```

### 3. 服务和工具
```
src/
├── services/
│   ├── index.ts                # API请求封装
│   ├── course.ts               # 课程相关API
│   └── teacher.ts              # 教师相关API
└── config/
    └── api.config.ts           # API配置
```

### 4. 临时开发说明

#### 4.1 机构隔离
- 开发阶段在表单中手动填写机构ID
- 课程列表支持按机构ID筛选
- 创建课程时手动输入机构ID
- 便于测试不同机构的数据隔离
- 后续需要改造：
  - 接入统一认证系统后，移除手动填写机构ID的字段
  - 改为从登录用户信息中获取机构ID
  - 添加机构切换功能（如果需要）
  - 实现基于用户权限的数据隔离 

### 课程教师管理

#### 1. 教师关联功能
- 支持查看课程关联的教师列表
- 支持关联新教师到课程
- 支持解除教师与课程的关联
- 支持查看教师关联的课程列表

#### 2. 用户体验优化
- 对话框内容区域限制最大高度，超出显示滚动条
- 已关联教师显示禁用状态，防止重复关联
- 操作后即时更新列表
- 添加加载状态和错误提示 

### 课程审核管理

#### 1. 管理员课程列表
- 支持多条件组合筛选
  - 机构ID筛选
  - 课程名称搜索
  - 课程状态筛选
  - 审核状态筛选
- 分页加载优化
- 课程信息展示优化

#### 2. 审核流程
- 审核状态管理
  - 待审核（202301）
  - 审核通过（202303）
  - 审核不通过（202302）
- 审核操作
  - 支持通过/驳回
  - 驳回必须填写意见
  - 审核后自动刷新列表 

### 图片上传功能实现指南

#### 1. 基础组件
1. ImageUpload 组件
   ```typescript
   interface ImageUploadProps {
     value?: string;
     onChange?: (value: string) => void;
     onUpload: (file: File) => Promise<string>;
     onDelete?: () => Promise<void>;
     loading?: boolean;
     aspectRatio?: number;
     maxSize?: number;
     accept?: string;
   }
   ```

2. ImageCropper 组件
   ```typescript
   interface ImageCropperProps {
     open: boolean;
     onClose: () => void;
     imageUrl: string;
     aspectRatio?: number;
     onCropComplete: (croppedImage: Blob) => void;
   }
   ```

3. ImagePreview 组件
   ```typescript
   interface ImagePreviewProps {
     src: string;
     alt?: string;
     open: boolean;
     onClose: () => void;
   }
   ```

#### 2. 业务实现流程
1. 临时上传
   ```typescript
   const handleFileSelect = async (file: File) => {
     const url = URL.createObjectURL(file);
     setTempImageUrl(url);
     setCropperOpen(true);
   };
   ```

2. 图片裁剪
   ```typescript
   const handleCropComplete = async (croppedBlob: Blob) => {
     const tempKey = await uploadToTemp(croppedBlob);
     setTempKey(tempKey);
   };
   ```

3. 确认使用
   ```typescript
   const handleConfirm = async () => {
     await confirmUpload(tempKey);
     onChange?.(tempKey);
   };
   ```

#### 3. URL 处理
```typescript
export function getMediaUrl(key?: string): string {
  if (!key) return '';
  if (key.startsWith('http')) return key;
  const baseUrl = process.env.NEXT_PUBLIC_MEDIA_URL;
  return `${baseUrl}${key}`;
}
```

#### 4. 注意事项
1. 文件处理
   - 验证文件类型和大小
   - 处理文件名和扩展名
   - 清理临时文件

2. 错误处理
   - 上传失败重试
   - 友好的错误提示
   - 网络异常处理

3. 性能优化
   - 图片压缩
   - 延迟加载
   - 缓存策略

4. 安全考虑
   - 文件类型限制
   - 大小限制
   - 访问权限控制 