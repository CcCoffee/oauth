package com.example.api_auth_server.config;

import java.time.Duration;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.example.api_auth_server.service.KeyStoreJwkService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableWebSecurity
public class AuthServerConfig {

    private final KeyStoreJwkService keyStoreJwkService;

    public AuthServerConfig(KeyStoreJwkService keyStoreJwkService) {
        this.keyStoreJwkService = keyStoreJwkService;
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
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/oauth/token", "/oauth/token_key")
                        .permitAll()
                        .requestMatchers("/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/clients/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new StandardPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        
        return new InMemoryUserDetailsManager(adminUser);
    }

//    @Bean
//    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
//        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
//
//        try {
////            // 不透明令牌客户端
////            if (registeredClientRepository.findByClientId("opaque-client") == null) {
////                RegisteredClient opaqueClient = RegisteredClient.withId(UUID.randomUUID().toString())
////                        .clientId("opaque-client")
////                        .clientSecret(passwordEncoder.encode("opaque-secret"))
////                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
////                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
////                        .scope("message.read")
////                        .tokenSettings(TokenSettings.builder()
////                                .accessTokenTimeToLive(Duration.ofDays(365)) // 有效期 365 天
////                                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
////                                .build())
////                        .clientSettings(ClientSettings.builder()
////                                .setting("resource.id", "opaque-client-resource-id")
////                                .build())
////                        .build();
////                registeredClientRepository.save(opaqueClient);
////            }
////
////            // JWT令牌客户端
////            if (registeredClientRepository.findByClientId("jwt-client") == null) {
////                RegisteredClient jwtClient = RegisteredClient.withId(UUID.randomUUID().toString())
////                        .clientId("jwt-client")
////                        .clientSecret(passwordEncoder.encode("jwt-secret"))
////                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
////                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
////                        .scope("message.read")
////                        .tokenSettings(TokenSettings.builder()
////                                .accessTokenTimeToLive(Duration.ofHours(24)) // 有效期 24 小时
////                                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
////                                .build())
////                        .clientSettings(ClientSettings.builder()
////                                .setting("resource.id", "jwt-client-resource-id")
////                                .build())
////                        .build();
////                registeredClientRepository.save(jwtClient);
////            }
//
//            // 资源服务器客户端（用于令牌内省）
//            if (registeredClientRepository.findByClientId("resource-server") == null) {
//                RegisteredClient resourceServer = RegisteredClient.withId(UUID.randomUUID().toString())
//                        .clientId("resource-server")
//                        .clientName("resource-server")
//                        .clientSecret(passwordEncoder.encode("secret"))
//                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                        // 关于内省客户端的scope设置，这不是强制固定为"introspection"的，但这是一种广泛接受的最佳实践。
//                        // OAuth2规范中并没有严格规定内省客户端必须使用名为"introspection"的scope，这是Spring Security的约定用法。
//                        // 实际上，您可以修改这个scope名称，但需要确保授权服务器能够正确识别并授权该客户端执行令牌内省操作。
//                        .scope("introspection")
//                        .build();
//                registeredClientRepository.save(resourceServer);
//            }
//        } catch (Exception e) {
//            // 处理异常
//            e.printStackTrace();
//        }
//
//        return registeredClientRepository;
//    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // 使用KeyStoreJwkService从密钥库获取JWK
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
        // 设置JWT token的自定义器
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);

        UUIDAuth2TokenGenerator uuidAuth2TokenGenerator = new UUIDAuth2TokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            uuidAuth2TokenGenerator
        );
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }
} 