package com.example.api_auth_server.logging;

import java.util.regex.Pattern;

public class SensitiveDataMasker {
    
    private static final Pattern CLIENT_SECRET_PATTERN = Pattern.compile("(client_secret[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("(access_token[\"']?\\s*[:=]\\s*[\"']?)([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
    
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
        
        return maskClientId(clientId);
    }
    
    private static String extractValueByKey(String data, String key) {
        Pattern pattern = Pattern.compile(key + "[\"']?\\s*[:=]\\s*[\"']?([^\"'&\\s]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
