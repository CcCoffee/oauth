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

## 令牌内省缓存机制

Spring Security默认实现了令牌内省结果的缓存机制，以优化性能并减少对授权服务器的请求负担：

1. **默认缓存实现**：Spring Security使用基于内存的缓存，将令牌及其内省结果进行缓存
2. **缓存策略**：
   - 相同的令牌在缓存有效期内只会触发一次内省请求
   - 每次访问 `/api/opaque/message` 端点时，会先检查缓存中是否存在有效的内省结果
   - 只有在缓存未命中或缓存失效时，才会向授权服务器发送新的内省请求

3. **缓存时间**：默认情况下，缓存时间取决于令牌的过期时间（exp声明）
   - 如果令牌包含exp声明，缓存将在令牌过期前失效
   - 如果令牌没有exp声明，默认缓存时间为5分钟

4. **缓存刷新**：在以下情况下缓存会被刷新
   - 令牌第一次被使用
   - 缓存条目过期
   - 服务重启

因此，在正常情况下，访问 `/api/opaque/message` 端点并不会每次都触发对授权服务器内省端点的请求，除非：
- 是首次使用该令牌
- 缓存已过期
- 资源服务器重启

这种缓存机制既保证了安全性（通过定期验证令牌），又提高了性能（减少不必要的网络请求）。

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

注意，API提供者的配置中**不需要指定scope**，这是因为：

1. scope是在授权服务器端为客户端配置的权限范围，在资源服务器的内省配置中无需重复指定
2. 当资源服务器进行内省请求时，它只是在验证一个令牌的有效性，而不是请求新的访问权限
3. 内省过程是使用基本认证(Basic Authentication)发送客户端凭据，然后发送令牌进行验证
4. Spring Security的OAuth2资源服务器模块会自动处理这个内省过程，无需在配置中显式指定scope

而在授权服务器端，内省客户端的scope通常设置为`introspection`：

```java
RegisteredClient resourceServer = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("resource-server")
        .clientSecret("{noop}secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope("introspection")  // 内省权限
        .build();
```

这个`introspection` scope并非OAuth2规范强制要求的，而是一种广泛接受的最佳实践命名约定，可以根据项目需要进行自定义。

### 安全最佳实践

1. **职责分离**：使用专门的客户端（resource-server）进行令牌内省，而不是使用获取令牌的客户端（opaque-client）
2. **最小权限原则**：令牌内省客户端只被授予内省权限，不具备其他操作权限
3. **令牌有效期管理**：不透明令牌设置合理的有效期限
4. **安全通信**：生产环境中所有通信应使用HTTPS加密
5. **客户端凭证保护**：妥善保管所有客户端凭证，定期更换凭证 