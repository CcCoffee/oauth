package com.example.api_auth_server.controller;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenKeyController {
    
    @Value("${app.jwt.keystore.path}")
    private String keystorePath;
    
    @Value("${app.jwt.keystore.password}")
    private String keystorePassword;
    
    @Value("${app.jwt.keystore.key-alias}")
    private String keyAlias;
    
    /**
     * 提供公钥信息的接口，兼容spring-cloud-starter-oauth2
     * 返回PEM格式的公钥
     * @return 包含公钥的Map
     */
    @GetMapping("/oauth/token_key")
    public Map<String, String> getTokenKey() {
        try {
            // 从类路径加载密钥库文件
            ClassPathResource resource = new ClassPathResource(keystorePath);
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(resource.getInputStream(), keystorePassword.toCharArray());
            
            // 获取证书
            Certificate cert = keyStore.getCertificate(keyAlias);
            RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
            
            // 转换为PEM格式的公钥
            byte[] encoded = publicKey.getEncoded();
            String publicKeyPEM = 
                    "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getEncoder().encodeToString(encoded) +
                    "\n-----END PUBLIC KEY-----";
            
            // 构建响应Map
            Map<String, String> result = new HashMap<>();
            result.put("alg", "SHA256withRSA");
            result.put("value", publicKeyPEM);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("获取公钥失败: " + e.getMessage(), e);
        }
    }
} 