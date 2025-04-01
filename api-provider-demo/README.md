# API提供者演示项目

这是一个基于Spring Boot的OAuth2资源服务器演示项目，演示如何创建受客户端凭证授权保护的API端点。

## 项目架构

本项目是OAuth2授权架构中的资源服务器组件，与授权服务器配合使用：

- **授权服务器(api-auth-server)**：负责颁发和验证OAuth2令牌
- **API提供者(api-provider-demo)**：本项目，提供受OAuth2保护的API资源
- **API消费者(api-consumer-demo)**：客户端应用，使用客户端凭证访问API

### 技术栈

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring OAuth2 Resource Server

## 快速开始

### 先决条件

- JDK 21+
- Maven 3.6+
- 已启动的授权服务器(默认端口9000)

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
java -jar target/api-provider-demo-0.0.1-SNAPSHOT.jar
```

服务将在端口8090上启动。

## API端点

项目提供以下API端点：

- **GET /api/message**：返回一个受保护的消息资源，需要有效的OAuth2令牌

## 安全配置

资源服务器配置为：

- 仅对`/api/**`路径下的资源进行访问控制
- 要求请求中必须包含有效的JWT令牌
- 使用无状态（stateless）会话管理
- 禁用CSRF保护，适合REST API

## 配置说明

主要配置文件位于`src/main/resources/application.yml`：

```yaml
server:
  port: 8090  # 项目端口

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000  # 授权服务器地址
```

## 客户端凭证流程

API提供者支持客户端凭证授权流程：

1. 客户端使用预配置的凭证从授权服务器获取JWT令牌
2. 客户端在请求API时在Authorization头中包含该令牌
3. 资源服务器验证JWT令牌的有效性和权限
4. 验证通过后，资源服务器处理请求并返回数据

## 测试API

您可以使用curl命令测试API端点：

```bash
# 首先从授权服务器获取访问令牌
curl -X POST -u "messaging-client:secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"

# 使用获取的令牌访问API
curl -X GET "http://localhost:8090/api/message" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 项目结构

```
api-provider-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_provider_demo/
│   │   │               ├── ApiProviderDemoApplication.java    # 应用程序入口
│   │   │               ├── config/
│   │   │               │   └── SecurityConfig.java            # 安全配置
│   │   │               └── controller/
│   │   │                   └── ApiController.java             # API控制器
│   │   └── resources/
│   │       └── application.yml                                # 应用配置
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java                           # 测试类
└── pom.xml                                                    # Maven配置
```

## JWT令牌验证

资源服务器会验证JWT令牌的以下信息：

- 颁发者(issuer)是否是配置的授权服务器
- 令牌是否在有效期内
- 令牌是否包含必要的权限范围(scope)
- 令牌签名是否有效

## 注意事项

- 确保授权服务器已启动并可访问
- 在真实环境中使用HTTPS保护API通信
- 适当配置CORS以支持来自不同源的客户端请求

## 许可证

本项目采用MIT许可证 