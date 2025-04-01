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

### 使用客户端凭证流程

访问以下端点测试客户端凭证授权流程：

```
http://localhost:8080/api/direct
```

或

```
http://localhost:8080/api/test
```

### 使用基础信息

查看基本服务信息：

```
http://localhost:8080/api/info
```

## 预配置账户信息

```
客户端ID: messaging-client
客户端密钥: secret

用户名: user
密码: password
```

## 服务端口

- 授权服务器: 9000
- 资源服务器: 8090
- 客户端: 8080

## 许可证

本项目采用MIT许可证 