# Legacy API Authorization Server Demo

这是一个基于Spring Cloud OAuth2 2.1.0.RELEASE的旧版OAuth2授权服务器示例项目。

## 项目特点

- 基于`spring-cloud-starter-oauth2:2.1.0.RELEASE`
- 提供JWT令牌颁发功能
- 支持`/oauth/token_key`和`/oauth/check_token`接口
- 支持数据库存储令牌
- 提供客户端管理REST API

## 项目结构

```
src/main/java/com/example/legacy_api_auth_server/
├── LegacyApiAuthServerApplication.java  # 主应用程序入口
├── config/                              # 配置类
│   ├── AuthServerConfig.java           # 授权服务器配置
│   └── SecurityConfig.java             # 安全配置
├── controller/                          # 控制器
│   ├── ApiDocController.java           # API文档控制器
│   └── ClientManagementController.java  # 客户端管理控制器
└── service/                             # 服务
    ├── ClientInitializationService.java # 客户端初始化服务
    └── ClientRegistrationService.java   # 客户端注册服务
```

## 数据库表

项目使用以下表存储OAuth2相关数据：

- `oauth_client_details`: 存储客户端详情
- `oauth_access_token`: 存储访问令牌
- `oauth_refresh_token`: 存储刷新令牌
- `oauth_code`: 存储授权码
- `oauth_approvals`: 存储授权批准信息

## 客户端配置

系统在启动时会自动初始化以下客户端：

1. `opaque-client`: 使用client_credentials授权类型获取不透明令牌
   - 客户端密钥: opaque-secret
   - 资源ID: legacy-api
   - 作用域: message.read
   - 令牌有效期: 1小时

2. `jwt-client`: 使用client_credentials授权类型获取JWT令牌
   - 客户端密钥: jwt-secret
   - 资源ID: legacy-api
   - 作用域: message.read
   - 令牌有效期: 1小时

## 客户端管理API

提供以下REST API用于管理OAuth2客户端：

1. 添加客户端: 
   - URL: `/api/clients`
   - 方法: POST
   - 认证: Basic Auth (admin/admin123)
   - 参数:
     - clientId: 客户端ID (必填)
     - clientSecret: 客户端密钥 (必填)
     - resourceIds: 资源ID (必填)
     - scope: 授权范围 (必填)
     - authorizedGrantTypes: 授权类型 (必填)
     - webServerRedirectUri: 重定向URI (可选)
     - authorities: 权限 (可选)
     - accessTokenValidity: 访问令牌有效期(秒) (可选)
     - refreshTokenValidity: 刷新令牌有效期(秒) (可选)
     - additionalInformation: 附加信息 (可选)
     - autoApprove: 自动批准 (可选)

2. 检查客户端是否存在:
   - URL: `/api/clients/{clientId}/exists`
   - 方法: GET
   - 认证: Basic Auth (admin/admin123)

3. 删除客户端:
   - URL: `/api/clients/{clientId}`
   - 方法: DELETE
   - 认证: Basic Auth (admin/admin123)

4. API文档:
   - URL: `/api/docs/clients`
   - 方法: GET
   - 认证: Basic Auth (admin/admin123)

## JWT配置

使用非对称密钥进行JWT令牌签名，密钥信息配置在`application.yml`中。

## 端点信息

- `/oauth/token`: 获取访问令牌
- `/oauth/token_key`: 获取JWT签名密钥
- `/oauth/check_token`: 验证令牌有效性

## 运行项目

```bash
mvn spring-boot:run
```

默认监听9001端口。

## 获取令牌示例

使用client_credentials获取JWT令牌：

```bash
curl -X POST \
  http://localhost:9001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=message.read&client_id=jwt-client&client_secret=jwt-secret"
```

使用client_credentials获取不透明令牌：

```bash
curl -X POST \
  http://localhost:9001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=message.read&client_id=opaque-client&client_secret=opaque-secret"
```

## 添加新客户端示例

```bash
curl -X POST \
  http://localhost:9001/api/clients \
  -u admin:admin123 \
  -d "clientId=my-client&clientSecret=my-secret&resourceIds=legacy-api&scope=message.read&authorizedGrantTypes=client_credentials&accessTokenValidity=3600"
```

## 与资源服务器集成

更新资源服务器的`application.yml`，配置以下属性：

```yaml
security:
  oauth2:
    resource:
      jwt:
        key-uri: http://localhost:9001/oauth/token_key
      token-info-uri: http://localhost:9001/oauth/check_token
```

这将允许资源服务器使用此授权服务器验证令牌。 