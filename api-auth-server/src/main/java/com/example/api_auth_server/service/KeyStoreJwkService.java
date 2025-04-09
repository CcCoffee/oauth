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
            // Load keystore file from classpath
            ClassPathResource resource = new ClassPathResource(keystorePath);
            InputStream is = resource.getInputStream();
            
            // Explicitly specify keystore type as JKS
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, keystorePassword.toCharArray());
            
            // Get RSA key pair from keystore
            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(
                    keyAlias, keyPassword.toCharArray());
            RSAPublicKey publicKey = (RSAPublicKey) keyStore.getCertificate(keyAlias)
                    .getPublicKey();
            
            // Build RSA key
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyAlias)
                    .build();
            
            // Create JWK set
            JWKSet jwkSet = new JWKSet(rsaKey);
            
            return new ImmutableJWKSet<>(jwkSet);
        } catch (Exception e) {
            // Print more detailed error information
            e.printStackTrace();
            throw new RuntimeException("Failed to load JWK from keystore: " + e.getMessage(), e);
        }
    }
} 