package com.example.api_auth_server.controller;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.example.api_auth_server.service.TokenService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for legacy OAuth2 endpoints
 */
@RestController
public class LegacyOAuthController {
    
    @Value("${app.jwt.keystore.path}")
    private String keystorePath;
    
    @Value("${app.jwt.keystore.password}")
    private String keystorePassword;
    
    @Value("${app.jwt.keystore.key-alias}")
    private String keyAlias;

    private final JwtDecoder jwtDecoder;
    private final TokenService tokenService;

    public LegacyOAuthController(JwtDecoder jwtDecoder, TokenService tokenService) {
        this.jwtDecoder = jwtDecoder;
        this.tokenService = tokenService;
    }
    
    /**
     * Provides public key information interface, compatible with spring-cloud-starter-oauth2
     * Returns the public key in PEM format
     * @return Map containing the public key
     */
    @GetMapping("/oauth/token_key")
    public Map<String, String> getTokenKey() {
        try {
            // Load the keystore file from the class path
            ClassPathResource resource = new ClassPathResource(keystorePath);
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(resource.getInputStream(), keystorePassword.toCharArray());
            
            // Get the certificate
            Certificate cert = keyStore.getCertificate(keyAlias);
            RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
            
            // Convert to PEM format public key
            byte[] encoded = publicKey.getEncoded();
            String publicKeyPEM =
                    "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getEncoder().encodeToString(encoded) +
                    "\n-----END PUBLIC KEY-----";

            // Build the response Map
            Map<String, String> result = new HashMap<>();
            result.put("alg", "SHA256withRSA");
            result.put("value", publicKeyPEM);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get public key: " + e.getMessage(), e);
        }
    }

    /**
     * Intercepts /oauth/token requests and forwards them to /oauth2/token
     */
    @RequestMapping(value = "/oauth/token", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleTokenRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/oauth2/token");
        dispatcher.forward(request, response);
    }

    @PostMapping("/oauth/check_token")
    public ResponseEntity<Map<String, Object>> checkToken(@RequestParam("token") String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            Map<String, Object> response = new HashMap<>();
            response.put("active", true);
            response.put("exp", jwt.getExpiresAt().toEpochMilli() / 1000);
            response.put("client_id", jwt.getClaimAsString("client_id"));
            response.put("jti", jwt.getId());
            response.put("scope", jwt.getClaimAsStringList("scope"));
            response.put("aud", jwt.getAudience());

            return ResponseEntity.ok(response);
        } catch (JwtValidationException e) {
            String msg = e.getMessage();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_token");
            error.put("error_description", msg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Remove token API with Spring Security Authentication
     * 使用Spring Security Authentication对象进行客户端验证
     * @param token 要删除的token
     * @param authentication Spring Security提供的认证对象，包含已验证的客户端信息
     * @return 删除结果
     */
    @PostMapping("/oauth/remove_token")
    public ResponseEntity<Map<String, Object>> removeToken(@RequestParam("token") String token, 
                                                          Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 获取已认证的客户端ID
            // Spring Security已经验证了Basic Auth，这里直接获取客户端ID
            String clientId = authentication.getName();
            
            // 2. 验证token是否存在
            if (!tokenService.isTokenValid(token)) {
                response.put("error", "invalid_token");
                response.put("error_description", "Token not found or expired");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // 3. 获取token对应的客户端ID（可选，用于日志记录）
            Optional<String> tokenClientId = tokenService.findClientIdByToken(token);
            
            // 4. 删除token
            boolean deleted = tokenService.deleteTokenByValue(token);
            if (deleted) {
                response.put("message", "Token removed successfully");
                response.put("authenticated_client_id", clientId);
                response.put("authorities", authentication.getAuthorities());
                if (tokenClientId.isPresent()) {
                    response.put("token_client_id", tokenClientId.get());
                }
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "token_not_found");
                response.put("error_description", "Token could not be deleted");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            response.put("error", "server_error");
            response.put("error_description", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

} 