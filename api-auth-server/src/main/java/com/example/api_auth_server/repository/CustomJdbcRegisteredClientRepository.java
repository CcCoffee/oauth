package com.example.api_auth_server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CustomJdbcRegisteredClientRepository implements RegisteredClientRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final JdbcRegisteredClientRepository defaultRepository;

    public CustomJdbcRegisteredClientRepository(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.defaultRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @PostConstruct
    public void initClients() {
        // 资源服务器客户端（用于令牌内省）
        if (this.findByClientId("resource-server") == null) {
            RegisteredClient resourceServer = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("resource-server")
                    .clientName("resource-server")
                    .clientSecret(passwordEncoder.encode("secret"))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    // 关于内省客户端的scope设置，这不是强制固定为"introspection"的，但这是一种广泛接受的最佳实践。
                    // OAuth2规范中并没有严格规定内省客户端必须使用名为"introspection"的scope，这是Spring Security的约定用法。
                    // 实际上，您可以修改这个scope名称，但需要确保授权服务器能够正确识别并授权该客户端执行令牌内省操作。
                    .scope("introspection")
                    .build();
            this.save(resourceServer);
        }
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        defaultRepository.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        return defaultRepository.findById(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return defaultRepository.findByClientId(clientId);
    }

    @Transactional
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
    }

    @Transactional
    public void deleteByClientId(String clientId) {
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
    }

    @Transactional
    public void deleteTokensByClientId(String clientId, List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", clientId);
        } else {
            jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE registered_client_id = ? AND scope IN (?)",
                clientId,
                String.join(" ", scopes)
            );
        }
    }

    public List<RegisteredClient> findByResourceId(String resourceId) {
        String sql = "SELECT * FROM oauth2_registered_client WHERE client_settings::jsonb->>'settings.client.resource.id' = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                String id = rs.getString("id");
                String clientId = rs.getString("client_id");
                String clientSecret = rs.getString("client_secret");
                String clientName = rs.getString("client_name");
                String clientAuthenticationMethods = rs.getString("client_authentication_methods");
                String authorizationGrantTypes = rs.getString("authorization_grant_types");
                String redirectUris = rs.getString("redirect_uris");
                String scopes = rs.getString("scopes");
                String clientSettings = rs.getString("client_settings");
                String tokenSettings = rs.getString("token_settings");

                RegisteredClient.Builder builder = RegisteredClient.withId(id)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .clientName(clientName);

                // 设置认证方法
                List<String> authMethods = objectMapper.readValue(clientAuthenticationMethods, new TypeReference<List<String>>() {});
                authMethods.forEach(method -> {
                    switch (method) {
                        case "client_secret_basic" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                        case "client_secret_post" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                        case "client_secret_jwt" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                        case "private_key_jwt" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                        case "none" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                    }
                });

                // 设置授权类型
                List<String> grantTypes = objectMapper.readValue(authorizationGrantTypes, new TypeReference<List<String>>() {});
                grantTypes.forEach(type -> {
                    switch (type) {
                        case "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                        case "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                        case "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                        case "urn:ietf:params:oauth:grant-type:device_code" -> builder.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE);
                        case "urn:ietf:params:oauth:grant-type:jwt-bearer" -> builder.authorizationGrantType(AuthorizationGrantType.JWT_BEARER);
                    }
                });

                // 设置重定向URI
                List<String> redirectUriList = objectMapper.readValue(redirectUris, new TypeReference<List<String>>() {});
                redirectUriList.forEach(builder::redirectUri);

                // 设置scope
                List<String> scopeList = objectMapper.readValue(scopes, new TypeReference<List<String>>() {});
                scopeList.forEach(builder::scope);

                // 设置客户端设置
                Map<String, Object> clientSettingsMap = objectMapper.readValue(clientSettings, new TypeReference<Map<String, Object>>() {});
                builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

                // 设置令牌设置
                Map<String, Object> tokenSettingsMap = objectMapper.readValue(tokenSettings, new TypeReference<Map<String, Object>>() {});
                builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

                return builder.build();
            } catch (Exception e) {
                throw new RuntimeException("Error mapping RegisteredClient", e);
            }
        }, resourceId);
    }
} 