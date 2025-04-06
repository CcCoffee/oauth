# Legacy API Consumer Demo

这是一个使用旧版Spring Cloud OAuth2客户端的示例应用程序，用于演示如何与OAuth2保护的API进行交互。

## 技术栈

- Spring Boot 2.6.4
- Spring Security
- Spring Cloud OAuth2 (2.2.0.RELEASE)
- Java 11

## 功能特点

- 使用客户端凭据授权流程获取OAuth2令牌
- 使用JWT令牌访问受保护资源
- 提供RESTful接口演示与受保护资源的交互

## 接口说明

应用程序提供以下API端点：

- `GET /api/jwt`: 使用JWT令牌访问受保护API
- `GET /api/info`: 获取服务信息

## 配置说明

配置文件`application.yml`包含以下关键配置：

```yaml
oauth2:
  server:
    token-uri: http://localhost:9001/oauth/token  # OAuth2服务器令牌端点
  client:
    jwt:
      client-id: jwt-client         # JWT令牌客户端ID
      client-secret: jwt-secret     # JWT令牌客户端密钥

api:
  server:
    url: http://localhost:8091      # API服务器URL
```

## 运行说明

1. 确保已安装Java 11或更高版本
2. 确保OAuth2授权服务器已运行
3. 确保API提供者服务已运行
4. 使用以下命令构建并运行应用程序：

```bash
mvn clean package
java -jar target/legacy-api-consumer-demo-0.0.1-SNAPSHOT.jar
```

或者直接使用Maven运行：

```bash
mvn spring-boot:run
```

应用程序将在端口8081上启动。 