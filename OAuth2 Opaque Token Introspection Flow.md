# OAuth2 Opaque Token Introspection Flow

## System Components

The entire system consists of three main components:

1. **Authorization Server (api-auth-server)**: Responsible for issuing and validating OAuth2 tokens
2. **API Provider (api-provider-demo)**: Acts as a resource server, providing API resources protected by OAuth2
3. **API Consumer (api-consumer-demo)**: Acts as a client, accessing API resources using client credentials

## Opaque Token Introspection Flow

### 1. Token Acquisition Phase

1. The API consumer (opaque-client) sends a request to the authorization server to obtain an access token using client credentials:
   ```
   Client ID: opaque-client
   Client Secret: opaque-secret
   Grant Type: client_credentials
   Scope: message.read
   ```

2. The authorization server validates the client credentials and issues an opaque token to the API consumer

### 2. Resource Access Phase

3. The API consumer carries the opaque token in the request header to access the protected resources of the API provider:
   ```
   GET /api/opaque/message
   Authorization: Bearer {opaque_token}
   ```

### 3. Token Validation (Introspection) Phase

4. After receiving the request, the API provider needs to validate the opaque token's validity, using the token introspection mechanism

5. The API provider uses its own client credentials to send a request to the authorization server's introspection endpoint:
   ```
   Client ID: resource-server
   Client Secret: secret
   Introspection Endpoint: http://localhost:9000/oauth2/introspect
   Token to Validate: {opaque_token}
   ```

6. The authorization server validates the API provider's credentials and checks the token's validity, returning token information:
   ```json
   {
     "active": true,
     "client_id": "opaque-client",
     "scope": "message.read",
     "exp": 1234567890,
     ...
   }
   ```

7. The API provider decides whether to allow access to the resource based on the returned result, including the token's validity and the scope of permissions it contains

### 4. Resource Response Phase

8. If the token is valid and has sufficient permissions, the API provider returns the requested resource information
9. If the token is invalid or lacks sufficient permissions, it returns the corresponding error information

## Token Introspection Cache Mechanism

Spring Security has implemented a caching mechanism for token introspection results to optimize performance and reduce the load on the authorization server:

1. **Default Cache Implementation**: Spring Security uses an in-memory cache to cache tokens and their introspection results
2. **Cache Policy**:
   - The same token will only trigger one introspection request within the cache validity period
   - Each time the `/api/opaque/message` endpoint is accessed, it will first check if there is a valid introspection result in the cache
   - A new introspection request will only be sent to the authorization server if the cache is missed or the cache expires

3. **Cache Time**: By default, the cache time depends on the token's expiration time (exp declaration)
   - If the token includes an exp declaration, the cache will expire before the token expires
   - If the token does not include an exp declaration, the default cache time is 5 minutes

4. **Cache Refresh**: The cache will be refreshed in the following situations:
   - The token is used for the first time
   - The cache entry expires
   - The service restarts

Therefore, under normal circumstances, accessing the `/api/opaque/message` endpoint will not trigger a request to the authorization server's introspection endpoint every time, unless:
- It is the first time using this token
- The cache has expired
- The resource server restarts

This caching mechanism ensures both security (through periodic token validation) and performance (by reducing unnecessary network requests).

## Configuration Explanation

### API Provider Introspection Configuration

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

Note that the API provider's configuration **does not need to specify scope**, because:

1. Scope is configured for the client's permission range on the authorization server side, and does not need to be repeated in the resource server's introspection configuration
2. When the resource server makes an introspection request, it is only validating the validity of a token, not requesting new access permissions
3. The introspection process uses basic authentication (Basic Authentication) to send client credentials, then sends the token for validation
4. Spring Security's OAuth2 resource server module automatically handles this introspection process, without needing to explicitly specify scope in the configuration

While on the authorization server side, the scope for the introspection client is typically set to `introspection`:

```java
RegisteredClient resourceServer = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("resource-server")
        .clientSecret("{noop}secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope("introspection")  // Introspection permission
        .build();
```

This `introspection` scope is not a mandatory requirement of the OAuth2 specification, but a widely accepted best practice naming convention that can be customized according to project needs.

### Security Best Practices

1. **Role Separation**: Use a dedicated client (resource-server) for token introspection, rather than the client that obtained the token (opaque-client)
2. **Minimum Privilege Principle**: The token introspection client is only granted introspection permissions, without other operation permissions
3. **Token Validity Management**: Set a reasonable validity period for opaque tokens
4. **Secure Communication**: All communication in production environments should use HTTPS encryption
5. **Client Credential Protection**: Properly manage all client credentials, and change them periodically