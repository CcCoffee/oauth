# OAuth2授权服务架构示例

这是一个基于Spring Boot和Spring Security的OAuth2示例项目，展示了完整的OAuth2授权服务架构。

## 项目结构

本项目包含以下三个主要组件：

- **[api-auth-server](api-auth-server/README.md)**: OAuth2授权服务器，负责身份验证和颁发访问令牌
- **[api-provider-demo](api-provider-demo/README.md)**: OAuth2资源服务器，提供受保护的API资源
- **[api-consumer-demo](api-consumer-demo/README.md)**: OAuth2客户端，演示如何获取令牌并访问受保护资源

## 技术栈

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Authorization Server
- Spring OAuth2 Resource Server
- Spring OAuth2 Client

## 快速开始

### 先决条件

- JDK 21+
- Maven 3.6+

### 启动所有服务

1. 首先启动授权服务器：

```bash
cd api-auth-server
mvn spring-boot:run
```

授权服务器将在端口9000上启动。

2. 然后启动资源服务器：

```bash
cd api-provider-demo
mvn spring-boot:run
```

资源服务器将在端口8090上启动。

3. 最后启动客户端：

```bash
cd api-consumer-demo
mvn spring-boot:run
```

客户端将在端口8080上启动。

## 测试API访问

### 使用测试脚本

运行提供的测试脚本来验证所有功能：

```bash
cd api-consumer-demo/src/test/shell
./api-test.sh
```

这个脚本会测试：
1. API信息端点
2. 直接API访问
3. OAuth2不透明令牌端点
4. OAuth2 JWT令牌端点
5. 授权服务器令牌端点

### 手动测试端点

或者手动访问以下端点：

```
http://localhost:8080/api/opaque  # 使用不透明令牌访问
http://localhost:8080/api/jwt     # 使用JWT令牌访问
http://localhost:8080/api/info    # 查看服务信息
```

## 预配置客户端

系统预配置了以下OAuth2客户端：

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

## 服务端口

- 授权服务器: 9000
- 资源服务器: 8090
- 客户端: 8080

## 许可证

本项目采用MIT许可证 