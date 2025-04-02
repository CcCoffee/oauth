# OAuth2不透明令牌的内省流程

## 系统组件

整个系统由三个主要组件组成：

1. **授权服务器 (api-auth-server)**：负责颁发和验证OAuth2令牌
2. **API提供者 (api-provider-demo)**：作为资源服务器，提供受OAuth2保护的API资源
3. **API消费者 (api-consumer-demo)**：作为客户端，使用客户端凭证访问API

## 不透明令牌内省流程

### 1. 令牌获取阶段

1. API消费者（opaque-client）向授权服务器发送请求，使用客户端凭证获取访问令牌：
   ```
   客户端ID: opaque-client
   客户端密钥: opaque-secret
   授权类型: client_credentials
   作用域: message.read
   ```

2. 授权服务器验证客户端凭证后，颁发不透明令牌（opaque token）给API消费者

### 2. 资源访问阶段

3. API消费者在请求头中携带不透明令牌，访问API提供者的受保护资源：
   ```
   GET /api/opaque/message
   Authorization: Bearer {opaque_token}
   ```

### 3. 令牌验证（内省）阶段

4. API提供者接收到请求后，需要验证不透明令牌的有效性，此时使用令牌内省机制

5. API提供者使用自身的客户端凭证向授权服务器的内省端点发送请求：
   ```
   客户端ID: resource-server
   客户端密钥: secret
   内省端点: http://localhost:9000/oauth2/introspect
   待验证令牌: {opaque_token}
   ```

6. 授权服务器验证API提供者的凭证，并检查令牌的有效性，返回令牌信息：
   ```json
   {
     "active": true,
     "client_id": "opaque-client",
     "scope": "message.read",
     "exp": 1234567890,
     ...
   }
   ```

7. API提供者根据返回结果判断令牌是否有效，以及令牌包含的权限范围，决定是否允许访问资源

### 4. 资源响应阶段

8. 如果令牌有效且具有足够权限，API提供者返回请求的资源信息
9. 如果令牌无效或权限不足，返回相应的错误信息

## 配置说明

### API提供者内省配置

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: http://localhost:9000/oauth2/introspect
          client-id: resource-server
          client-secret: secret
```

### 安全最佳实践

1. **职责分离**：使用专门的客户端（resource-server）进行令牌内省，而不是使用获取令牌的客户端（opaque-client）
2. **最小权限原则**：令牌内省客户端只被授予内省权限，不具备其他操作权限
3. **令牌有效期管理**：不透明令牌设置合理的有效期限
4. **安全通信**：生产环境中所有通信应使用HTTPS加密
5. **客户端凭证保护**：妥善保管所有客户端凭证，定期更换凭证 