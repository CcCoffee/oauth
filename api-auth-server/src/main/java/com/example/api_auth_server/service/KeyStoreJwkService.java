package com.example.api_auth_server.service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Service
public class KeyStoreJwkService {
    
    @Value("${app.jwt.keystore.path}")
    private String keystorePath;
    
    @Value("${app.jwt.keystore.password}")
    private String keystorePassword;
    
    @Value("${app.jwt.keystore.key-alias}")
    private String keyAlias;
    
    @Value("${app.jwt.keystore.key-password}")
    private String keyPassword;
    
    public JWKSource<SecurityContext> getJwkSource() {
        try {
            // 从类路径加载密钥库文件
            ClassPathResource resource = new ClassPathResource(keystorePath);
            InputStream is = resource.getInputStream();
            
            // 明确指定密钥库类型为JKS
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, keystorePassword.toCharArray());
            
            // 从密钥库获取RSA密钥对
            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(
                    keyAlias, keyPassword.toCharArray());
            RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(keyAlias)
                    .getPublicKey();
            
            // 构建RSA密钥
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyAlias)
                    .build();
            
            // 创建JWK集
            JWKSet jwkSet = new JWKSet(rsaKey);
            
            return new ImmutableJWKSet<>(jwkSet);
        } catch (Exception e) {
            // 打印更详细的错误信息
            e.printStackTrace();
            throw new RuntimeException("无法从密钥库加载JWK: " + e.getMessage(), e);
        }
    }
} 