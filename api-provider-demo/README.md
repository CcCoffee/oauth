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

### 不透明令牌端点
- **GET /api/opaque/message**：使用不透明令牌保护的API端点
- 需要有效的不透明访问令牌
- 返回令牌属性信息和消息

### JWT令牌端点
- **GET /api/jwt/message**：使用JWT令牌保护的API端点
- 需要有效的JWT访问令牌
- 返回JWT声明信息和消息

## 安全配置

资源服务器配置了两种安全机制：

### 不透明令牌配置
```java
.securityMatcher("/api/opaque/**")
.oauth2ResourceServer(oauth2 -> oauth2
    .opaqueToken(opaque -> opaque
        .introspectionUri("http://localhost:9000/oauth2/introspect")
        .introspectionClientCredentials("resource-server", "secret"))
)
```

### JWT令牌配置
```java
.securityMatcher("/api/jwt/**")
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(Customizer.withDefaults())
)
```

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

API提供者支持两种令牌验证流程：

### 不透明令牌流程
1. 客户端携带不透明令牌访问API
2. 资源服务器使用`resource-server`凭证调用内省端点
3. 授权服务器验证令牌并返回令牌信息
4. 资源服务器验证权限并处理请求

### JWT令牌流程
1. 客户端携带JWT令牌访问API
2. 资源服务器使用公钥验证JWT签名
3. 资源服务器验证JWT声明（过期时间、作用域等）
4. 验证通过后处理请求

## 测试API

您可以使用curl命令测试API端点：

### 测试不透明令牌端点
```bash
# 首先获取不透明访问令牌
curl -X POST -u "opaque-client:opaque-secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"

# 使用获取的令牌访问API
curl -X GET "http://localhost:8090/api/opaque/message" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 测试JWT令牌端点
```bash
# 首先获取JWT访问令牌
curl -X POST -u "jwt-client:jwt-secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"

# 使用获取的令牌访问API
curl -X GET "http://localhost:8090/api/jwt/message" \
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

## 令牌验证

资源服务器会验证令牌的以下信息：

### 不透明令牌验证
- 令牌是否处于活动状态（active）
- 令牌是否在有效期内
- 令牌是否具有所需的作用域
- 令牌的客户端ID是否正确

### JWT令牌验证
- 令牌签名是否有效
- 颁发者(issuer)是否正确
- 令牌是否在有效期内
- 令牌是否包含必要的作用域(scope)

## 注意事项

- 确保授权服务器已启动并可访问
- 在生产环境中使用HTTPS保护API通信
- 适当配置CORS以支持来自不同源的客户端请求
- 定期更新JWT签名密钥

## 许可证

本项目采用MIT许可证 