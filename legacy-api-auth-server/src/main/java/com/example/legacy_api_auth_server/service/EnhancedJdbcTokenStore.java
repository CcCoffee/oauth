package com.example.legacy_api_auth_server.service;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;
import java.util.Base64;

/**
 * 增强版的JdbcTokenStore，用于在保存token时打印序列化对象信息
 */
public class EnhancedJdbcTokenStore extends JdbcTokenStore {

    public EnhancedJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        // 在存储令牌前，打印序列化对象的信息
        try {
            byte[] serializedToken = SerializationUtils.serialize(token);
            byte[] serializedAuthentication = SerializationUtils.serialize(authentication);
            
            String tokenBase64 = Base64.getEncoder().encodeToString(serializedToken);
            String authenticationBase64 = Base64.getEncoder().encodeToString(serializedAuthentication);
            
            System.out.println("========== 新增 OAuth Access Token 信息 ==========");
            System.out.println("Token 类型: " + token.getClass().getName());
            System.out.println("Token 值: " + token.getValue());
            System.out.println("Token 过期时间: " + token.getExpiration());
            System.out.println("Token 序列化大小: " + serializedToken.length + " 字节");
            System.out.println("Token 序列化对象 (Base64): " + tokenBase64);
            
            System.out.println("\nAuthentication 类型: " + authentication.getClass().getName());
            System.out.println("Authentication 客户端ID: " + authentication.getOAuth2Request().getClientId());
            System.out.println("Authentication 作用域: " + authentication.getOAuth2Request().getScope());
            System.out.println("Authentication 序列化大小: " + serializedAuthentication.length + " 字节");
            System.out.println("Authentication 序列化对象 (Base64): " + authenticationBase64);
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("打印Token序列化对象时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 调用父类方法执行实际的存储操作
        super.storeAccessToken(token, authentication);
    }
} 