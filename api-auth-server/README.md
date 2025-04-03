# OAuth2授权服务器

这是一个基于Spring Authorization Server的OAuth2授权服务器演示项目，专注于提供客户端凭证授权流程支持。

## 项目架构

本项目是OAuth2授权架构中的授权服务器组件，与资源服务器配合使用：

- **授权服务器(api-auth-server)**：本项目，负责颁发和验证OAuth2令牌
- **API提供者(api-provider-demo)**：资源服务器，提供受OAuth2保护的API资源
- **API消费者(api-consumer-demo)**：客户端应用，使用客户端凭证访问API

### 技术栈

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Authorization Server

## 快速开始

### 先决条件

- JDK 21+
- Maven 3.6+
- PostgreSQL 数据库

### 生成密钥库

在运行项目之前，需要生成JWT签名使用的密钥库：

```bash
# 生成RSA密钥对并存储到JKS密钥库
keytool -genkeypair -alias myalias -keyalg RSA -keysize 2048 -storetype JKS -keystore mykeystore.jks -storepass mykeystorepass -keypass mykeypass -dname "CN=localhost, OU=Development, O=Example, L=City, S=State, C=CN"

# 将生成的密钥库文件移动到项目资源目录
mv mykeystore.jks api-auth-server/src/main/resources/
```

### 构建项目

```bash
mvn clean package
```

### 运行项目

```bash
mvn spring-boot:run
```

或者使用JAR文件启动：

```bash
java -jar target/api-auth-server-0.0.1-SNAPSHOT.jar
```

服务将在端口9000上启动。

## 授权服务器端点

项目提供以下OAuth2标准端点：

- **授权端点**: `/oauth2/authorize`
- **令牌端点**: `/oauth2/token`
- **令牌撤销端点**: `/oauth2/revoke`
- **令牌内省端点**: `/oauth2/introspect`
- **JWK集端点**: `/oauth2/jwks`
- **OpenID Connect发现端点**: `/.well-known/openid-configuration`

## 客户端凭证授权流程

授权服务器支持客户端凭证授权流程，主要用于服务器到服务器的API访问：

1. 客户端以自身名义（而非用户）请求访问令牌
2. 授权服务器验证客户端凭证（ID和密钥）
3. 验证通过后，授权服务器颁发访问令牌
4. 客户端使用访问令牌访问受保护的API资源

## 预配置客户端

授权服务器预配置了以下OAuth2客户端：

### 不透明令牌客户端
```
客户端ID: opaque-client
客户端密钥: opaque-secret
授权类型: client_credentials
作用域: message.read
令牌格式: 不透明令牌
有效期: 30分钟
```

### JWT令牌客户端
```
客户端ID: jwt-client
客户端密钥: jwt-secret
授权类型: client_credentials
作用域: message.read
令牌格式: JWT
有效期: 1小时
```

### 资源服务器客户端
```
客户端ID: resource-server
客户端密钥: secret
用途: 令牌内省
作用域: introspection
```

## 集群部署指南

本项目支持在多实例集群环境中部署，关键配置包括：

### 1. 数据库配置

所有实例必须连接到相同的PostgreSQL数据库。在`application.yml`中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-db-host:5432/oauth
    username: your-username
    password: your-password
    driver-class-name: org.postgresql.Driver
```

### 2. 数据库表结构

在首次启动前，确保PostgreSQL数据库中已创建所需表结构。项目会自动初始化表结构，具体SQL脚本位于：
`src/main/resources/schema/oauth2-schema.sql`

### 3. 密钥管理

集群所有节点共享相同的JWT签名密钥。系统自动通过数据库表 `oauth2_jwt_keys` 管理密钥：
- 首次启动时，系统会生成新密钥并存储到数据库
- 后续所有实例都会从数据库获取相同的密钥
- 密钥自动在集群间同步，无需手动配置

### 4. 负载均衡配置

在使用负载均衡器时，需要修改授权服务器颁发者URL：

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://your-load-balancer-domain
```

所有资源服务器也需要使用相同的颁发者URL。

### 5. 会话共享

本项目为无状态服务，所有状态都存储在数据库中，无需额外配置会话共享。

## 测试令牌获取

您可以使用以下命令测试客户端凭证授权流程：

### 获取不透明令牌
```bash
curl -X POST \
  http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'opaque-client:opaque-secret' | base64)" \
  -d "grant_type=client_credentials&scope=message.read"
```

### 获取JWT令牌
```bash
curl -X POST \
  http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'jwt-client:jwt-secret' | base64)" \
  -d "grant_type=client_credentials&scope=message.read"
```

## JWT令牌格式

授权服务器颁发的JWT令牌包含以下标准声明：

- `iss`：颁发者，值为授权服务器URL
- `sub`：主题，值为客户端ID
- `aud`：受众，值为资源服务器标识符
- `exp`：过期时间
- `iat`：颁发时间
- `scope`：权限范围

## 项目结构

```
api-auth-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_auth_server/
│   │   │               ├── ApiAuthServerApplication.java    # 应用程序入口
│   │   │               └── config/
│   │   │                   └── AuthServerConfig.java        # 授权服务器配置
│   │   └── resources/
│   │       └── application.properties                       # 应用配置
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── ApiAuthServerApplicationTests.java   # 测试类
└── pom.xml                                                  # Maven配置
```

## 安全注意事项

- 在生产环境中，应确保使用HTTPS保护所有通信
- 客户端密钥应妥善保管，避免泄露
- 适当设置令牌有效期，定期轮换密钥
- 根据最小权限原则配置客户端权限范围

## 许可证

本项目采用MIT许可证 