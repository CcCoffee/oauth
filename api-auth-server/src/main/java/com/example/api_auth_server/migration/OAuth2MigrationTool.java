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
 * OAuth2 Data Migration Tool
 * Used to migrate legacy OAuth2 data (oauth_client_details, oauth_access_token, etc.)
 * to new OAuth2 data (oauth2_registered_client, oauth2_authorization, etc.)
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
     * Migrate client details data
     * Migrate from oauth_client_details table to oauth2_registered_client table
     */
    @Transactional
    public void migrateClientDetails() {
        logger.info("Starting OAuth2 client data migration...");
        
        List<Map<String, Object>> clients = jdbcTemplate.queryForList("SELECT * FROM oauth_client_details");
        int successCount = 0;
        int failCount = 0;
        
        for (Map<String, Object> client : clients) {
            try {
                String clientId = (String) client.get("client_id");
                logger.info("Migrating client: {}", clientId);
                
                // Check if client already exists
                RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
                if (existingClient != null) {
                    logger.info("Client {} already exists in the new table, skipping", clientId);
                    continue;
                }
                
                // Get client secret
                String clientSecret = (String) client.get("client_secret");
                
                // Process grant types
                Set<AuthorizationGrantType> grantTypes = convertGrantTypesToSet((String) client.get("authorized_grant_types"));
                
                // Process redirect URIs
                Set<String> redirectUris = convertToSet((String) client.get("web_server_redirect_uri"));
                
                // Process scopes
                Set<String> scopes = convertToSet((String) client.get("scope"));
                
                // Process resource IDs
                String resourceIds = (String) client.get("resource_ids");

                // Process additional information
                String additionalInfo = (String) client.get("additional_information");
                
                // Create TokenSettings
                TokenSettings tokenSettings = buildTokenSettings(client);
                
                // Create ClientSettings
                ClientSettings clientSettings = buildClientSettings(resourceIds, additionalInfo);
                
                // Create RegisteredClient
                RegisteredClient.Builder clientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientIdIssuedAt(java.time.Instant.now());
                
                // Set client secret (if any)
                if (clientSecret != null && !clientSecret.isEmpty()) {
                    clientBuilder.clientSecret(clientSecret);
                }
                
                // Set client name
                clientBuilder.clientName(clientId);
                
                // Set authentication methods
                clientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                             .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                
                // Set grant types
                for (AuthorizationGrantType grantType : grantTypes) {
                    clientBuilder.authorizationGrantType(grantType);
                }
                
                // Set redirect URIs
                for (String uri : redirectUris) {
                    clientBuilder.redirectUri(uri);
                }
                
                // Set scopes
                for (String scope : scopes) {
                    clientBuilder.scope(scope);
                }
                
                // Set token settings and client settings
                clientBuilder.tokenSettings(tokenSettings)
                            .clientSettings(clientSettings);
                
                // Create RegisteredClient instance
                RegisteredClient registeredClient = clientBuilder.build();
                
                // Save to repository
                registeredClientRepository.save(registeredClient);
                
                successCount++;
                logger.info("Successfully migrated client: {}", clientId);
            } catch (Exception e) {
                failCount++;
                logger.error("Failed to migrate client: {}", client.get("client_id"), e);
            }
        }
        
        logger.info("Client data migration completed. Success: {}, Failures: {}", successCount, failCount);
    }
    
    /**
     * Convert string to Set
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
     * Convert grant types to Set
     */
    private Set<AuthorizationGrantType> convertGrantTypesToSet(String authorizedGrantTypes) {
        Set<AuthorizationGrantType> grantTypes = new HashSet<>();
        
        if (!StringUtils.hasText(authorizedGrantTypes)) {
            // Default grant type
            grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
            return grantTypes;
        }
        
        String[] types = authorizedGrantTypes.split(",");
        
        for (String type : types) {
            switch (type.trim()) {
                case "password":
                    // The password grant type is no longer supported in the new OAuth2, consider alternatives or special handling
                    logger.warn("New OAuth2 does not recommend using password authorization type, consider alternative solutions");
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
                    // New OAuth2 no longer recommends implicit mode
                    logger.warn("New OAuth2 does not recommend using implicit authorization type, consider alternative solutions");
                    break;
                default:
                    // Other custom authorization types
                    grantTypes.add(new AuthorizationGrantType(type.trim()));
            }
        }
        
        if (grantTypes.isEmpty()) {
            // If there are no valid authorization types, use the default value
            grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
        }
        
        return grantTypes;
    }
    
    /**
     * Build client settings
     */
    private ClientSettings buildClientSettings(String resourceIds, String additionalInfo) {
        ClientSettings.Builder builder = ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false);
        
        // Save resource_id to client settings
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
     * Build token settings
     */
    private TokenSettings buildTokenSettings(Map<String, Object> client) {
        TokenSettings.Builder builder = TokenSettings.builder();
        
        // Access token validity period (seconds)
        Integer accessTokenValidity = (Integer) client.get("access_token_validity");
        if (accessTokenValidity != null) {
            builder.accessTokenTimeToLive(Duration.ofSeconds(accessTokenValidity));
        } else {
            throw new RuntimeException("accessTokenValidity not found");
        }

        // Refresh token validity period (seconds)
//        Integer refreshTokenValidity = (Integer) client.get("refresh_token_validity");
//        if (refreshTokenValidity != null) {
//            builder.refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValidity));
//        } else {
//            builder.refreshTokenTimeToLive(Duration.ofDays(30)); // Default 30 days
//        }
        
        // Token format - Use SELF_CONTAINED (JWT) or REFERENCE (UUID)
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
     * Migrate access token data
     * Migrate from oauth_access_token table to oauth2_authorization table
     * Note: Since legacy tokens are usually serialized objects, actual migration is more complex, possibly requiring specialized deserialization logic
     */
    @Transactional
    public void migrateAccessTokens() {
        logger.info("Starting OAuth2 token data migration...");
        
        try {
            // Get Token data from legacy system API
            List<Map<String, Object>> importedTokens = fetchTokenDataFromLegacySystem();
            
            if (importedTokens.isEmpty()) {
                logger.warn("No token data found to migrate");
                return;
            }
            
            logger.info("Found {} tokens to migrate", importedTokens.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> tokenData : importedTokens) {
                String clientId = (String) tokenData.get("client_id");
                if (clientId == null || clientId.isEmpty()) {
                    logger.warn("Skipping migration: Client ID is empty");
                    failCount++;
                    continue;
                }
                
                // Find corresponding RegisteredClient
                RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
                if (registeredClient == null) {
                    logger.warn("Skipping migration: Client {} not found", clientId);
                    failCount++;
                    continue;
                }
                
                try {
                    // Parse OAuth2Authentication object from JSON
                    String authJson = (String) tokenData.get("authentication");
                    Map<String, Object> authenticationMap = objectMapper.readValue(authJson, Map.class);
                    
                    // Verify whether it's client_credentials authorization type
                    if (authenticationMap.containsKey("oauth2Request")) {
                        Map<String, Object> oauth2Request = (Map<String, Object>) authenticationMap.get("oauth2Request");
                        if (oauth2Request.containsKey("grantType")) {
                            String grantType = (String) oauth2Request.get("grantType");
                            if (!"client_credentials".equals(grantType)) {
                                logger.info("Skipping non-client_credentials authorization type token: {}", grantType);
                                continue;
                            }
                        }
                    }
                    
                    // Parse token value and authorization scope
                    String tokenJson = (String) tokenData.get("token");
                    Map<String, Object> tokenMap = objectMapper.readValue(tokenJson, Map.class);
                    
                    String tokenValue = (String) tokenMap.get("access_token");
                    Set<String> scopes = new HashSet<>();
                    if (tokenMap.containsKey("scope")) {
                        List<String> scopeList = Arrays.stream(tokenMap.get("scope").toString().split(",")).toList();
                        scopes.addAll(scopeList);
                    }
                    
                    // Parse expiration time
                    long expiresInSeconds = (Integer) tokenMap.get("expires_in");
                    
                    // Create access token
                    OAuth2AccessToken accessToken = new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            tokenValue,
                            Instant.now(),
                            Instant.now().plusSeconds(expiresInSeconds),
                            scopes
                    );
                    
                    // Create authentication principal
                    OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                            registeredClient,
                            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                            null
                    );
                    
                    // Create authorization object
                    OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                            .principalName(clientId)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .authorizedScopes(scopes);
                    
                    // Set Token attributes
                    authorizationBuilder.token(
                            accessToken,
                            metadata -> {
                                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, new HashMap<String, Object>());
                                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
                            }
                    );
                    
                    // Save authorization object
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
                    logger.info("Successfully migrated token: {}", tokenValue);
                } catch (Exception e) {
                    failCount++;
                    logger.error("Token migration failed: {}", tokenData.get("token_id"), e);
                }
            }
            
            logger.info("Token migration completed. Success: {}, Failures: {}", successCount, failCount);
            
        } catch (Exception e) {
            logger.error("Token migration process error", e);
            throw new RuntimeException("Token migration failed", e);
        }
    }
    
    /**
     * Get Token data from legacy system
     */
    private List<Map<String, Object>> fetchTokenDataFromLegacySystem() {
        // Get legacy system URL from configuration
        String legacyServerUrl = environment.getProperty("oauth2.migration.legacy-server.url", "http://localhost:8080");
        String legacyApiUrl = legacyServerUrl + "/api/admin/token/export-csv";
        
        logger.info("Getting Token data from legacy system: {}", legacyApiUrl);
        
        // Create RestTemplate with Basic authentication
        RestTemplate restTemplate = new RestTemplate();
        
        // Add Basic authentication header
        HttpHeaders headers = new HttpHeaders();
        String username = environment.getProperty("oauth2.migration.legacy-server.username", "admin");
        String password = environment.getProperty("oauth2.migration.legacy-server.password", "admin123");
        
        // Create Base64 encoded authentication information
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
                logger.error("Failed to get Token data, status code: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
            String csvContent = response.getBody();
            if (csvContent == null || csvContent.isEmpty()) {
                logger.warn("CSV content is empty");
                return Collections.emptyList();
            }
            
            // Parse CSV data
            List<Map<String, Object>> tokens = new ArrayList<>();
            
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> iterator = csvMapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(csvContent);
            
            while (iterator.hasNext()) {
                tokens.add(new HashMap<>(iterator.next()));
            }
            
            logger.info("Successfully got {} Token data from legacy system", tokens.size());
            return tokens;
            
        } catch (Exception e) {
            logger.error("Failed to get Token data from legacy system", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Convert Map object to JSON string
     */
    private String writeMap(Map<String, Object> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }
    
    /**
     * Convert Set object to JSON string
     */
    private String writeSet(Set<String> set) throws JsonProcessingException {
        return objectMapper.writeValueAsString(set);
    }
    
    /**
     * Execute full migration
     */
    @Transactional
    public void migrateAll() {
        migrateClientDetails();
        // Execute token migration
        migrateAccessTokens();
    }
} 