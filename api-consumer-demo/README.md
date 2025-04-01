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

1. **GET /api/test**
   - 使用客户端凭证授权流程访问受保护的资源
   - 返回JSON格式的API响应数据

2. **GET /api/direct**
   - 直接使用客户端凭证访问受保护的资源
   - 使用WebClient自动处理令牌获取和API调用

3. **GET /api/info**
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

主要配置文件位于`src/main/resources/application.yml`，其中定义了：

- OAuth2客户端注册信息
- 授权服务器连接信息
- 端口和日志级别等

## 客户端凭证流程

本项目使用客户端凭证流程实现服务器到服务器的API访问：

1. 客户端使用预配置的客户端ID和密钥向授权服务器请求访问令牌
2. 授权服务器验证客户端并颁发JWT访问令牌
3. 客户端使用获取的JWT令牌访问受保护的API资源
4. 资源服务器验证令牌并返回请求的数据

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
3. 测试OAuth2客户端凭证授权API端点
4. 直接测试授权服务器令牌端点

### 使用curl命令

也可以手动使用curl命令测试API端点：

```bash
# 测试信息端点
curl -v http://localhost:8080/api/info

# 测试客户端凭证授权
curl -v http://localhost:8080/api/test

# 测试直接API访问
curl -v http://localhost:8080/api/direct
```

## 授权服务器账户

使用以下测试账户登录授权服务器：

```
客户端ID: messaging-client
客户端密钥: secret
```

## 注意事项

- 确保授权服务器和资源服务器已启动并可访问
- 在真实环境中，您应该使用HTTPS保护所有通信
- 客户端凭证应妥善保管，避免泄露

## 许可证

本项目采用MIT许可证 