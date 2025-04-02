# API消费者演示项目

这是一个基于Spring Boot的OAuth2客户端演示项目，展示如何使用客户端凭证授权流程来访问受保护的API资源。

## 项目架构

本项目是OAuth2授权架构中的客户端组件，与授权服务器和资源服务器配合使用：

- **授权服务器(api-auth-server)**：负责用户认证和颁发访问令牌
- **资源服务器(api-provider-demo)**：提供受OAuth2保护的API资源
- **客户端(api-consumer-demo)**：本项目，使用OAuth2访问受保护的API资源

### 技术栈

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring OAuth2 Client
- WebClient

## 快速开始

### 先决条件

- JDK 21+
- Maven 3.6+
- 已启动的授权服务器(默认端口9000)
- 已启动的资源服务器(默认端口8090)

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
java -jar target/api-consumer-demo-0.0.1-SNAPSHOT.jar
```

应用将在端口8080上启动。

## API端点

本项目提供以下REST API端点：

### OAuth2端点
1. **GET /api/opaque**
   - 使用不透明令牌访问受保护的资源
   - 自动处理令牌获取和API调用
   - 返回不透明令牌的属性信息

2. **GET /api/jwt**
   - 使用JWT令牌访问受保护的资源
   - 自动处理令牌获取和API调用
   - 返回JWT令牌的声明信息

### 其他端点
1. **GET /api/direct**
   - 直接使用不透明令牌访问受保护的资源
   - 演示手动令牌获取和API调用

2. **GET /api/info**
   - 返回服务基本信息
   - 不需要授权即可访问

## 项目结构

```
api-consumer-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_consumer_demo/
│   │   │               ├── ApiConsumerDemoApplication.java     # 应用程序入口
│   │   │               ├── config/
│   │   │               │   ├── SecurityConfig.java             # 安全配置
│   │   │               │   └── WebClientConfig.java            # WebClient配置
│   │   │               ├── controller/
│   │   │               │   └── ApiController.java              # API控制器
│   │   │               └── service/
│   │   │                   └── ApiService.java                 # API服务
│   │   └── resources/
│   │       └── application.yml                                 # 应用配置
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java                            # 测试类
│       └── shell/
│           └── api-test.sh                                     # API测试脚本
└── pom.xml                                                     # Maven配置
```

## 配置说明

主要配置文件位于`src/main/resources/application.yml`：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          opaque-client:                                # 不透明令牌客户端配置
            client-id: opaque-client
            client-secret: opaque-secret
            authorization-grant-type: client_credentials
            scope: message.read
            provider: spring
          jwt-client:                                   # JWT令牌客户端配置
            client-id: jwt-client
            client-secret: jwt-secret
            authorization-grant-type: client_credentials
            scope: message.read
            provider: spring
        provider:
          spring:
            token-uri: http://localhost:9000/oauth2/token
```

## 客户端凭证流程

本项目支持两种令牌类型的客户端凭证流程：

### 不透明令牌流程
1. 客户端使用`opaque-client`凭证请求不透明访问令牌
2. 授权服务器验证客户端并颁发不透明令牌
3. 客户端使用该令牌访问`/api/opaque/message`端点
4. 资源服务器通过令牌内省验证令牌

### JWT令牌流程
1. 客户端使用`jwt-client`凭证请求JWT访问令牌
2. 授权服务器验证客户端并颁发JWT令牌
3. 客户端使用该令牌访问`/api/jwt/message`端点
4. 资源服务器直接验证JWT令牌

## 测试方法

### 使用测试脚本

项目包含一个Shell脚本，可自动测试所有API端点：

```bash
cd src/test/shell
chmod +x api-test.sh
./api-test.sh
```

这个脚本会执行以下测试：
1. 测试API信息端点
2. 测试直接API访问端点
3. 测试OAuth2不透明令牌端点
4. 测试OAuth2 JWT令牌端点
5. 直接测试授权服务器令牌端点

### 使用curl命令

也可以手动使用curl命令测试API端点：

```bash
# 测试信息端点
curl -v http://localhost:8080/api/info

# 测试不透明令牌端点
curl -v http://localhost:8080/api/opaque

# 测试JWT令牌端点
curl -v http://localhost:8080/api/jwt

# 测试直接访问端点
curl -v http://localhost:8080/api/direct
```

## 预配置客户端

使用以下OAuth2客户端访问授权服务器：

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

## 注意事项

- 确保授权服务器和资源服务器已启动并可访问
- 在生产环境中使用HTTPS保护所有通信
- 客户端凭证应妥善保管，避免泄露
- 定期更新客户端密钥
- 使用适当的令牌类型：
  - 不透明令牌：需要中央验证的场景
  - JWT令牌：需要分布式验证的场景

## 许可证

本项目采用MIT许可证 