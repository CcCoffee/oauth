# API提供者演示项目

这是一个基于Spring Boot的OAuth2资源服务器演示项目，演示如何创建受保护的API端点，要求客户端必须提供有效的OAuth2令牌才能访问。

## 项目架构

本项目是OAuth2授权架构中的资源服务器组件，与授权服务器配合使用：

- **授权服务器(auth-server)**：负责颁发和验证OAuth2令牌
- **API提供者(api-provider-demo)**：本项目，提供受OAuth2保护的API资源

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

## 测试

项目包含用于测试OAuth2流程的Shell脚本，位于`src/test/shell/oauth-test.sh`。

### 使用测试脚本

1. 确保授权服务器和API提供者已启动
2. 安装jq工具（用于JSON解析）：`brew install jq`（Mac）或`apt-get install jq`（Ubuntu/Debian）
3. 执行测试脚本：

```bash
cd src/test/shell
chmod +x oauth-test.sh
./oauth-test.sh
```

这个脚本将自动执行以下操作：
- 测试客户端凭证授权流程
- 提供授权码流程的操作指南

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
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── api_provider_demo/
│       │               └── AppTest.java                       # 测试类
│       └── shell/
│           └── oauth-test.sh                                  # OAuth2测试脚本
└── pom.xml                                                    # Maven配置
```

## OAuth2流程说明

### 客户端凭证授权流程

适用于服务器到服务器的API访问：

1. 客户端使用客户端ID和密钥请求访问令牌：
   ```bash
   curl -X POST -u "messaging-client:secret" \
     "http://localhost:9000/oauth2/token" \
     -d "grant_type=client_credentials&scope=message.read" \
     -H "Content-Type: application/x-www-form-urlencoded"
   ```

2. 使用获取的令牌访问API：
   ```bash
   curl -X GET "http://localhost:8090/api/message" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
   ```

### 授权码授权流程

适用于用户授权的场景：

1. 客户端将用户重定向到授权服务器进行登录和授权：
   ```
   http://localhost:9000/oauth2/authorize?response_type=code&client_id=messaging-client&scope=message.read&redirect_uri=http://127.0.0.1:8080/authorized
   ```

2. 用户授权后，使用授权码交换访问令牌：
   ```bash
   curl -X POST -u "messaging-client:secret" \
     "http://localhost:9000/oauth2/token" \
     -d "grant_type=authorization_code&code=YOUR_AUTHORIZATION_CODE&redirect_uri=http://127.0.0.1:8080/authorized" \
     -H "Content-Type: application/x-www-form-urlencoded"
   ```

## 贡献指南

欢迎提交问题和改进建议！

## 许可证

本项目采用MIT许可证 