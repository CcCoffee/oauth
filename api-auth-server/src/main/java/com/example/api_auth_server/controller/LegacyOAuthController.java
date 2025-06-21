package com.example.api_auth_server.controller;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
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
    private final RegisteredClientRepository registeredClientRepository;

    public LegacyOAuthController(JwtDecoder jwtDecoder, RegisteredClientRepository registeredClientRepository) {
        this.jwtDecoder = jwtDecoder;
        this.registeredClientRepository = registeredClientRepository;
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
     * Compatible with legacy OAuth2 behavior: when no scope is provided, use all client scopes
     */
    @RequestMapping(value = "/oauth/token", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleTokenRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Check if scope parameter is missing
        String scope = request.getParameter("scope");
        String clientId = extractClientId(request);
        
        if (scope == null && clientId != null) {
            // Find the registered client
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient != null) {
                Set<String> clientScopes = registeredClient.getScopes();
                if (!clientScopes.isEmpty()) {
                    // Create a wrapper request with default scopes
                    String defaultScope = String.join(" ", clientScopes);
                    LegacyTokenRequestWrapper wrappedRequest = new LegacyTokenRequestWrapper(request, defaultScope);
                    RequestDispatcher dispatcher = wrappedRequest.getRequestDispatcher("/oauth2/token");
                    dispatcher.forward(wrappedRequest, response);
                    return;
                }
            }
        }
        
        // Forward original request if scope is provided or client not found
        RequestDispatcher dispatcher = request.getRequestDispatcher("/oauth2/token");
        dispatcher.forward(request, response);
    }

    /**
     * Extract client ID from request, supporting three methods:
     * 1. Basic Auth (recommended) - Authorization: Basic base64(client_id:client_secret)
     * 2. Form parameters - client_id parameter in request body
     * 3. URL parameters - client_id parameter in query string
     */
    private String extractClientId(HttpServletRequest request) {
        // Method 1: Basic Auth (highest priority)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials));
                String[] parts = credentials.split(":", 2);
                if (parts.length >= 1) {
                    return parts[0];
                }
            } catch (Exception e) {
                // Ignore parsing errors and fall back to parameter extraction
            }
        }
        
        // Method 2 & 3: Form parameters or URL parameters
        String clientId = request.getParameter("client_id");
        if (clientId != null) {
            return clientId;
        }
        
        return null;
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
     * HttpServletRequest wrapper that adds a default scope parameter
     */
    private static class LegacyTokenRequestWrapper extends HttpServletRequestWrapper {
        private final String scope;
        
        public LegacyTokenRequestWrapper(HttpServletRequest request, String scope) {
            super(request);
            this.scope = scope;
        }
        
        @Override
        public String getParameter(String name) {
            if ("scope".equals(name)) {
                return scope;
            }
            return super.getParameter(name);
        }
        
        @Override
        public String[] getParameterValues(String name) {
            if ("scope".equals(name)) {
                return new String[]{scope};
            }
            return super.getParameterValues(name);
        }
        
        @Override
        public Enumeration<String> getParameterNames() {
            Set<String> names = Collections.list(super.getParameterNames()).stream()
                    .collect(Collectors.toSet());
            names.add("scope");
            return Collections.enumeration(names);
        }
        
        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> params = new HashMap<>(super.getParameterMap());
            params.put("scope", new String[]{scope});
            return params;
        }
    }

} 