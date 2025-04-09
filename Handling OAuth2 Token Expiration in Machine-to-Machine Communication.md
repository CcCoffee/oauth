# Handling OAuth2 Token Expiration in Machine-to-Machine Communication

In OAuth2-based machine-to-machine (M2M) communication scenarios, the expiration of access tokens is a common challenge. This document introduces several common strategies and best practices for handling token expiration.

## Default Expiration Time for Client Credentials Tokens

In Spring Security OAuth2, the default expiration time for client credentials tokens is **5 minutes** (300 seconds).

As seen in the project configuration:
```
"settings.token.access-token-time-to-live":"PT300S"
```

Here, `PT300S` is in ISO 8601 duration format:
- `P` indicates this is a period
- `T` indicates the time unit follows
- `300S` indicates 300 seconds

## Strategies for Handling Token Expiration

### 1. Proactive Refresh Strategy

Proactively obtain a new token before the existing one expires, avoiding request failures due to expired tokens.

```java
// Implementing token validity management in client code
public class TokenManager {
    private String accessToken;
    private long expiresAt;
    private final ApiClient apiClient;
    
    public String getValidToken() {
        // If the token is about to expire (e.g., 30 seconds left), proactively get a new token
        if (isTokenExpiringSoon()) {
            refreshToken();
        }
        return accessToken;
    }
    
    private boolean isTokenExpiringSoon() {
        // Refresh 30 seconds ahead
        return System.currentTimeMillis() + 30000 > expiresAt;
    }
    
    private void refreshToken() {
        // Call the authorization server to get a new token
        TokenResponse response = apiClient.getClientCredentialsToken();
        accessToken = response.getAccessToken();
        // Set the expiration time to the current time + token validity period
        expiresAt = System.currentTimeMillis() + (response.getExpiresIn() * 1000);
    }
}
```

### 2. Error Retry Strategy

When API calls return errors due to token expiration, automatically obtain a new token and retry the request.

```java
public class ApiCaller {
    private final TokenService tokenService;
    private final RestTemplate restTemplate;
    
    public ApiResult callApi() {
        try {
            // Attempt to use the current token to call the API
            return callApiWithRetry();
        } catch (TokenExpiredException e) {
            // If the token has expired, get a new token and retry
            tokenService.refreshToken();
            return callApiWithRetry();
        }
    }
    
    private ApiResult callApiWithRetry() {
        // Set a maximum retry count to prevent infinite loops
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String token = tokenService.getToken();
                return makeApiCall(token);
            } catch (TokenExpiredException e) {
                if (i == maxRetries - 1) throw e;
                tokenService.refreshToken();
            }
        }
        throw new RuntimeException("Maximum retry count reached");
    }
}
```

### 3. Using Spring's Built-in Token Management

The Spring Security OAuth2 client library provides automatic token management capabilities:

```java
@Bean
public OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientRepository authorizedClientRepository) {
    
    OAuth2AuthorizedClientProvider authorizedClientProvider = 
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
    
    DefaultOAuth2AuthorizedClientManager authorizedClientManager = 
            new DefaultOAuth2AuthorizedClientManager(
                    clientRegistrationRepository, authorizedClientRepository);
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
    
    return authorizedClientManager;
}
```

Used in conjunction with WebClient:
```java
@Bean
public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth2Client.setDefaultClientRegistrationId("messaging-client-credentials");
    
    return WebClient.builder()
            .apply(oauth2Client.oauth2Configuration())
            .build();
}
```

Under this approach, Spring automatically handles token expiration, obtaining new tokens and retrying requests.

### 4. Increasing Token Validity Period

For machine-to-machine communication, it is possible to appropriately increase the token validity period, reducing the frequency of token refreshes:

```java
RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
    .clientId("messaging-client")
    .clientSecret("{noop}secret")
    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    .scope("message.read")
    .tokenSettings(TokenSettings.builder()
        // Set the access token validity period to 1 hour
        .accessTokenTimeToLive(Duration.ofHours(1))
        .build())
    .build();
```

### 5. Token Caching

In distributed systems, it is possible to use Redis or similar caching services to share and cache tokens:

```java
@Service
public class TokenCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ApiClient apiClient;
    
    private static final String TOKEN_KEY = "api:access_token";
    private static final String TOKEN_EXPIRY_KEY = "api:token_expiry";
    
    public String getToken() {
        // Check if there is a usable token in Redis
        String token = redisTemplate.opsForValue().get(TOKEN_KEY);
        String expiryStr = redisTemplate.opsForValue().get(TOKEN_EXPIRY_KEY);
        
        if (token == null || expiryStr == null || isExpired(expiryStr)) {
            // Get a new token
            TokenResponse response = apiClient.getClientCredentialsToken();
            token = response.getAccessToken();
            // Set a cache time slightly shorter than the actual expiration time to avoid edge cases
            long expiryInSeconds = response.getExpiresIn() - 60;
            redisTemplate.opsForValue().set(TOKEN_KEY, token, expiryInSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(TOKEN_EXPIRY_KEY, 
                    String.valueOf(System.currentTimeMillis() + (expiryInSeconds * 1000)), 
                    expiryInSeconds, TimeUnit.SECONDS);
        }
        
        return token;
    }
    
    private boolean isExpired(String expiryStr) {
        long expiry = Long.parseLong(expiryStr);
        return System.currentTimeMillis() > expiry;
    }
}
```

## Best Practices

1. **Balance Security and Convenience**: Short-term tokens are more secure, but require more frequent refreshes; long-term tokens are more convenient, but increase security risks
2. **Implement Error Handling**: Handle 401/403 responses, automatically attempting to refresh the token and retry
3. **Use Exponential Backoff Algorithm**: Increase wait time on failed retries to avoid overwhelming the server
4. **Monitor Token Usage**: Record token acquisition counts, failure rates, and other metrics to identify anomalies promptly
5. **Consider High Availability**: Token management components should be able to handle temporary unavailability of the authorization server
6. **Avoid Token Leakage**: Tokens should be transmitted through secure channels, avoiding storage in logs or insecure storage

## Choosing the Right Strategy

- **Small Applications**: Use Spring's built-in automatic token management functionality
- **Medium-Sized Applications**: Implement proactive refresh and error retry strategies
- **Large Distributed Applications**: Use token caching and high availability designs

In practical applications, it is common to select a combination of strategies suitable for the system scale and performance requirements, achieving efficient management of the token lifecycle. 