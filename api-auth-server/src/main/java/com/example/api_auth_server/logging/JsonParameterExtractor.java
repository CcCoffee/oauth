package com.example.api_auth_server.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON parameter extractor
 * Handles application/json type requests
 */
@Component
public class JsonParameterExtractor implements RequestParameterExtractor {
    
    private static final String[] SUPPORTED_CONTENT_TYPES = {
        "application/json"
    };
    
    private final ObjectMapper objectMapper;
    
    public JsonParameterExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Map<String, Object> extractParameters(HttpServletRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            // Extract JSON request body parameters
            Map<String, Object> jsonParams = extractJsonParameters(request);
            parameters.putAll(jsonParams);
            
            // Extract query parameters (JSON requests may also have query parameters)
            Map<String, String> queryParams = extractQueryParameters(request);
            parameters.putAll(queryParams);
            
            // Mask sensitive parameters
            return maskSensitiveParameters(parameters);
            
        } catch (Exception e) {
            // Return empty map when parameter extraction fails
            return new HashMap<>();
        }
    }
    
    @Override
    public boolean supports(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        for (String supportedType : SUPPORTED_CONTENT_TYPES) {
            if (contentType.toLowerCase().startsWith(supportedType)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String[] getSupportedContentTypes() {
        return SUPPORTED_CONTENT_TYPES.clone();
    }
    
    /**
     * Extract JSON parameters
     */
    private Map<String, Object> extractJsonParameters(HttpServletRequest request) {
        Map<String, Object> jsonParams = new HashMap<>();
        
        try {
            String jsonBody = getRequestBody(request);
            if (jsonBody != null && !jsonBody.trim().isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(jsonBody);
                flattenJsonNode("", rootNode, jsonParams);
            }
        } catch (Exception e) {
            // Ignore JSON parsing failures
        }
        
        return jsonParams;
    }
    
    /**
     * Recursively flatten JSON nodes
     */
    private void flattenJsonNode(String prefix, JsonNode node, Map<String, Object> result) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                flattenJsonNode(key, field.getValue(), result);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenJsonNode(key, node.get(i), result);
            }
        } else {
            // Leaf node, store value
            Object value = extractJsonValue(node);
            result.put(prefix, value);
        }
    }
    
    /**
     * Extract value from JSON node
     */
    private Object extractJsonValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else {
                return node.asDouble();
            }
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNull()) {
            return null;
        } else {
            return node.toString();
        }
    }
    
    /**
     * Extract query parameters
     */
    private Map<String, String> extractQueryParameters(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length >= 1) {
                    try {
                        String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String value = keyValue.length == 2 ? 
                            java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                        queryParams.put(key, value);
                    } catch (Exception e) {
                        // Skip this parameter if decoding fails
                    }
                }
            }
        }
        
        return queryParams;
    }
    
    /**
     * Get request body content
     */
    private String getRequestBody(HttpServletRequest request) {
        try {
            if (request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    return new String(content, StandardCharsets.UTF_8);
                }
            } else {
                // Try to read request body (note: can only be read once)
                StringBuilder body = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }
                return body.toString();
            }
        } catch (Exception e) {
            // Return null when read fails
        }
        return null;
    }
    
    /**
     * Mask sensitive parameters
     */
    private Map<String, Object> maskSensitiveParameters(Map<String, Object> params) {
        Map<String, Object> maskedParams = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (isSensitiveParameter(key)) {
                maskedParams.put(key, maskValue(value));
            } else {
                maskedParams.put(key, value);
            }
        }
        
        return maskedParams;
    }
    
    /**
     * Check if parameter is sensitive
     */
    private boolean isSensitiveParameter(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") ||
               lowerName.contains("secret") ||
               lowerName.contains("token") ||
               lowerName.contains("credential") ||
               lowerName.contains("auth") ||
               lowerName.equals("client_secret") ||
               lowerName.equals("access_token") ||
               lowerName.equals("refresh_token");
    }
    
    /**
     * Mask value
     */
    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return SensitiveDataMasker.maskSensitiveData((String) value);
        }
        
        return "***";
    }
} 