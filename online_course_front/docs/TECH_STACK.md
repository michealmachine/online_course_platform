# 技术栈说明

## 前端技术栈

### 核心框架
- Next.js 14 (App Router)
- React 19
- TypeScript 5

### UI 组件库
- shadcn/ui (基于 Radix UI)
  - Button
  - Card
  - Dialog
  - Form
  - Input
  - Select
  - Toast
  - Tabs
  - AlertDialog
  - DropdownMenu

### 状态管理
- React Hooks
- Context API

### 表单处理
- React Hook Form
- Zod (表单验证)

### HTTP 请求
- Axios
- 自定义请求封装

### 工具库
- date-fns (日期处理)
- lucide-react (图标)

### 开发工具
- ESLint
- Prettier
- TypeScript
- Cursor IDE

## 后端接口

### 内容服务 (Content Service)
- 课程基本信息管理
- 课程计划管理
- 课程教师管理
- 课程审核流程

### 媒体服务 (Media Service)
- 文件上传
- 文件管理

## 项目结构
```
src/
├── app/                    # 页面路由
├── components/            # 组件
│   ├── ui/               # UI基础组件
│   ├── common/           # 通用业务组件
│   ├── courses/          # 课程相关组件
│   └── teachers/         # 教师相关组件
├── config/               # 配置文件
├── hooks/                # 自定义Hooks
├── lib/                  # 工具函数
└── services/             # API服务
```

## 开发规范

### 组件开发
- 使用 TypeScript
- 组件文件使用 .tsx 后缀
- 组件采用函数式编程
- Props 类型定义清晰

### 样式管理
- 使用 Tailwind CSS
- 遵循移动优先原则
- 组件样式模块化

### 代码质量
- ESLint 检查
- Prettier 格式化
- TypeScript 严格模式
- 代码注释完善

### Git 规范
- 功能分支开发
- 提交信息规范
- 代码审查流程

## 项目结构

```
online_course_front/
├── src/
│   ├── app/              # Next.js App Router 目录
│   ├── components/       # React 组件
│   │   ├── ui/          # 基础 UI 组件
│   │   └── layout/      # 布局组件
│   ├── hooks/           # 自定义 Hooks
│   ├── lib/             # 工具函数和配置
│   └── styles/          # 全局样式
├── public/              # 静态资源
└── docs/               # 项目文档
```

## 最佳实践

### 1. 组件开发
- 使用函数组件和 Hooks
- 遵循单一职责原则
- 组件拆分和复用
- 使用 TypeScript 类型定义
- 添加 JSDoc 注释
- 使用 React.memo 优化渲染性能

### 2. 状态管理
- 本地状态使用 useState
- 复杂状态使用 Zustand
- API 状态使用 TanStack Query
- 避免状态提升过深
- 使用 Context 共享全局配置

### 3. 表单处理
- 使用 React Hook Form 管理表单状态
- 使用 Zod 进行表单验证
- 实现表单联动和条件渲染
- 处理表单提交和错误
- 优化表单性能

### 4. API 集成
- 使用 Axios 拦截器统一处理请求/响应
- 实现请求错误重试
- 实现请求缓存
- 处理并发请求
- 实现请求取消
- 添加请求超时处理

### 5. 性能优化
- 使用 Next.js 的图片组件优化图片加载
- 实现组件懒加载
- 使用 Suspense 和 loading 状态
- 实现数据预取
- 优化首屏加载时间
- 实现页面预渲染

### 6. 错误处理
- 实现全局错误边界
- 统一的错误提示
- API 错误处理
- 表单错误展示
- 404/500 错误页面
- 断网处理

### 7. 用户体验
- 添加加载状态
- 实现骨架屏
- 添加过渡动画
- 实现响应式设计
- 支持键盘操作
- 实现无障碍访问

### 8. 测试
- 单元测试
- 组件测试
- 集成测试
- 端到端测试
- 性能测试
- 覆盖率报告

### 9. 部署和监控
- 自动化部署
- 性能监控
- 错误监控
- 用户行为分析
- SEO 优化
- 安全防护

## 开发规范

### 1. 命名规范
- 组件使用大驼峰命名
- 函数使用小驼峰命名
- 常量使用大写下划线
- 文件名使用小写中划线
- 类型定义使用大驼峰

### 2. 代码组织
- 相关代码放在一起
- 按功能模块组织代码
- 公共代码抽离
- 保持目录结构清晰
- 避免过深的目录层级

### 3. Git 规范
- 遵循 Git Flow 工作流
- 提交信息规范
- 分支命名规范
- 代码审查流程
- 版本发布流程

## 开发内容

### 1. 用户界面
- 响应式布局设计
- 深色模式支持
- 动画和过渡效果
- 组件库定制

### 2. 功能模块
- 用户认证和授权
- 课程管理
- 视频播放器
- 学习进度追踪
- 个人中心
- 搜索和过滤

### 3. 性能优化
- 图片优化
- 代码分割
- 静态生成
- 缓存策略

### 4. 开发规范
- TypeScript 类型定义
- ESLint 规则配置
- 组件文档
- Git 提交规范

## 环境要求
- Node.js 18.18 或更高版本
- npm 9.0 或更高版本 

## 已完成组件
1. 布局组件
  - Navbar：顶部导航栏
  - Sidebar：侧边栏
  - PageHeader：页面头部

2. 业务组件
  - CourseList：课程列表组件
  - CourseFilter：课程筛选组件
  - CourseForm：课程表单组件
  - Pagination：分页组件
  - TeacherList：教师列表组件（开发中）
  - CourseTeacherList：课程教师列表
    - 显示已关联教师
    - 支持关联/解除关联操作
    - 集成教师选择对话框
  - AssociateTeacherDialog：教师选择对话框
    - 显示可选教师列表
    - 支持滚动加载
    - 显示已关联状态
  - TeacherCourseList：教师课程列表
    - 显示教师关联的课程
    - 支持查看课程详情

3. 页面组件
  - 课程列表页
  - 课程创建页
  - 课程编辑页
  - 机构管理页 