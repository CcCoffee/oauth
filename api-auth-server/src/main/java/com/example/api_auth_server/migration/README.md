# OAuth2数据迁移工具

## 概述

此工具用于将旧版Spring OAuth2（spring-cloud-starter-oauth2）的数据库表迁移到新版Spring Authorization Server（spring-boot-starter-oauth2-authorization-server）。

主要迁移内容：
- 从 `oauth_client_details` 表迁移客户端数据到 `oauth2_registered_client` 表
- 令牌数据迁移（可选，由于序列化机制差异，可能需要重新获取令牌）

## 迁移方式

### 1. 启动参数触发

在应用启动时添加命令行参数：

```
--oauth2.migration.enabled=true
```

系统将在启动时自动执行迁移。

### 2. API接口触发

迁移工具提供了以下API接口：

- `POST /api/migration/clients` - 仅迁移客户端数据
- `POST /api/migration/all` - 执行完整迁移

这些接口需要管理员权限才能访问。

示例：

```bash
# 迁移客户端数据
curl -X POST -H "Authorization: Bearer {管理员令牌}" http://localhost:8080/api/migration/clients

# 执行完整迁移
curl -X POST -H "Authorization: Bearer {管理员令牌}" http://localhost:8080/api/migration/all
```

## 技术实现

### 客户端迁移实现

迁移工具使用Spring Authorization Server提供的`RegisteredClient` API直接创建客户端记录，确保数据格式完全兼容新框架要求。主要步骤：

1. 从旧表`oauth_client_details`中读取客户端数据
2. 使用`RegisteredClient.Builder`构建新客户端对象
3. 设置授权类型、作用域、重定向URI等属性
4. 通过`ClientSettings`和`TokenSettings`设置客户端配置和令牌属性
5. 使用`RegisteredClientRepository`保存客户端

### 客户端数据映射

| 旧表字段 (oauth_client_details) | 新表对应 | 处理方式 |
|------------------------------|---------|---------|
| client_id                    | RegisteredClient.clientId | 直接映射 |
| client_secret                | RegisteredClient.clientSecret | 直接映射，保留加密格式 |
| resource_ids                 | ClientSettings | 作为"resource.id"保存在客户端设置中 |
| scope                        | RegisteredClient.scopes | 转换为Set集合并映射 |
| authorized_grant_types       | RegisteredClient.authorizationGrantTypes | 转换为AuthorizationGrantType对象 |
| web_server_redirect_uri      | RegisteredClient.redirectUris | 转换为Set集合并映射 |
| access_token_validity        | TokenSettings | 通过TokenSettings.Builder设置令牌有效期 |
| refresh_token_validity       | TokenSettings | 通过TokenSettings.Builder设置刷新令牌有效期 |

### 授权类型兼容性

迁移工具会自动处理授权类型的兼容性转换：

- `authorization_code` → `AuthorizationGrantType.AUTHORIZATION_CODE`
- `refresh_token` → `AuthorizationGrantType.REFRESH_TOKEN`
- `client_credentials` → `AuthorizationGrantType.CLIENT_CREDENTIALS`
- `password` → 不支持，会记录警告
- `implicit` → 不支持，会记录警告
- 其他类型 → 创建自定义`AuthorizationGrantType`对象

### 注意事项

1. 如果旧表中使用了不兼容的授权类型（如password），迁移工具会记录警告并跳过不兼容的类型
2. 为确保安全，迁移工具默认禁用，需要显式启用
3. 令牌迁移因序列化机制差异可能无法完全兼容，建议在迁移后让用户重新获取令牌
4. 迁移过程会记录详细日志，可在日志中查看迁移进度和可能的错误
5. 令牌格式默认设置为`SELF_CONTAINED`（JWT格式），可在迁移后根据需要修改 