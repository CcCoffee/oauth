package com.example.api_auth_server.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class CustomJdbcRegisteredClientRepository extends JdbcRegisteredClientRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public CustomJdbcRegisteredClientRepository(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        super(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
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
                String.join(",", scopes)
            );
        }
    }

    public List<RegisteredClient> findByResourceId(String resourceId) {
        String sql = "SELECT id, client_id, client_id_issued_at, '***' as client_secret, client_secret_expires_at, client_name, client_authentication_methods," +
                " authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings" +
                " FROM oauth2_registered_client WHERE client_settings::jsonb->>'resource.id' = ?";
        return jdbcTemplate.query(sql, new RegisteredClientRowMapper(), resourceId);
    }

    public List<RegisteredClient> findAll() {
        String sql = "SELECT id, client_id, client_id_issued_at, '***' as client_secret, client_secret_expires_at, client_name, client_authentication_methods," +
                " authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings" +
                " FROM oauth2_registered_client";
        return jdbcTemplate.query(sql, new RegisteredClientRowMapper());
    }
} 