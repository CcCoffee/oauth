package com.example.api_auth_server.config;

import com.example.api_auth_server.service.KeyStoreJwkService;
import com.example.api_auth_server.util.EncryptUtil;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AuthServerConfig {

    private final KeyStoreJwkService keyStoreJwkService;
    private final AdminProperties adminProperties;

    public AuthServerConfig(KeyStoreJwkService keyStoreJwkService, AdminProperties adminProperties) {
        this.keyStoreJwkService = keyStoreJwkService;
        this.adminProperties = adminProperties;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .tokenIntrospectionEndpoint(Customizer.withDefaults())
            .oidc(Customizer.withDefaults());
        http.oauth2ResourceServer(resourceServer -> resourceServer
            .jwt(Customizer.withDefaults()));
        http.cors(Customizer.withDefaults());
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Legacy OAuth remove_token endpoint security configuration
     * 为legacy OAuth remove_token端点配置OAuth2客户端认证
     */
    @Bean
    @Order(2)
    public SecurityFilterChain legacyOAuthSecurityFilterChain(HttpSecurity http, 
                                                             @Lazy AuthenticationManager oAuth2ClientAuthenticationManager) throws Exception {
        http
            .securityMatcher("/oauth/remove_token")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth/remove_token").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .authenticationManager(oAuth2ClientAuthenticationManager)
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }

    /**
     * Migration API security configuration
     * 为migration API配置admin用户认证
     */
    @Bean
    @Order(3)
    public SecurityFilterChain migrationApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/admin/migration/**", "/api/migration/**", "/clients/**")
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/admin/migration/**").hasRole("ADMIN")
                .requestMatchers("/api/migration/**").hasRole("ADMIN")
                .requestMatchers("/clients/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .userDetailsService(adminUserDetailsService())
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }

    /**
     * Default security configuration for other endpoints
     * 其他端点的默认安全配置
     */
    @Bean
    @Order(4)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth/token_key", "/oauth/check_token", "/oauth/token", "/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }

    /**
     * Admin用户的UserDetailsService
     * 使用AdminProperties中的配置创建In-memory用户
     */
    @Bean
    public UserDetailsService adminUserDetailsService() {
        // 从AdminProperties获取admin用户信息
        UserDetails admin = User.builder()
                .username(adminProperties.getUsername())
                .password(passwordEncoder().encode(adminProperties.getPassword()))
                .roles("ADMIN")
                .build();
        
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * 用于Legacy OAuth端点的AuthenticationManager
     * 使用@Lazy注解避免循环依赖
     */
    @Bean
    public AuthenticationManager oAuth2ClientAuthenticationManager(@Lazy OAuth2ClientAuthenticationProvider oAuth2ClientAuthenticationProvider) {
        return new ProviderManager(oAuth2ClientAuthenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new StandardPasswordEncoder();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // Use KeyStoreJwkService to get JWK from the keystore
        return keyStoreJwkService.getJwkSource();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource, JwtTokenCustomizer jwtTokenCustomizer) {
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        // Set the customizer for the JWT token
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);

        UUIDAuth2TokenGenerator uuidAuth2TokenGenerator = new UUIDAuth2TokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            uuidAuth2TokenGenerator
        );
    }

//    @Bean
//    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
//            RegisteredClientRepository registeredClientRepository) {
//        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
//    }

    @Bean
    public OAuth2AuthorizationService customOAuth2AuthorizationService(
            JdbcOperations jdbcOps,
            RegisteredClientRepository repo,
            EncryptUtil eu) {
        var delegate = new JdbcOAuth2AuthorizationService(jdbcOps, repo);
        return new OAuth2AuthorizationService() {
            @Override
            public void save(OAuth2Authorization auth) {
                delegate.save(eu.encryptAccessToken(auth));
            }
            @Override
            public void remove(OAuth2Authorization auth) {
                delegate.remove(auth);
            }
            @Override
            public OAuth2Authorization findById(String id) {
                OAuth2Authorization auth = delegate.findById(id);
                return auth == null ? null : eu.decryptAccessToken(auth);
            }
            @Override
            public OAuth2Authorization findByToken(String token, OAuth2TokenType type) {
                OAuth2Authorization auth = delegate.findByToken(eu.encrypt(token), type);
                return auth == null ? null : eu.decryptAccessToken(auth);
            }
        };
    }

}