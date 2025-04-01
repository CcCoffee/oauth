# OAuth2授权服务架构示例

这是一个基于Spring Boot和Spring Security的OAuth2示例项目，展示了完整的OAuth2授权服务架构，包含授权服务器和资源服务器。

## 项目结构

本项目包含以下两个主要组件：

- **[api-auth-server](api-auth-server/README.md)**: OAuth2授权服务器，负责身份验证和颁发访问令牌
- **[api-provider-demo](api-provider-demo/README.md)**: OAuth2资源服务器，提供受保护的API资源

## 技术栈

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Authorization Server
- Spring OAuth2 Resource Server

## 快速开始

### 先决条件

- JDK 21+
- Maven 3.6+

### 启动授权服务器

```bash
cd api-auth-server
mvn spring-boot:run
```

授权服务器将在端口9000上启动。

### 启动资源服务器

```bash
cd api-provider-demo
mvn spring-boot:run
```

资源服务器将在端口8090上启动。

## 测试OAuth2流程

项目中包含一个Shell脚本，可用于测试OAuth2流程：

```bash
cd api-provider-demo/src/test/shell
chmod +x oauth-test.sh
./oauth-test.sh
```

这个脚本会执行以下操作：
- 测试客户端凭证授权流程
- 提供授权码授权流程的操作说明

## OAuth2授权流程

### 客户端凭证授权流程

适用于服务器到服务器的通信：

1. 客户端使用ID和密钥获取访问令牌
2. 使用访问令牌访问受保护的资源

### 授权码授权流程

适用于需要用户参与授权的场景：

1. 用户被重定向到授权服务器并进行身份验证
2. 用户授权应用程序访问其资源
3. 授权服务器将授权码发送到客户端的重定向URI
4. 客户端使用授权码交换访问令牌
5. 客户端使用访问令牌访问受保护的资源

## 预配置客户端和用户

授权服务器已预配置以下测试账户：

```
客户端ID: messaging-client
客户端密钥: secret

用户名: user
密码: password
```

## 服务器端点

### 授权服务器 (端口9000)

- **授权端点**: `/oauth2/authorize`
- **令牌端点**: `/oauth2/token`
- **JWT密钥集端点**: `/oauth2/jwks`

### 资源服务器 (端口8090)

- **示例API端点**: `/api/message`

## 许可证

本项目采用MIT许可证 