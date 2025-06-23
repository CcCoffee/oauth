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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;
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
     * Compatible with legacy OAuth2 behavior: 
     * 1. Prioritizes form body parameters over query parameters
     * 2. Falls back to query parameters when form body doesn't contain grant_type/scope
     * 3. When no scope is provided, use all client scopes
     */
    @RequestMapping(value = "/oauth/token", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleTokenRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Separate form body parameters and query parameters

        if (!StringUtils.hasText(request.getParameter("grant_type"))) {

        }

        Map<String, String> formParams = extractFormParameters(request);
        Map<String, String> queryParams = extractQueryParameters(request);
        
        // Build final parameter map with priority: form body > query parameters
        Map<String, String> finalParams = new HashMap<>();
        
        // Start with query parameters
        finalParams.putAll(queryParams);
        
        // Override with form parameters (higher priority)
        finalParams.putAll(formParams);
        
        // Special handling: if grant_type or scope is missing from form body, use query values
        if (!formParams.containsKey("grant_type") && queryParams.containsKey("grant_type")) {
            finalParams.put("grant_type", queryParams.get("grant_type"));
        }
        if (!formParams.containsKey("scope") && queryParams.containsKey("scope")) {
            finalParams.put("scope", queryParams.get("scope"));
        }
        
        // Extract client ID and handle default scope logic
        String clientId = extractClientId(request);
        String scope = finalParams.get("scope");
        
        // Handle scope - if missing, try to get default scopes from client
        if (scope == null && clientId != null) {
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient != null) {
                Set<String> clientScopes = registeredClient.getScopes();
                if (!clientScopes.isEmpty()) {
                    String defaultScope = String.join(" ", clientScopes);
                    finalParams.put("scope", defaultScope);
                }
            }
        }
        
        // Convert all parameters to application/x-www-form-urlencoded format
        String formEncodedBody = buildFormEncodedBody(finalParams);
        
        // Create wrapped request with form-encoded body
        FormEncodedRequestWrapper wrappedRequest = new FormEncodedRequestWrapper(request, formEncodedBody);
        RequestDispatcher dispatcher = wrappedRequest.getRequestDispatcher("/oauth2/token");
        dispatcher.forward(wrappedRequest, response);
    }

    /**
     * Extract form body parameters from request
     */
    private Map<String, String> extractFormParameters(HttpServletRequest request) {
        Map<String, String> formParams = new HashMap<>();
        
        // Only extract form parameters if it's a POST request with form content type
        if ("POST".equalsIgnoreCase(request.getMethod()) && 
            request.getContentType() != null && 
            request.getContentType().startsWith("application/x-www-form-urlencoded")) {
            
            try {
                // Read the request body
                StringBuilder body = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }
                
                // Parse form-encoded parameters
                String bodyContent = body.toString();
                if (!bodyContent.isEmpty()) {
                    String[] pairs = bodyContent.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                            String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            formParams.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                // If we can't read the form body, fall back to empty map
                // This allows query parameters to be used instead
            }
        }
        
        return formParams;
    }

    /**
     * Extract query parameters from request
     */
    private Map<String, String> extractQueryParameters(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        queryParams.put(key, value);
                    } catch (Exception e) {
                        // Skip malformed parameters
                    }
                } else if (keyValue.length == 1) {
                    // Handle parameters without values
                    try {
                        String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        queryParams.put(key, "");
                    } catch (Exception e) {
                        // Skip malformed parameters
                    }
                }
            }
        }
        
        return queryParams;
    }

    /**
     * Build application/x-www-form-urlencoded body from parameter map
     */
    private String buildFormEncodedBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                               URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode parameter: " + entry.getKey(), e);
                    }
                })
                .collect(Collectors.joining("&"));
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
     * HttpServletRequest wrapper that converts query parameters to form-encoded body
     * Ensures OAuth2 parameters are sent as application/x-www-form-urlencoded
     */
    private static class FormEncodedRequestWrapper extends HttpServletRequestWrapper {
        private final String formEncodedBody;
        private final byte[] bodyBytes;
        
        public FormEncodedRequestWrapper(HttpServletRequest request, String formEncodedBody) {
            super(request);
            this.formEncodedBody = formEncodedBody;
            this.bodyBytes = formEncodedBody.getBytes(StandardCharsets.UTF_8);
        }
        
        @Override
        public String getMethod() {
            return "POST";
        }
        
        @Override
        public String getContentType() {
            return "application/x-www-form-urlencoded";
        }
        
        @Override
        public int getContentLength() {
            return bodyBytes.length;
        }
        
        @Override
        public long getContentLengthLong() {
            return bodyBytes.length;
        }
        
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new StringReader(formEncodedBody));
        }
        
        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final ByteArrayInputStream inputStream = new ByteArrayInputStream(bodyBytes);
                
                @Override
                public int read() {
                    return inputStream.read();
                }
                
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }
                
                @Override
                public boolean isReady() {
                    return true;
                }
                
                @Override
                public void setReadListener(ReadListener readListener) {
                    // Not implemented for this use case
                }
            };
        }
        
        // Remove query string to prevent duplicate parameters
        @Override
        public String getQueryString() {
            return null;
        }
        
        @Override
        public String getRequestURI() {
            String uri = super.getRequestURI();
            // Remove query parameters from URI
            int queryIndex = uri.indexOf('?');
            return queryIndex != -1 ? uri.substring(0, queryIndex) : uri;
        }
        
        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer(super.getRequestURL().toString());
            // Remove query parameters from URL
            int queryIndex = url.indexOf("?");
            if (queryIndex != -1) {
                url.setLength(queryIndex);
            }
            return url;
        }
    }

} 