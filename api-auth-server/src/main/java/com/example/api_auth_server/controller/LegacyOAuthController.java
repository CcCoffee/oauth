package com.example.api_auth_server.controller;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
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

    public LegacyOAuthController(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
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
        } catch (JwtException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("active", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

} 