package com.example.api_auth_server.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
    
    @Autowired
    private org.springframework.core.env.Environment environment;
    
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
        
        try {
            // 从遗留系统API获取Token数据
            List<Map<String, Object>> importedTokens = fetchTokenDataFromLegacySystem();
            
            if (importedTokens.isEmpty()) {
                logger.warn("没有找到可迁移的令牌数据");
                return;
            }
            
            logger.info("找到{}个令牌需要迁移", importedTokens.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> tokenData : importedTokens) {
                String clientId = (String) tokenData.get("client_id");
                if (clientId == null || clientId.isEmpty()) {
                    logger.warn("跳过迁移：客户端ID为空");
                    failCount++;
                    continue;
                }
                
                // 查找对应的RegisteredClient
                RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
                if (registeredClient == null) {
                    logger.warn("跳过迁移：找不到客户端 {}", clientId);
                    failCount++;
                    continue;
                }
                
                try {
                    // 从JSON解析出OAuth2Authentication对象
                    String authJson = (String) tokenData.get("authentication");
                    Map<String, Object> authenticationMap = objectMapper.readValue(authJson, Map.class);
                    
                    // 验证是否为client_credentials授权类型
                    if (authenticationMap.containsKey("oauth2Request")) {
                        Map<String, Object> oauth2Request = (Map<String, Object>) authenticationMap.get("oauth2Request");
                        if (oauth2Request.containsKey("grantType")) {
                            String grantType = (String) oauth2Request.get("grantType");
                            if (!"client_credentials".equals(grantType)) {
                                logger.info("跳过非client_credentials授权类型的令牌: {}", grantType);
                                continue;
                            }
                        }
                    }
                    
                    // 解析令牌值和授权作用域
                    String tokenJson = (String) tokenData.get("token");
                    Map<String, Object> tokenMap = objectMapper.readValue(tokenJson, Map.class);
                    
                    String tokenValue = (String) tokenMap.get("access_token");
                    Set<String> scopes = new HashSet<>();
                    if (tokenMap.containsKey("scope")) {
                        List<String> scopeList = Arrays.stream(tokenMap.get("scope").toString().split(",")).toList();
                        scopes.addAll(scopeList);
                    }
                    
                    // 解析过期时间
                    long expiresInSeconds = (Integer) tokenMap.get("expires_in");
                    
                    // 创建访问令牌
                    OAuth2AccessToken accessToken = new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            tokenValue,
                            Instant.now(),
                            Instant.now().plusSeconds(expiresInSeconds),
                            scopes
                    );
                    
                    // 创建身份验证主体
                    OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                            registeredClient,
                            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                            null
                    );
                    
                    // 创建授权对象
                    OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                            .principalName(clientId)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .authorizedScopes(scopes);
                    
                    // 设置Token属性
                    authorizationBuilder.token(
                            accessToken,
                            metadata -> {
                                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, new HashMap<String, Object>());
                                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
                            }
                    );
                    
                    // 保存授权对象
                    OAuth2Authorization authorization = authorizationBuilder.build();
                    jdbcTemplate.update(
                            "INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type, attributes, state, " +
                                    "authorized_scopes, access_token_value, access_token_issued_at, access_token_expires_at, " +
                                    "access_token_type, access_token_scopes, access_token_metadata) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            authorization.getId(),
                            authorization.getRegisteredClientId(),
                            authorization.getPrincipalName(),
                            authorization.getAuthorizationGrantType().getValue(),
                            writeMap(authorization.getAttributes()),
                            null,
                            writeSet(authorization.getAuthorizedScopes()),
                            tokenValue,
                            Timestamp.from(accessToken.getIssuedAt()),
                            Timestamp.from(accessToken.getExpiresAt()),
                            accessToken.getTokenType().getValue(),
                            writeSet(accessToken.getScopes()),
                            writeMap(authorization.getAccessToken().getMetadata())
                    );
                    
                    successCount++;
                    logger.info("成功迁移令牌: {}", tokenValue);
                } catch (Exception e) {
                    failCount++;
                    logger.error("迁移令牌失败: {}", tokenData.get("token_id"), e);
                }
            }
            
            logger.info("令牌迁移完成。成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            logger.error("令牌迁移过程发生错误", e);
            throw new RuntimeException("令牌迁移失败", e);
        }
    }
    
    /**
     * 从遗留系统获取Token数据
     */
    private List<Map<String, Object>> fetchTokenDataFromLegacySystem() {
        // 从配置获取遗留系统URL
        String legacyServerUrl = environment.getProperty("oauth2.migration.legacy-server.url", "http://localhost:8080");
        String legacyApiUrl = legacyServerUrl + "/api/admin/token/export-csv";
        
        logger.info("正在从遗留系统获取Token数据: {}", legacyApiUrl);
        
        // 创建带有Basic认证的RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        
        // 添加Basic认证头
        HttpHeaders headers = new HttpHeaders();
        String username = environment.getProperty("oauth2.migration.legacy-server.username", "admin");
        String password = environment.getProperty("oauth2.migration.legacy-server.password", "admin123");
        
        // 创建Base64编码的认证信息
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    legacyApiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("获取Token数据失败，状态码: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
            String csvContent = response.getBody();
            if (csvContent == null || csvContent.isEmpty()) {
                logger.warn("获取的CSV内容为空");
                return Collections.emptyList();
            }
            
            // 解析CSV数据
            List<Map<String, Object>> tokens = new ArrayList<>();
            
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> iterator = csvMapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(csvContent);
            
            while (iterator.hasNext()) {
                tokens.add(new HashMap<>(iterator.next()));
            }
            
            logger.info("成功从遗留系统获取{}个Token数据", tokens.size());
            return tokens;
            
        } catch (Exception e) {
            logger.error("从遗留系统获取Token数据失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 将Map对象序列化为JSON字符串
     */
    private String writeMap(Map<String, Object> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }
    
    /**
     * 将Set对象序列化为JSON字符串
     */
    private String writeSet(Set<String> set) throws JsonProcessingException {
        return objectMapper.writeValueAsString(set);
    }
    
    /**
     * 执行完整迁移
     */
    @Transactional
    public void migrateAll() {
        migrateClientDetails();
        // 执行令牌迁移
        migrateAccessTokens();
    }
} 