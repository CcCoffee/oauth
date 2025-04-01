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

```
客户端ID: messaging-client
客户端密钥: secret
授权类型: client_credentials
作用域: message.read
```

## 配置说明

主要配置在`com.example.api_auth_server.config.AuthServerConfig`类中，包括：

- JWT令牌的签名密钥配置
- 客户端凭证配置
- 令牌有效期配置
- 授权服务器安全设置

## 测试令牌获取

您可以使用以下命令测试客户端凭证授权流程：

```bash
# 获取访问令牌
curl -X POST -u "messaging-client:secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"
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