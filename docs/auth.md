基于你当前的理解和 MVP 的目标，我强烈建议你按照以下顺序开始开发：

1. 认证服务器核心功能 (最优先):

用户实体和数据访问:

创建 User 实体类，包含 username、password、roles 等字段。

创建 UserRepository 接口，使用 Spring Data JPA 访问数据库，实现用户的CRUD。

安全起见，密码必须使用 BCryptPasswordEncoder 加密存储！

使用 H2 内存数据库快速开始。

用户认证服务 (UserDetailsService):

创建 CustomUserDetailsService 实现 UserDetailsService 接口。

实现 loadUserByUsername 方法，从数据库加载用户信息并构建 UserDetails 对象。

Spring Security 配置:

创建一个配置类 (例如 SecurityConfig.java)，使用 @EnableWebSecurity 注解启用 Spring Security。

配置 HttpSecurity，允许匿名访问 /login 页面，并保护其他 endpoint。

配置 AuthenticationManagerBuilder，使用 CustomUserDetailsService 和 BCryptPasswordEncoder 进行身份验证。

先不考虑 OAuth2，确保基本的用户名/密码登录能够正常工作。

登录页面 (/login):

创建一个简单的 HTML 登录页面，用于输入用户名和密码。

使用 Spring Security 的表单登录功能来处理登录请求。

为什么从这里开始？

这是 OAuth2 的基础： 在实现 OAuth2 之前，你需要先确保用户能够通过用户名/密码登录你的系统。

与业务逻辑无关： 这个阶段的开发与具体的业务逻辑无关，可以让你专注于安全框架的搭建。

快速迭代： 相对简单，可以快速看到效果，增强信心。

2. OAuth2 授权服务器配置 (在核心功能基础上):

添加 OAuth2 依赖:

在 pom.xml 或 build.gradle 中添加 spring-security-oauth2-authorization-server 依赖。

配置 AuthorizationServerConfig.java:

创建一个配置类 (例如 AuthorizationServerConfig.java)，使用 @EnableAuthorizationServer 注解启用 OAuth2 授权服务器。

配置 ClientDetailsServiceConfigurer，定义 OAuth2 客户端的信息 (例如 client_id、client_secret、redirect_uri、grant_types、scopes)。

配置 AuthorizationServerEndpointsConfigurer，配置授权服务器的 endpoints (例如 /oauth/authorize、/oauth/token)。

配置 TokenStore (例如 JwtTokenStore)，用于存储 Access Token 和 Refresh Token。

配置 JwtAccessTokenConverter，用于生成 JWT。

使用内存存储 (例如 InMemoryClientDetailsService、InMemoryTokenStore) 快速开始。

配置 JWT:

生成 JWT 签名密钥。

配置 JwtAccessTokenConverter 使用签名密钥。

在 Access Token 中添加用户信息 (例如用户 ID、角色)。

为什么接下来做 OAuth2 配置？

构建 OAuth2 流程： 让你能够理解 OAuth2 的核心概念和流程。

为后续集成 PKCE 做好准备： 奠定基础。

3. API 网关 JWT 验证 (同步进行):

创建 Spring Cloud Gateway 项目:

使用 Spring Initializr 创建一个 Spring Cloud Gateway 项目。

添加必要的依赖 (例如 spring-cloud-starter-gateway、spring-boot-starter-security、spring-boot-starter-oauth2-resource-server)。

配置 JWT 验证过滤器:

创建一个 GlobalFilter，拦截所有 API 请求。

验证 Authorization 请求头中的 JWT 的有效性 (签名、有效期、issuer)。

提取 JWT 中的用户信息 (例如用户 ID、角色)，并添加到请求头中。

使用 Spring Security 的 ReactiveJwtDecoder 和 ServerHttpSecurity 来验证 JWT。

配置路由规则:

配置路由规则，将请求路由到不同的微服务。

为什么同步进行网关配置？

尽早验证端到端流程： 确保认证服务器颁发的 JWT 能够被 API 网关正确验证。

并行开发： 网关的配置与认证服务器的核心逻辑关联不大，可以并行开发。

4. 实施 PKCE (提升安全性):

前端修改：

生成 code_verifier 和 code_challenge。

在重定向到 /oauth/authorize 端点时，添加 code_challenge 和 code_challenge_method 参数。

在请求 /oauth/token 端点时，添加 code_verifier 参数。

认证服务器修改：

验证 /oauth/token 请求中的 code_verifier 参数。

如果 code_verifier 无效，则拒绝颁发 Access Token。

5. 微服务授权 (最后一步):

移除微服务自身的用户认证逻辑。

配置 Spring Security，允许所有经过 API 网关的请求访问。

使用 @PreAuthorize 注解或 HttpSecurity 配置，根据请求头中的用户信息进行授权。

# 认证服务器开发进度

## 认证服务开发进度

### 已完成功能
1. 基础框架搭建
   - Spring Security 配置
   - JWT 认证
   - 全局异常处理
   - 数据库迁移(Flyway)

2. 用户认证功能
   - 用户注册
   - 用户登录
   - 登录失败处理
   - 账号锁定机制
   - Token 生成与验证

3. 用户管理功能
   - 用户查询
   - 用户创建
   - 用户信息更新
   - 角色权限控制

### 待开发功能
1. 权限管理模块
   - 角色管理（CRUD）
   - 权限管理（CRUD）
   - 角色-权限关联
   - 动态权限控制

2. 安全增强
   - 密码强度校验
   - 图形验证码
   - IP 限流
   - 敏感操作日志

3. 账号管理
   - 密码重置
   - 邮箱验证
   - 账号注销
   - 个人信息管理

## 一、已完成内容

### 1. 基础设施搭建
- 创建auth_service模块
- 配置基础依赖（Spring Security, JPA, MySQL, JWT等）
- 配置JPA自动建表

### 2. 实体层设计
- 创建User实体（包含username、password、roles等字段）
- 创建Role实体（包含name、description字段）
- 创建Permission实体（包含name、description字段）
- 创建RolePermission关联实体
- 实现实体间的关联关系（ManyToMany等）

### 3. 数据访问层
- 创建UserRepository（支持按username查询、检查用户名和邮箱是否存在）
- 创建RoleRepository（支持按name查询角色）
- 创建PermissionRepository

### 4. 安全框架配置
- 创建SecurityConfig配置密码加密器
- 创建SecurityUser实现UserDetails接口
- 实现CustomUserDetailsService用于用户认证
- 配置WebSecurityConfig实现基础的安全配置：
  - 配置认证提供者
  - 配置认证管理器
  - 配置安全过滤链
  - 允许/api/auth/**匿名访问
  - 启用无状态会话管理

## 二、待完成内容

### 1. 认证功能实现
- [ ] 创建认证相关的DTO类（登录请求、注册请求等）
- [ ] 实现认证服务（AuthService）
- [ ] 创建认证控制器（AuthController）
- [ ] 实现用户注册功能
- [ ] 实现用户登录功能
- [ ] 实现JWT token生成和验证

### 2. 角色和权限管理
- [ ] 实现基础角色初始化
- [ ] 实现权限管理功能
- [ ] 实现角色分配功能

### 3. 测试
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 进行接口测试

### 4. 文档
- [ ] 编写API文档
- [ ] 编写部署文档

## 三、下一步计划
1. 创建认证相关的DTO类
2. 实现认证服务
3. 创建认证控制器
4. 实现基本的注册和登录功能

## 四、技术要点记录
1. 使用JPA自动建表而不是Flyway迁移脚本
2. 采用RBAC模型进行权限管理
3. 使用JWT进行token管理
4. 采用无状态会话管理

# 认证服务文档

## 1. 已完成功能

### 1.1 用户认证
- 普通用户注册
- 用户登录
- JWT Token生成与验证
- 密码加密存储

### 1.2 安全机制
- 账户锁定功能
- 登录失败次数统计
- 自动解锁机制
- 基础权限控制

### 1.3 用户管理
- 管理员创建用户（支持多角色分配）
- 用户信息查询
- 基础信息更新

### 1.4 数据结构
- 用户表(users)
  - 基础信息字段
  - 安全相关字段
  - 审计字段
- 角色表(roles)
- 用户角色关联表(user_roles)

## 2. 待开发功能

### 2.1 RBAC权限系统
- [ ] 权限表设计
- [ ] 角色-权限关联
- [ ] 权限检查机制
- [ ] 动态权限控制

### 2.2 用户功能完善
- [ ] 密码重置
- [ ] 账号注销
- [ ] 用户状态管理

### 2.3 集成功能
- [ ] 邮箱验证（等待邮件服务）
- [ ] 手机验证（等待短信服务）
- [ ] 第三方登录支持

### 2.4 系统优化
- [ ] 缓存机制
- [ ] 性能优化
- [ ] 更完善的异常处理
- [ ] 接口文档完善

## 五、最新开发进展

### 1. 完成的功能
1. RBAC权限系统基础实现
   - 完善了Permission实体设计
   - 建立了Role-Permission多对多关联
   - 实现了基于注解的权限控制
   - 完成了JWT认证过滤器

2. 用户管理功能
   - 实现了管理员创建用户功能
   - 支持多角色分配
   - 添加了用户信息验证
   - 完善了错误处理机制

3. 安全机制增强
   - 统一的错误响应格式
   - JWT认证集成
   - 方法级权限控制
   - 全局异常处理

4. 测试覆盖
   - 集成测试完善
   - 用户管理接口测试
   - 权限控制测试
   - 认证流程测试

### 2. 进行中的任务
1. 权限管理功能
   - [ ] 权限创建和管理接口
   - [ ] 角色权限分配接口
   - [ ] 权限验证缓存机制

2. 用户功能完善
   - [ ] 用户搜索优化
   - [ ] 批量操作接口
   - [ ] 用户状态管理

### 3. 待开发功能
1. 集成功能
   - 邮箱验证
   - 手机验证
   - 第三方登录

2. 系统优化
   - 缓存机制
   - 性能优化
   - 接口文档完善

### 4. 技术债务
1. 代码优化
   - 提取公共常量
   - 优化异常处理
   - 完善日志记录

2. 测试完善
   - 单元测试覆盖
   - 性能测试
   - 安全测试

## 六、权限管理模块开发计划

### 1. 数据层设计与实现
#### 1.1 数据库设计
- 权限表(permissions)
  - id: 权限ID
  - code: 权限代码
  - name: 权限名称
  - description: 描述
  - type: 权限类型(菜单/按钮/API)
  - created_at: 创建时间
  - updated_at: 更新时间

- 角色权限关联表(role_permissions)
  - role_id: 角色ID
  - permission_id: 权限ID

#### 1.2 实体类设计
- Permission实体
- RolePermission关联实体
- 相关Repository接口

### 2. 服务层实现
#### 2.1 核心服务
- PermissionService
  - 权限CRUD操作
  - 权限分配与回收
  - 权限树构建
  - 权限缓存管理

#### 2.2 辅助功能
- 权限检查服务
- 权限数据初始化
- 缓存更新策略

### 3. 控制层实现
#### 3.1 接口设计
- 权限管理接口
  - 创建权限
  - 修改权限
  - 删除权限
  - 权限列表查询
  - 权限树查询

- 角色权限管理接口
  - 为角色分配权限
  - 回收角色权限
  - 查询角色权限

### 4. 测试计划
#### 4.1 单元测试
- PermissionService测试
- 权限检查服务测试

#### 4.2 集成测试
- 权限管理接口测试
- 角色权限分配测试
- 权限验证测试
## 七、权限管理模块开发进展

### 1. 已完成功能
1. 权限模型增强
   - 完善了Permission实体设计，增加type和scope字段
   - 支持API、UI和OAuth2三种权限类型
   - 支持OAuth2授权范围管理

2. 数据库结构优化
   - 完成V4数据库迁移脚本
   - 添加权限类型和授权范围字段
   - 预置OAuth2相关权限数据

3. 权限管理功能
   - 实现权限CRUD接口
   - 支持分页查询权限列表
   - 支持按资源类型和操作类型筛选
   - 权限删除时进行使用检查

4. OAuth2授权支持
   - 实现用户授权范围查询
   - 支持授权范围验证
   - 为后续OAuth2集成做准备

### 2. 进行中的任务
1. 角色-权限管理功能
   - [ ] 角色分配权限接口
   - [ ] 角色回收权限接口
   - [ ] 查询角色权限接口
   - [ ] 权限分配验证逻辑

2. 权限树功能
   - [ ] 权限资源分组
   - [ ] 权限树构建
   - [ ] 权限树查询接口

3. 权限缓存优化
   - [ ] 设计缓存策略
   - [ ] 实现缓存管理
   - [ ] 缓存更新机制

### 3. 开发计划

#### 3.1 角色-权限管理（优先级：高）
##### 接口设计
http
POST /api/roles/{roleId}/permissions // 为角色分配权限
DELETE /api/roles/{roleId}/permissions/{permissionId} // 回收角色权限
GET /api/roles/{roleId}/permissions // 查询角色权限列表

##### 数据传输对象
json
// AssignPermissionRequest
{
"permissionIds": [1, 2, 3] // 要分配的权限ID列表
}
// RolePermissionResponse
{
"roleId": 1,
"permissions": [
{
"id": 1,
"name": "user:create",
"type": "API",
"resource": "user",
"action": "create",
"scope": null
}
]
}

##### 实现步骤
1. 创建相关DTO类
2. 实现RoleService中的权限管理方法
3. 实现RoleController接口
4. 编写单元测试和集成测试
5. 添加接口文档

#### 3.2 权限树功能（优先级：中）
##### 数据结构
java
public class PermissionTreeNode {
private String resource; // 资源类型
private List<Permission> permissions; // 该资源下的权限列表
private List<PermissionTreeNode> children; // 子节点
}

##### 实现步骤
1. 创建树形结构相关类
2. 实现树构建算法
3. 添加树形查询接口
4. 编写测试用例

#### 3.3 缓存优化（优先级：低）
##### 缓存设计
- 使用Redis作为缓存存储
- 缓存角色的权限列表
- 设置合理的缓存过期时间

##### 实现步骤
1. 添加Redis依赖和配置
2. 实现缓存管理服务
3. 在权限变更时更新缓存
4. 添加缓存预热功能

### 4. 后续规划
1. 实现动态权限控制
2. 添加权限变更审计日志
3. 优化权限查询性能
4. 完善接口文档

### 5. 技术要点记录
1. 使用MapStruct进行对象映射
2. 采用枚举管理权限类型
3. 实现基于注解的权限控制
4. 使用JPA进行数据访问
5. 集成Swagger/OpenAPI文档

## 权限树功能实现

### 1. 数据结构
- PermissionTreeNode：权限树节点
  - resource: 资源类型
  - description: 资源描述
  - permissions: 该资源下的权限列表
  - sort: 资源排序码

### 2. 配置管理
- ResourceMetaConfig：资源元数据配置
  - 系统权限 (sort: 0)
  - 用户管理 (sort: 1)
  - 角色管理 (sort: 2)
  - 权限管理 (sort: 3)
  - 用户档案 (sort: 4)
  - 测试资源 (sort: 99)

### 3. 功能特点
- 资源分组：按资源类型对权限进行分组
- 排序支持：
  - 资源级别排序（通过sort字段）
  - 权限级别排序（按action字段）
- 资源描述：支持自定义资源描述
- 默认处理：未配置的资源使用默认值

### 4. API接口
http
GET /api/permissions/tree
响应示例：
json
[
{
"resource": "user",
"description": "用户管理",
"sort": 1,
"permissions": [
{
"id": 1,
"name": "user:create",
"description": "创建用户",
"action": "create"
}
]
}
]
### 5. 后续优化方向
1. 权限缓存
   - 实现用户权限缓存
   - 实现角色权限缓存
   - 缓存更新策略

2. 权限树扩展
   - 支持多级权限树
   - 动态资源配置
   - 权限组功能
## 已完成功能

### 缓存实现
1. 添加了 Redis 缓存支持
   - 配置了 Redis 连接和序列化
   - 实现了权限树的缓存管理
   - 添加了角色权限的缓存清理

2. 完善了测试用例
   - 添加了缓存相关的单元测试
   - 修复了集成测试中的问题
   - 完善了权限树测试用例

## 下一步计划

### 1. 权限验证优化
- [ ] 实现基于注解的权限验证
- [ ] 添加权限验证的缓存机制
- [ ] 优化权限验证的性能

### 2. 用户管理功能
- [ ] 实现用户注册
- [ ] 实现用户信息管理
- [ ] 实现用户-角色关联

### 3. 安全性增强
- [ ] 添加密码策略
- [ ] 实现登录失败处理
- [ ] 添加操作日志

### 4. API 文档完善
- [ ] 添加 Swagger 文档
- [ ] 完善接口说明
- [ ] 添加接口示例

### 5. 监控与运维
- [ ] 添加健康检查
- [ ] 实现性能监控
- [ ] 添加操作审计


第一阶段: OAuth2基础设施搭建
a. 依赖添加
- spring-security-oauth2-authorization-server
- 相关依赖更新

b. 配置文件扩展
- oauth2相关配置(端点、token有效期等)
- CORS配置调整
- 安全配置调整

c. 数据库扩展
- 添加oauth2_client表(存储客户端信息)
- 添加oauth2_authorization表(存储授权信息)
- 添加oauth2_authorization_consent表(存储授权同意信息)

第二阶段: 授权服务器核心功能
a. 授权服务器配置
- AuthorizationServerConfig配置类
- 配置授权端点
- 配置Token端点
- 配置客户端管理
- 配置Token管理

b. PKCE支持
- 添加code_verifier验证
- 实现PKCE流程
- 扩展授权请求验证

c. Token增强
- 自定义JWT负载信息
- 添加权限范围(scope)信息
- 配置Token格式

第三阶段: 业务代码适配
a. 认证流程调整
- 修改AuthController
- 调整登录流程
- 添加授权确认页面
- 实现授权码获取接口

b. 响应对象调整
- 扩展AuthResponse
- 添加OAuth2相关DTO
- 标准化错误响应

c. 权限模型适配
- 完善Permission的OAuth2属性
- 调整权限验证逻辑
- 实现Scope到Permission的映射

第四阶段: 安全增强
a. 授权流程安全性
- 添加state参数验证
- 实现CSRF防护
- 添加请求来源验证

b. Token安全性
- 实现Token撤销
- 添加Token黑名单
- 实现Refresh Token轮换

c. 客户端管理
- 实现客户端注册
- 动态客户端管理
- 客户端认证增强

. 第五阶段: 测试与文档

a. 单元测试
- OAuth2配置测试
- PKCE流程测试
- Token管理测试

b. 集成测试
- 授权流程测试
- 客户端测试
- 端到端测试

c. 文档更新
- API文档更新
- 流程说明文档
- 部署文档更新

## 已完成功能

### OAuth2 授权码模式实现
1. 授权流程
   - 实现了 AuthorizationConsentService 处理用户授权
   - 完成授权码生成和存储
   - 实现了授权码验证和消费逻辑

2. 令牌管理
   - 实现了 TokenController 处理令牌请求
   - 完成 TokenService 处理令牌业务逻辑
   - 实现了 JwtService 处理 JWT 令牌的生成和验证
   - 添加了 JwtTokenProvider 处理底层 JWT 操作

3. 安全配置
   - 合并和优化了 SecurityConfig
   - 配置了必要的端点权限
   - 实现了 JWT 认证过滤器

4. 异常处理
   - 完善了全局异常处理
   - 统一了错误码和状态码映射
   - 标准化了错误响应格式

5. 测试覆盖
   - 添加了 TokenControllerIntegrationTest
   - 完成了 JwtServiceTest
   - 实现了 TokenServiceTest

### 令牌管理
- 实现了令牌内省功能，支持令牌验证和信息获取

## 下一步计划

### 1. 刷新令牌功能（优先级：高）
- [ ] 实现刷新令牌的生成和存储
- [ ] 添加刷新令牌的验证逻辑
- [ ] 实现令牌刷新接口
- [ ] 添加相关测试用例

### 2. PKCE 支持（优先级：高）
- [ ] 实现 PKCE 验证流程
- [ ] 扩展授权请求验证
- [ ] 添加 code_verifier 验证
- [ ] 编写 PKCE 相关测试

### 3. 用户信息端点（优先级：中）
- [ ] 实现用户信息获取接口
- [ ] 添加用户信息的权限控制
- [ ] 支持自定义用户信息返回
- [ ] 完善相关测试

### 4. 其他授权模式（优先级：低）
- [ ] 实现密码模式
- [ ] 实现客户端模式
- [ ] 添加相应的测试用例

### 5. 安全增强（优先级：中）
- [ ] 实现 scope 验证机制
- [ ] 添加 state 参数验证
- [ ] 增强客户端认证
- [ ] 完善安全相关测试

## 授权服务API文档

### 目录
1. [客户端注册](#客户端注册)
2. [授权码模式](#授权码模式)
3. [令牌端点](#令牌端点)
4. [令牌撤销](#令牌撤销)
5. [令牌内省](#令牌内省)

### 客户端注册
...（保持现有内容不变）

### 授权码模式
...（保持现有内容不变）

### 令牌端点
...（保持现有内容不变）

### 令牌撤销

#### 请求
- 方法：POST
- 路径：/api/oauth2/revoke
- Content-Type: application/json

```json
{
    "token": "access_token或refresh_token",
    "tokenTypeHint": "access_token"  // 可选，指示令牌类型
}
```

#### 响应
- 成功：HTTP 200
```json
// 无响应体
```

- 失败：HTTP 400/401
```json
{
    "code": 300302,
    "message": "无效的Token"
}
```

#### 错误码
- 300302: 无效的Token
- 300501: 参数验证失败

#### 说明
1. 该接口用于撤销访问令牌或刷新令牌
2. 撤销后的令牌将无法继续使用
3. tokenTypeHint 参数可选，用于提示令牌类型，但服务器会自行验证实际类型
4. 令牌一旦被撤销将无法恢复，需要重新获取新的令牌

### 令牌内省

#### 请求
- 方法：POST
- 路径：/api/oauth2/introspect
- Content-Type: application/json

```json
{
    "token": "访问令牌或刷新令牌",
    "tokenTypeHint": "access_token"  // 可选，指示令牌类型
}
```

#### 响应
- 成功：HTTP 200
```json
{
    "active": true,           // 令牌是否有效
    "clientId": "client_id",  // 客户端ID
    "userId": "user_id",      // 用户ID
    "scope": "read write",    // 权限范围
    "exp": 1740247864,       // 过期时间（Unix时间戳）
    "iat": 1740244264,       // 签发时间（Unix时间戳）
    "tokenType": "access_token" // 令牌类型
}
```

- 令牌无效：HTTP 200
```json
{
    "active": false
}
```

- 请求错误：HTTP 400
```json
{
    "code": 300501,
    "message": "参数验证失败"
}
```

#### 说明
1. 该接口用于验证令牌的有效性并获取令牌信息
2. 支持验证访问令牌和刷新令牌
3. 当令牌无效（过期、被撤销等）时，返回 active=false
4. 该接口主要供资源服务器使用，用于验证令牌

## 安全考虑
...（保持现有内容不变）