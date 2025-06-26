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
    
    // OAuth2 特定敏感字段
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
        
        return clientId; // 不再脱敏client_id
    }
    
    /**
     * 从请求中提取客户端ID，支持多种方式：
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
        
        return clientId; // 不再脱敏client_id
    }
    
    /**
     * 从Basic Auth header中提取客户端ID
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
                return parts[0]; // 返回未脱敏的client_id，由调用方处理脱敏
            }
        } catch (Exception e) {
            // 忽略解析错误，返回null
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
     * JWT token特殊脱敏处理
     * 保留header部分，脱敏payload和signature
     */
    private static String maskJwtToken(String jwtToken) {
        if (jwtToken == null || jwtToken.length() <= 8) {
            return "***";
        }
        
        String[] parts = jwtToken.split("\\.");
        if (parts.length == 3) {
            // JWT格式：header.payload.signature
            // 保留header，脱敏payload和signature
            return parts[0] + ".***." + "***";
        }
        
        // 不是标准JWT格式，使用通用脱敏
        return maskToken(jwtToken);
    }
    
    /**
     * 检查字段名是否为敏感字段
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
     * 脱敏参数Map中的敏感值
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
     * 脱敏参数值
     */
    private static Object maskParameterValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            
            // 检查是否是JWT token格式
            if (strValue.matches("[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+")) {
                return maskJwtToken(strValue);
            }
            
            // 普通token脱敏
            return maskToken(strValue);
        }
        
        return "***";
    }
}
