package com.example.legacy_api_auth_server.service;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;
import java.util.Base64;

/**
 * Enhanced version of JdbcTokenStore, used to print serialized object information when saving tokens
 */
public class EnhancedJdbcTokenStore extends JdbcTokenStore {

    public EnhancedJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        // Print serialized object information before storing the token
        try {
            byte[] serializedToken = SerializationUtils.serialize(token);
            byte[] serializedAuthentication = SerializationUtils.serialize(authentication);
            
            String tokenBase64 = Base64.getEncoder().encodeToString(serializedToken);
            String authenticationBase64 = Base64.getEncoder().encodeToString(serializedAuthentication);
            
            System.out.println("========== New OAuth Access Token Information ==========");
            System.out.println("Token Type: " + token.getClass().getName());
            System.out.println("Token Value: " + token.getValue());
            System.out.println("Token Expiration: " + token.getExpiration());
            System.out.println("Token Serialized Size: " + serializedToken.length + " bytes");
            System.out.println("Token Serialized Object (Base64): " + tokenBase64);
            
            System.out.println("\nAuthentication Type: " + authentication.getClass().getName());
            System.out.println("Authentication Client ID: " + authentication.getOAuth2Request().getClientId());
            System.out.println("Authentication Scope: " + authentication.getOAuth2Request().getScope());
            System.out.println("Authentication Serialized Size: " + serializedAuthentication.length + " bytes");
            System.out.println("Authentication Serialized Object (Base64): " + authenticationBase64);
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("Error occurred while printing token serialized object: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Call the parent class method to perform the actual storage operation
        super.storeAccessToken(token, authentication);
    }
} 