package com.example.api_auth_server.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;

/**
 * OAuth2数据迁移工具
 * 用于将旧版OAuth2数据（oauth_client_details, oauth_access_token等）
 * 迁移到新版OAuth2数据（oauth2_registered_client, oauth2_authorization等）
 */
@Component
public class OAuth2MigrationTool {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2MigrationTool.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RegisteredClientRepository registeredClientRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 迁移客户端详情数据
     * 从oauth_client_details表迁移到oauth2_registered_client表
     */
    @Transactional
    public void migrateClientDetails() {
        logger.info("开始迁移OAuth2客户端数据...");
        
        List<Map<String, Object>> clients = jdbcTemplate.queryForList("SELECT * FROM oauth_client_details");
        int successCount = 0;
        int failCount = 0;
        
        for (Map<String, Object> client : clients) {
            try {
                String clientId = (String) client.get("client_id");
                logger.info("正在迁移客户端: {}", clientId);
                
                // 检查客户端是否已经存在
                RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
                if (existingClient != null) {
                    logger.info("客户端 {} 已存在于新表中，跳过", clientId);
                    continue;
                }
                
                // 获取客户端密钥
                String clientSecret = (String) client.get("client_secret");
                
                // 处理授权类型
                Set<AuthorizationGrantType> grantTypes = convertGrantTypesToSet((String) client.get("authorized_grant_types"));
                
                // 处理重定向URI
                Set<String> redirectUris = convertToSet((String) client.get("web_server_redirect_uri"));
                
                // 处理作用域
                Set<String> scopes = convertToSet((String) client.get("scope"));
                
                // 处理资源ID
                String resourceIds = (String) client.get("resource_ids");

                // 处理额外信息
                String additionalInfo = (String) client.get("additional_information");
                
                // 创建TokenSettings
                TokenSettings tokenSettings = buildTokenSettings(client);
                
                // 创建ClientSettings
                ClientSettings clientSettings = buildClientSettings(resourceIds, additionalInfo);
                
                // 创建RegisteredClient
                RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientIdIssuedAt(java.time.Instant.now());
                
                // 设置客户端密钥（如果有）
                if (clientSecret != null && !clientSecret.isEmpty()) {
                    clientBuilder.clientSecret(clientSecret);
                }
                
                // 设置客户端名称
                clientBuilder.clientName(clientId);
                
                // 设置认证方式
                clientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                             .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                
                // 设置授权类型
                for (AuthorizationGrantType grantType : grantTypes) {
                    clientBuilder.authorizationGrantType(grantType);
                }
                
                // 设置重定向URI
                for (String uri : redirectUris) {
                    clientBuilder.redirectUri(uri);
                }
                
                // 设置作用域
                for (String scope : scopes) {
                    clientBuilder.scope(scope);
                }
                
                // 设置令牌设置和客户端设置
                clientBuilder.tokenSettings(tokenSettings)
                            .clientSettings(clientSettings);
                
                // 创建RegisteredClient实例
                RegisteredClient registeredClient = clientBuilder.build();
                
                // 保存到仓库
                registeredClientRepository.save(registeredClient);
                
                successCount++;
                logger.info("成功迁移客户端: {}", clientId);
            } catch (Exception e) {
                failCount++;
                logger.error("迁移客户端失败: {}", client.get("client_id"), e);
            }
        }
        
        logger.info("客户端数据迁移完成。成功: {}, 失败: {}", successCount, failCount);
    }
    
    /**
     * 将字符串转换为Set集合
     */
    private Set<String> convertToSet(String commaSeparatedString) {
        Set<String> result = new HashSet<>();
        if (StringUtils.hasText(commaSeparatedString)) {
            String[] parts = commaSeparatedString.split(",");
            for (String part : parts) {
                if (StringUtils.hasText(part)) {
                    result.add(part.trim());
                }
            }
        }
        return result;
    }
    
    /**
     * 转换授权类型为Set集合
     */
    private Set<AuthorizationGrantType> convertGrantTypesToSet(String authorizedGrantTypes) {
        Set<AuthorizationGrantType> grantTypes = new HashSet<>();
        
        if (!StringUtils.hasText(authorizedGrantTypes)) {
            // 默认授权类型
            grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
            return grantTypes;
        }
        
        String[] types = authorizedGrantTypes.split(",");
        
        for (String type : types) {
            switch (type.trim()) {
                case "password":
                    // 新版OAuth2已不支持password模式，可以考虑替代方案或特殊处理
                    logger.warn("新版OAuth2不推荐使用password授权类型，请考虑替代方案");
                    break;
                case "authorization_code":
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    break;
                case "refresh_token":
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                    break;
                case "client_credentials":
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    break;
                case "implicit":
                    // 新版OAuth2不再推荐implicit模式
                    logger.warn("新版OAuth2不推荐使用implicit授权类型，请考虑替代方案");
                    break;
                default:
                    // 其他自定义授权类型
                    grantTypes.add(new AuthorizationGrantType(type.trim()));
            }
        }
        
        if (grantTypes.isEmpty()) {
            // 如果没有有效的授权类型，使用默认值
            grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
        }
        
        return grantTypes;
    }
    
    /**
     * 构建客户端设置
     */
    private ClientSettings buildClientSettings(String resourceIds, String additionalInfo) {
        ClientSettings.Builder builder = ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false);
        
        // 将resource_id保存到client settings中
        if (StringUtils.hasText(resourceIds)) {
            builder.setting("resource.id", resourceIds);
        }

        if (StringUtils.hasText(additionalInfo)) {
            try {
                Map additionalInfoMap = objectMapper.readValue(additionalInfo, Map.class);
                additionalInfoMap.forEach((k,v) -> {
                    builder.setting(k.toString(), v.toString());
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 构建令牌设置
     */
    private TokenSettings buildTokenSettings(Map<String, Object> client) {
        TokenSettings.Builder builder = TokenSettings.builder();
        
        // 访问令牌有效期（秒）
        Integer accessTokenValidity = (Integer) client.get("access_token_validity");
        if (accessTokenValidity != null) {
            builder.accessTokenTimeToLive(Duration.ofSeconds(accessTokenValidity));
        } else {
            throw new RuntimeException("accessTokenValidity not found");
        }

        // 刷新令牌有效期（秒）
//        Integer refreshTokenValidity = (Integer) client.get("refresh_token_validity");
//        if (refreshTokenValidity != null) {
//            builder.refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValidity));
//        } else {
//            builder.refreshTokenTimeToLive(Duration.ofDays(30)); // 默认30天
//        }
        
        // 令牌格式 - 使用 SELF_CONTAINED（JWT）或 REFERENCE (UUID)
        String additionalInfo = (String) client.get("additional_information");
        if (StringUtils.hasText(additionalInfo)) {
            try {
                Map additionalInfoMap = objectMapper.readValue(additionalInfo, Map.class);
                Object profileType = additionalInfoMap.getOrDefault("type", "none"); // external, internal, none
                if ("external".equalsIgnoreCase(profileType.toString()) || "internal".equalsIgnoreCase(profileType.toString())) {
                    builder.accessTokenFormat(OAuth2TokenFormat.REFERENCE);
                } else {
                    builder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 迁移访问令牌数据
     * 从oauth_access_token表迁移到oauth2_authorization表
     * 注：由于旧版令牌通常是序列化对象，实际迁移较为复杂，可能需要专门的反序列化逻辑
     */
    @Transactional
    public void migrateAccessTokens() {
        logger.info("开始迁移OAuth2令牌数据...");
        logger.warn("注意：令牌迁移涉及序列化对象转换，可能不完全兼容，建议只迁移关键令牌或在迁移后重新获取令牌");
        
        // 由于令牌迁移复杂性，这里仅提供一个示例框架
        // 实际实现需要根据具体序列化机制来反序列化token和authentication对象
        
        List<Map<String, Object>> tokens = jdbcTemplate.queryForList("SELECT * FROM oauth_access_token");
        
        logger.info("找到{}个令牌需要迁移", tokens.size());
        logger.info("由于令牌迁移的复杂性，建议在应用低峰期执行此操作或考虑让用户重新获取令牌");
        
        // 令牌迁移的具体实现...
    }
    
    /**
     * 执行完整迁移
     */
    @Transactional
    public void migrateAll() {
        migrateClientDetails();
        // 令牌迁移可选执行
        // migrateAccessTokens();
    }
} 