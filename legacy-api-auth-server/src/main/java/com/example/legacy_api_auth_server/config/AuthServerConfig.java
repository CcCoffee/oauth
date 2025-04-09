package com.example.legacy_api_auth_server.config;

import com.example.legacy_api_auth_server.service.EnhancedJdbcTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.sql.DataSource;
import java.security.KeyPair;
import java.util.Arrays;

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
        
        // Load key pair from keystore
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
                new ClassPathResource(keystorePath), 
                keystorePassword.toCharArray()
        );
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(keyAlias, keyPassword.toCharArray());
        
        // Set key pair to converter
        converter.setKeyPair(keyPair);
        
        return converter;
    }

    @Bean
    public TokenStore jdbcTokenStore() {
        return new EnhancedJdbcTokenStore(dataSource);
    }
    
    @Bean
    public TokenStore jwtTokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }
    
    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(jdbcTokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        return defaultTokenServices;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
            .tokenKeyAccess("permitAll()")      // Allow access to /oauth/token_key endpoint
            .checkTokenAccess("isAuthenticated()") // Allow authenticated clients to access /oauth/check_token endpoint
            .allowFormAuthenticationForClients(); // Allow client form authentication
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // Configure using JDBC, read client information from database
        clients.jdbc(dataSource)
               .passwordEncoder(passwordEncoder);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        // Configure token enhancer chain, support JWT format
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(accessTokenConverter()));
        
        endpoints
            .authenticationManager(authenticationManager)
            .tokenStore(jdbcTokenStore())  // Use JDBC token store
            .tokenEnhancer(tokenEnhancerChain)
            .accessTokenConverter(accessTokenConverter());
    }
} 