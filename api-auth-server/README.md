# OAuth2授权服务器

这是一个基于Spring Authorization Server的OAuth2授权服务器演示项目，提供OAuth2认证和授权功能。

## 项目架构

本项目是OAuth2授权架构中的授权服务器组件，与资源服务器配合使用：

- **授权服务器(api-auth-server)**：本项目，负责颁发和验证OAuth2令牌
- **API提供者(api-provider-demo)**：资源服务器，提供受OAuth2保护的API资源

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

## 预配置客户端

授权服务器预配置了以下OAuth2客户端：

```
客户端ID: messaging-client
客户端密钥: secret
授权类型: authorization_code, refresh_token, client_credentials
重定向URI: http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc, http://127.0.0.1:8080/authorized
作用域: openid, profile, message.read, message.write
```

## 预配置用户

```
用户名: user
密码: password
角色: USER
```

## 配置说明

主要配置在`com.example.api_auth_server.config.AuthServerConfig`类中，定义了：

- OAuth2授权服务器安全过滤器链
- 默认安全过滤器链
- 用户详情服务
- 注册客户端仓库
- JWK源和相关密钥配置
- JWT解码器
- 授权服务器设置

## 测试

您可以使用项目`api-provider-demo`中的测试脚本进行测试，或使用以下命令：

### 客户端凭证授权流程

```bash
# 获取访问令牌
curl -X POST -u "messaging-client:secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

### 授权码授权流程

1. 在浏览器中访问：
```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=messaging-client&scope=openid profile&redirect_uri=http://127.0.0.1:8080/authorized
```

2. 登录（user/password）并授权

3. 获取授权码后，使用授权码交换访问令牌：
```bash
curl -X POST -u "messaging-client:secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=authorization_code&code=YOUR_AUTHORIZATION_CODE&redirect_uri=http://127.0.0.1:8080/authorized" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

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
│                   └── api_auth_server/
│                       └── ApiAuthServerApplicationTests.java  # 测试类
└── pom.xml                                                    # Maven配置
```

## OAuth2和OpenID Connect支持

本授权服务器支持：

- OAuth2.0的所有标准授权类型
- OpenID Connect 1.0

## 贡献指南

欢迎提交问题和改进建议！

## 许可证

本项目采用MIT许可证 