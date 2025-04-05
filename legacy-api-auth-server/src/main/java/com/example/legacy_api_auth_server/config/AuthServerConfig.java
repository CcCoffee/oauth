package com.example.legacy_api_auth_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.sql.DataSource;
import java.security.KeyPair;

@Configuration
@EnableAuthorizationServer
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Value("${app.jwt.keystore.path}")
    private String keystorePath;
    
    @Value("${app.jwt.keystore.password}")
    private String keystorePassword;
    
    @Value("${app.jwt.keystore.key-alias}")
    private String keyAlias;
    
    @Value("${app.jwt.keystore.key-password}")
    private String keyPassword;

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        
        // 从密钥库加载密钥对
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
                new ClassPathResource(keystorePath), 
                keystorePassword.toCharArray()
        );
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(keyAlias, keyPassword.toCharArray());
        
        // 设置密钥对到转换器
        converter.setKeyPair(keyPair);
        
        return converter;
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
            .tokenKeyAccess("permitAll()")      // 允许访问/oauth/token_key端点
            .checkTokenAccess("isAuthenticated()") // 允许已认证客户端访问/oauth/check_token端点
            .allowFormAuthenticationForClients(); // 允许客户端表单认证
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 使用内存中的客户端配置进行测试
        clients.inMemory()
            .withClient("opaque-client")
                .secret(passwordEncoder.encode("opaque-secret"))
                .authorizedGrantTypes("client_credentials")
                .scopes("message.read")
                .accessTokenValiditySeconds(3600)
                .resourceIds("legacy-api")
            .and()
            .withClient("jwt-client")
                .secret(passwordEncoder.encode("jwt-secret"))
                .authorizedGrantTypes("client_credentials")
                .scopes("message.read")
                .accessTokenValiditySeconds(3600)
                .resourceIds("legacy-api");

        // 注释掉JDBC配置，使用内存配置进行测试
        // clients.jdbc(dataSource);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
            .authenticationManager(authenticationManager)
            .tokenStore(tokenStore())
            .accessTokenConverter(accessTokenConverter());
    }
} 