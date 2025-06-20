package com.example.api_auth_server.config;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 自定义OAuth2客户端认证提供者
 * 用于验证客户端的Basic Authentication
 */
@Component
public class OAuth2ClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2ClientAuthenticationProvider(@Lazy RegisteredClientRepository registeredClientRepository, 
                                            @Lazy PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String clientId = authentication.getName();
        String clientSecret = (String) authentication.getCredentials();

        if (clientId == null || clientSecret == null) {
            throw new BadCredentialsException("Client credentials are required");
        }

        // 查找注册的客户端
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new BadCredentialsException("Invalid client credentials");
        }

        // 验证客户端密钥
        if (!passwordEncoder.matches(clientSecret, registeredClient.getClientSecret())) {
            throw new BadCredentialsException("Invalid client credentials");
        }

        // 创建认证成功的Authentication对象
        // 使用客户端ID作为principal，并添加ROLE_CLIENT权限
        return new UsernamePasswordAuthenticationToken(
            clientId, 
            null, // 不保存密码
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 