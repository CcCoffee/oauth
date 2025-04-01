package com.example.api_auth_server.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Service
public class JdbcJwkService {
    
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcJwkService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public JWKSource<SecurityContext> getJwkSource() {
        try {
            JwkData jwkData = getActiveJwk();
            RSAKey rsaKey = buildRsaKey(jwkData);
            return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (EmptyResultDataAccessException e) {
            // 没有找到活跃的JWK，创建新的
            return createAndSaveNewJwk();
        } catch (Exception e) {
            throw new RuntimeException("获取JWK失败", e);
        }
    }
    
    private JwkData getActiveJwk() {
        String sql = "SELECT id, key_id, public_key, private_key FROM oauth2_jwt_keys WHERE is_active = true LIMIT 1";
        return jdbcTemplate.queryForObject(sql, new JwkRowMapper());
    }
    
    private JWKSource<SecurityContext> createAndSaveNewJwk() {
        try {
            // 生成新的RSA密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            
            String keyId = UUID.randomUUID().toString();
            String id = UUID.randomUUID().toString();
            
            // 将密钥编码为Base64
            String publicKeyEncoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String privateKeyEncoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            
            // 保存到数据库
            String sql = "INSERT INTO oauth2_jwt_keys (id, key_id, public_key, private_key, is_active) VALUES (?, ?, ?, ?, true)";
            jdbcTemplate.update(sql, id, keyId, publicKeyEncoded, privateKeyEncoded);
            
            // 创建并返回JWKSource
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();
            
            return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (Exception e) {
            throw new RuntimeException("创建JWK失败", e);
        }
    }
    
    private RSAKey buildRsaKey(JwkData jwkData) {
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            
            // 解码Base64密钥
            byte[] publicKeyBytes = Base64.getDecoder().decode(jwkData.getPublicKey());
            byte[] privateKeyBytes = Base64.getDecoder().decode(jwkData.getPrivateKey());
            
            // 重建RSA公钥和私钥
            java.security.spec.X509EncodedKeySpec publicKeySpec = new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
            
            java.security.spec.PKCS8EncodedKeySpec privateKeySpec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
            
            // 构建RSAKey
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwkData.getKeyId())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("构建RSAKey失败", e);
        }
    }
    
    private static class JwkData {
        private final String id;
        private final String keyId;
        private final String publicKey;
        private final String privateKey;
        
        public JwkData(String id, String keyId, String publicKey, String privateKey) {
            this.id = id;
            this.keyId = keyId;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        
        public String getId() {
            return id;
        }
        
        public String getKeyId() {
            return keyId;
        }
        
        public String getPublicKey() {
            return publicKey;
        }
        
        public String getPrivateKey() {
            return privateKey;
        }
    }
    
    private static class JwkRowMapper implements RowMapper<JwkData> {
        @Override
        public JwkData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new JwkData(
                rs.getString("id"),
                rs.getString("key_id"),
                rs.getString("public_key"),
                rs.getString("private_key")
            );
        }
    }
} 