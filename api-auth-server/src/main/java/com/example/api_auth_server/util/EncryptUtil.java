package com.example.api_auth_server.util;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

@Component
public class EncryptUtil {
    private final JasyptEncryptor jasypt;

    public EncryptUtil(JasyptEncryptor jasypt) {
        this.jasypt = jasypt;
    }

    public String encrypt(String plain) {
        return jasypt.encrypt(plain);
    }

    public OAuth2Authorization encryptAccessToken(OAuth2Authorization auth) {
        OAuth2AccessToken token = auth.getAccessToken().getToken();
        String plain = token.getTokenValue();
        String cipher = jasypt.encrypt(plain);
        OAuth2AccessToken wrapped = new OAuth2AccessToken(token.getTokenType(), cipher,
            token.getIssuedAt(), token.getExpiresAt(), token.getScopes());
        return OAuth2Authorization.from(auth)
             .token(wrapped, metadata -> metadata.putAll(auth.getAccessToken().getMetadata()))
             .build();
    }

    public OAuth2Authorization decryptAccessToken(OAuth2Authorization auth) {
        if (auth.getAccessToken() == null) return auth;
        OAuth2AccessToken token = auth.getAccessToken().getToken();
        String cipher = token.getTokenValue();
        String plain = jasypt.decrypt(cipher);
        OAuth2AccessToken wrapped = new OAuth2AccessToken(token.getTokenType(), plain,
            token.getIssuedAt(), token.getExpiresAt(), token.getScopes());
        return OAuth2Authorization.from(auth)
             .token(wrapped, metadata -> metadata.putAll(auth.getAccessToken().getMetadata()))
             .build();
    }
}
