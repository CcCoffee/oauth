package com.example.api_auth_server.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 表单数据参数提取器
 * 处理 application/x-www-form-urlencoded 类型的请求
 */
@Component
public class FormDataParameterExtractor implements RequestParameterExtractor {
    
    private static final String[] SUPPORTED_CONTENT_TYPES = {
        "application/x-www-form-urlencoded"
    };
    
    @Override
    public Map<String, Object> extractParameters(HttpServletRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        
        try {
            // 提取表单参数
            Map<String, String> formParams = extractFormParameters(request);
            parameters.putAll(formParams);
            
            // 提取查询参数
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
     * 提取表单参数
     */
    private Map<String, String> extractFormParameters(HttpServletRequest request) {
        Map<String, String> formParams = new HashMap<>();
        
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return formParams;
        }
        
        try {
            String body = getRequestBody(request);
            if (body != null && !body.isEmpty()) {
                parseFormEncodedParameters(body, formParams);
            }
        } catch (Exception e) {
            // 读取失败时忽略
        }
        
        return formParams;
    }
    
    /**
     * 提取查询参数
     */
    private Map<String, String> extractQueryParameters(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            parseFormEncodedParameters(queryString, queryParams);
        }
        
        return queryParams;
    }
    
    /**
     * 解析表单编码的参数
     */
    private void parseFormEncodedParameters(String data, Map<String, String> params) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        String[] pairs = data.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length >= 1) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = keyValue.length == 2 ? 
                        URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                    params.put(key, value);
                } catch (Exception e) {
                    // 解码失败时跳过此参数
                }
            }
        }
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