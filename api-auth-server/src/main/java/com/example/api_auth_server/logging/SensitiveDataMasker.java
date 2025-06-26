package com.example.api_auth_server.logging;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class SensitiveDataMasker {
    
    private static final Pattern CLIENT_SECRET_PATTERN = Pattern.compile("(client_secret[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("(access_token[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("(refresh_token[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("(authorization[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JWT_PATTERN = Pattern.compile("(\\b[A-Za-z0-9-_]{20,})\\.[A-Za-z0-9-_]{20,}\\.[A-Za-z0-9-_]{20,}");
    
    // OAuth2 specific sensitive fields
    private static final String[] OAUTH2_SENSITIVE_FIELDS = {
        "client_secret", "access_token", "refresh_token", "id_token", 
        "password", "credential", "authorization", "auth", "token"
    };
    
    public static String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        String masked = data;
        
        // Mask client_secret and password completely
        masked = CLIENT_SECRET_PATTERN.matcher(masked).replaceAll("$1***");
        masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1***");
        
        // Mask access_token keeping first 4 and last 4 characters
        masked = ACCESS_TOKEN_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String prefix = matchResult.group(1);
            String token = matchResult.group(2);
            return prefix + maskToken(token);
        });
        
        // Mask refresh_token
        masked = REFRESH_TOKEN_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String prefix = matchResult.group(1);
            String token = matchResult.group(2);
            return prefix + maskToken(token);
        });
        
        // Mask authorization header values
        masked = AUTHORIZATION_PATTERN.matcher(masked).replaceAll("$1***");
        
        // Mask JWT tokens
        masked = JWT_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String token = matchResult.group();
            return maskJwtToken(token);
        });
        
        return masked;
    }
    
    public static String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 4) {
            return clientId;
        }
        return clientId.substring(0, Math.min(4, clientId.length())) + "***";
    }
    
    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
    
    public static String extractClientId(String requestBody, String queryString) {
        String clientId = null;
        
        // Try to extract from request body
        if (requestBody != null) {
            clientId = extractValueByKey(requestBody, "client_id");
        }
        
        // Try to extract from query string if not found in body
        if (clientId == null && queryString != null) {
            clientId = extractValueByKey(queryString, "client_id");
        }
        
        return clientId; // No longer mask client_id
    }
    
    /**
     * Extract client ID from request, supporting multiple methods:
     * 1. Basic Auth (Authorization header)
     * 2. Request body parameters
     * 3. Query string parameters
     */
    public static String extractClientId(String requestBody, String queryString, String authorizationHeader) {
        String clientId = null;
        
        // 1. Try to extract from Basic Auth (highest priority)
        if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
            clientId = extractClientIdFromBasicAuth(authorizationHeader);
        }
        
        // 2. Try to extract from request body
        if (clientId == null && requestBody != null) {
            clientId = extractValueByKey(requestBody, "client_id");
        }
        
        // 3. Try to extract from query string if not found in body
        if (clientId == null && queryString != null) {
            clientId = extractValueByKey(queryString, "client_id");
        }
        
        return clientId; // No longer mask client_id
    }
    
    /**
     * Extract client ID from Basic Auth header
     * Authorization: Basic base64(client_id:client_secret)
     */
    private static String extractClientIdFromBasicAuth(String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
                return null;
            }
            
            String base64Credentials = authorizationHeader.substring("Basic ".length());
            String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);
            
            if (parts.length >= 1) {
                return parts[0]; // Return unmasked client_id, let caller handle masking
            }
        } catch (Exception e) {
            // Ignore parsing errors, return null
        }
        
        return null;
    }
    
    private static String extractValueByKey(String data, String key) {
        Pattern pattern = Pattern.compile(key + "[\"']?\\s*[:=]\\s*[\"']?([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Special masking processing for JWT tokens
     * Keep header part, mask payload and signature
     */
    private static String maskJwtToken(String jwtToken) {
        if (jwtToken == null || jwtToken.length() <= 8) {
            return "***";
        }
        
        String[] parts = jwtToken.split("\\.");
        if (parts.length == 3) {
            // JWT format: header.payload.signature
            // Keep header, mask payload and signature
            return parts[0] + ".***." + "***";
        }
        
        // Not standard JWT format, use generic masking
        return maskToken(jwtToken);
    }
    
    /**
     * Check if field name is a sensitive field
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        for (String sensitiveField : OAUTH2_SENSITIVE_FIELDS) {
            if (lowerFieldName.contains(sensitiveField)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Mask sensitive values in parameter Map
     */
    public static Map<String, Object> maskSensitiveParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }
        
        Map<String, Object> maskedParams = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (isSensitiveField(key)) {
                maskedParams.put(key, maskParameterValue(value));
            } else {
                maskedParams.put(key, value);
            }
        }
        
        return maskedParams;
    }
    
    /**
     * Mask parameter value
     */
    private static Object maskParameterValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            
            // Check if it's a JWT token format
            if (strValue.matches("[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+")) {
                return maskJwtToken(strValue);
            }
            
            // Regular token masking
            return maskToken(strValue);
        }
        
        return "***";
    }
}
