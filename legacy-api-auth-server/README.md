# Legacy API Authorization Server Demo

这是一个基于Spring Cloud OAuth2 2.1.0.RELEASE的旧版OAuth2授权服务器示例项目。

## 项目特点

- 基于`spring-cloud-starter-oauth2:2.1.0.RELEASE`
- 提供JWT令牌颁发功能
- 支持`/oauth/token_key`和`/oauth/check_token`接口

## 项目结构

```
src/main/java/com/example/legacy_api_auth_server/
├── LegacyApiAuthServerApplication.java  # 主应用程序入口
└── config/                              # 配置类
    ├── AuthServerConfig.java           # 授权服务器配置
    └── SecurityConfig.java             # 安全配置
```

## 数据库表

项目使用以下表存储OAuth2相关数据：

- `oauth_client_details`: 存储客户端详情
- `oauth_access_token`: 存储访问令牌
- `oauth_refresh_token`: 存储刷新令牌
- `oauth_code`: 存储授权码
- `oauth_approvals`: 存储授权批准信息

## 客户端配置

预配置了以下客户端：

1. `jwt-client`: 使用client_credentials授权类型获取JWT令牌
   - 客户端密钥: jwt-secret
   - 资源ID: resource-id.legacy-api-provider-demo
   - 作用域: message.read
   - 令牌有效期: 24小时

2. `resource-server`: 用于令牌内省
   - 客户端密钥: secret
   - 作用域: introspection

## JWT配置

使用对称密钥进行JWT令牌签名。

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
curl -X POST http://localhost:9001/oauth/token \
  -H "Authorization: Basic and0LWNsaWVudDpqd3Qtc2VjcmV0" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=message.read"
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