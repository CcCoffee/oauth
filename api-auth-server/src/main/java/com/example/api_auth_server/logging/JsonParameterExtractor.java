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
 * JSON参数提取器
 * 处理 application/json 类型的请求
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
            // 提取JSON请求体参数
            Map<String, Object> jsonParams = extractJsonParameters(request);
            parameters.putAll(jsonParams);
            
            // 提取查询参数（JSON请求也可能有查询参数）
            Map<String, String> queryParams = extractQueryParameters(request);
            parameters.putAll(queryParams);
            
            // 对敏感参数进行脱敏
            return maskSensitiveParameters(parameters);
            
        } catch (Exception e) {
            // 参数提取失败时返回空map
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
     * 提取JSON参数
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
            // JSON解析失败时忽略
        }
        
        return jsonParams;
    }
    
    /**
     * 递归展平JSON节点
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
            // 叶子节点，存储值
            Object value = extractJsonValue(node);
            result.put(prefix, value);
        }
    }
    
    /**
     * 提取JSON节点的值
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
     * 提取查询参数
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
                        // 解码失败时跳过此参数
                    }
                }
            }
        }
        
        return queryParams;
    }
    
    /**
     * 获取请求体内容
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
                // 尝试读取请求体（注意：只能读取一次）
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
            // 读取失败时返回null
        }
        return null;
    }
    
    /**
     * 对敏感参数进行脱敏
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
     * 判断是否为敏感参数
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
     * 脱敏值
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