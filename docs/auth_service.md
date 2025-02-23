# 认证服务 (Auth Service)

## 1. 服务概述

认证服务是整个系统的安全基础设施，采用 OAuth2 + JWT 的方案，提供统一的身份认证和授权管理。

### 1.1 核心功能模块

#### 1.1.1 用户认证（Authentication）
- 账号密码登录
- 手机号验证码登录（预留）
- 第三方账号登录（预留）
- 双因素认证支持（预留）
- 登录失败处理
  - 账号锁定机制
  - 验证码校验
  - 登录失败次数限制

#### 1.1.2 RBAC权限控制
- 三层权限模型
  - 用户（User）
  - 角色（Role）
  - 权限（Permission）
- 权限管理功能
  - 角色创建与管理
  - 权限分配与回收
  - 权限继承关系
- 动态权限控制
  - 基于注解的权限校验
  - 动态权限更新
  - 权限缓存机制

#### 1.1.3 OAuth2 授权服务
- 支持的授权模式
  - 授权码模式（Authorization Code）
  - 刷新令牌（Refresh Token）
- PKCE 安全增强
  - code_verifier 验证
  - code_challenge 生成
  - PKCE 流程实现
- 客户端管理
  - 客户端注册
  - 客户端认证
  - 授权范围控制

#### 1.1.4 令牌管理
- JWT令牌
  - 访问令牌（Access Token）
  - 刷新令牌（Refresh Token）
- 令牌操作
  - 令牌签发
  - 令牌验证
  - 令牌撤销
  - 令牌内省
- 安全特性
  - 令牌加密
  - 令牌黑名单
  - 令牌自动续期

### 1.2 技术架构

#### 1.2.1 核心框架
- Spring Boot 3.x：应用基础框架
- Spring Security 6.x：安全框架
- Spring Authorization Server：OAuth2授权服务器
- Spring Data JPA：数据访问层

#### 1.2.2 存储方案
- MySQL：
  - 用户数据
  - 角色权限
  - 客户端信息
  - 授权记录
- Redis：
  - 令牌黑名单
  - 权限缓存
  - 验证码存储
  - 登录失败计数

#### 1.2.3 安全实现
- JWT（JSON Web Token）
  - 签名算法：HS512
  - 令牌格式：Header.Payload.Signature
  - 自定义声明支持
- 密码加密
  - BCrypt 加密算法
  - 密码强度校验
  - 密码定期更新机制

#### 1.2.4 集成特性
- 分布式会话
- 跨域支持
- 统一异常处理
- 操作日志记录

### 1.3 安全特性

#### 1.3.1 攻击防护
- XSS 防护
  - 输入验证
  - 输出转义
  - 安全Headers
- CSRF 防护
  - Token 验证
  - SameSite Cookie
- SQL 注入防护
  - 参数绑定
  - 输入过滤
- 暴力破解防护
  - 登录失败限制
  - IP 黑名单
  - 验证码机制

#### 1.3.2 数据安全
- 敏感数据加密
- 传输层安全（TLS）
- 数据脱敏处理
- 审计日志记录

#### 1.3.3 访问控制
- 基于角色的访问控制（RBAC）
- 基于资源的访问控制（RBAC）
- IP 白名单控制
- 时间段访问控制

### 1.4 性能优化

#### 1.4.1 缓存策略
- 多级缓存
  - 本地缓存（Caffeine）
  - 分布式缓存（Redis）
- 缓存管理
  - 缓存更新
  - 缓存失效
  - 缓存预热

#### 1.4.2 并发处理
- 线程池管理
- 请求限流
- 服务降级
- 并发锁处理

### 1.5 可用性保障

#### 1.5.1 监控告警
- 系统监控
  - 服务状态
  - 性能指标
  - 资源使用
- 业务监控
  - 登录异常
  - 权限变更
  - 令牌操作

#### 1.5.2 运维支持
- 配置中心
- 日志中心
- 链路追踪
- 健康检查

### 1.6 扩展能力

#### 1.6.1 三方集成
- 短信服务
- 邮件服务
- 推送服务
- 存储服务

#### 1.6.2 定制化
- 自定义认证方式
- 自定义授权策略
- 自定义令牌格式
- 插件化扩展

## 2. API 端点

### 2.1 认证相关端点

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "string",
    "password": "string",
    "email": "string"
}
```

响应:
```json
{
    "accessToken": "string",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshToken": "string",
    "scope": "string"
}
```

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "string",
    "password": "string"
}
```

响应: 同注册接口

### 2.2 OAuth2 端点

#### 授权请求
```http
GET /api/oauth2/authorize
    ?response_type=code
    &client_id={clientId}
    &redirect_uri={redirectUri}
    &scope={scope}
    &state={state}
    &code_challenge={codeChallenge}
    &code_challenge_method=S256
```

#### 令牌请求
```http
POST /api/oauth2/token
Content-Type: application/json

{
    "grant_type": "authorization_code",
    "code": "string",
    "redirect_uri": "string",
    "client_id": "string",
    "client_secret": "string",
    "code_verifier": "string"
}
```

#### 令牌撤销
```http
POST /api/oauth2/revoke
Content-Type: application/json

{
    "token": "string",
    "token_type_hint": "access_token"
}
```

#### 令牌内省
```http
POST /api/oauth2/introspect
Content-Type: application/json

{
    "token": "string",
    "token_type_hint": "access_token"
}
```

### 2.3 用户管理端点

#### 创建用户（管理员）
```http
POST /api/users
Content-Type: application/json

{
    "username": "string",
    "password": "string",
    "email": "string",
    "roleIds": [1, 2]
}
```

#### 更新用户
```http
PUT /api/users/{userId}
Content-Type: application/json

{
    "email": "string",
    "roleIds": [1, 2]
}
```

## 3. 错误码说明

### 3.1 认证错误 (3003xx)
- 300301: TOKEN_EXPIRED - 令牌已过期
- 300302: TOKEN_INVALID - 无效的令牌
- 300303: TOKEN_REVOKED - 令牌已被撤销

### 3.2 授权错误 (3004xx)
- 300401: CLIENT_UNAUTHORIZED - 未授权的客户端
- 300402: INVALID_AUTHORIZATION_CODE - 无效的授权码
- 300403: INVALID_GRANT - 无效的授权类型

### 3.3 参数错误 (3005xx)
- 300501: PARAMETER_VALIDATION_FAILED - 参数验证失败
- 300502: MISSING_REQUIRED_PARAMETER - 缺少必需参数

## 4. 安全机制

### 4.1 密码安全
- BCrypt 加密存储
- 密码强度要求：
  - 最小长度：8位
  - 必须包含：大小写字母、数字、特殊字符
  - 不允许连续重复字符

### 4.2 令牌安全
- JWT格式
- HS512签名算法
- 访问令牌有效期：1小时
- 刷新令牌有效期：30天
- 支持令牌撤销
- 令牌黑名单机制

### 4.3 登录安全
- 失败次数限制：5次/30分钟
- 账号锁定时间：30分钟
- IP限流：100次/分钟
- 支持登录日志记录

## 5. 缓存策略

### 5.1 Redis缓存
- 令牌黑名单
- 用户权限缓存
- 登录失败计数
- 权限树缓存

### 5.2 缓存时间
- 令牌黑名单：与令牌有效期一致
- 用户权限：30分钟
- 登录失败计数：30分钟
- 权限树：24小时

## 6. 监控指标

### 6.1 性能指标
- API响应时间
- 并发请求数
- 缓存命中率
- 数据库连接数

### 6.2 安全指标
- 登录失败率
- 令牌撤销次数
- 无效令牌访问次数
- 异常IP访问次数

## 7. 部署要求

### 7.1 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- 最小内存：4GB
- 推荐CPU：4核

### 7.2 配置项
```yaml
auth:
  jwt:
    secret: your-jwt-secret
    access-token-validity: 3600
    refresh-token-validity: 2592000
  security:
    password-strength: true
    max-login-attempts: 5
    lock-duration: 1800
  redis:
    token-blacklist-prefix: "token:blacklist:"
    user-permission-prefix: "user:permission:"
```

## 8. 集成示例

### 8.1 客户端接入
```java
@Configuration
public class OAuth2ClientConfig {
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        
        OAuth2AuthorizedClientProvider authorizedClientProvider = 
            OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
                
        DefaultOAuth2AuthorizedClientManager authorizedClientManager = 
            new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, 
                authorizedClientRepository);
                
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        
        return authorizedClientManager;
    }
}
```

### 8.2 资源服务器配置
```java
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.tokenServices(tokenServices());
    }
    
    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }
    
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey("your-jwt-secret");
        return converter;
    }
}
```

## 9. 常见问题

### 9.1 令牌相关
Q: 如何处理令牌过期？
A: 使用刷新令牌获取新的访问令牌，如果刷新令牌也过期，需要重新登录。

Q: 令牌撤销后如何处理？
A: 令牌撤销后会被加入黑名单，需要重新获取令牌。客户端应当捕获401响应并处理重新认证流程。

### 9.2 权限相关
Q: 如何动态更新权限？
A: 权限更新后会自动清除相关缓存，下次请求时会重新加载。

Q: 角色和权限的关系？
A: 采用RBAC模型，用户分配角色，角色包含权限。支持角色继承和权限组。

## 10. 更新日志

### v1.0.0 (2024-02-23)
- 实现基础认证功能
- 完成OAuth2授权服务器
- 添加PKCE支持
- 实现令牌管理功能
- 添加用户权限管理
