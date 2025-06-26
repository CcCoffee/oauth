package com.example.api_auth_server.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class UnifiedApiLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger apiLogger = LoggerFactory.getLogger("API_LOG");
    private final ObjectMapper objectMapper;
    private final FormDataParameterExtractor formDataExtractor;
    private final JsonParameterExtractor jsonExtractor;
    
    public UnifiedApiLoggingFilter(ObjectMapper objectMapper, 
                                 FormDataParameterExtractor formDataExtractor,
                                 JsonParameterExtractor jsonExtractor) {
        this.objectMapper = objectMapper;
        this.formDataExtractor = formDataExtractor;
        this.jsonExtractor = jsonExtractor;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Only process API endpoints defined in Controllers
        if (!shouldLogRequest(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest, wrappedResponse, startTime);
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private void logRequest(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, 
                          long startTime) {
        try {
            ApiLogData logData = new ApiLogData();
            logData.setEndpoint(request.getRequestURI());
            logData.setMethod(request.getMethod());
            logData.setDuration(System.currentTimeMillis() - startTime);
            logData.setStatus(String.valueOf(response.getStatus()));
            
            // Extract parameters using appropriate extractor
            Map<String, Object> parameters = extractParameters(request);
            // Apply sensitive data masking
            parameters = SensitiveDataMasker.maskSensitiveParameters(parameters);
            logData.setParameters(parameters);
            
            // Extract client ID from parameters or request
            String clientId = extractClientId(parameters, request);
            logData.setClientId(clientId);
            
            // Add error message for non-successful responses
            if (response.getStatus() >= 400) {
                logData.setErrorMessage("HTTP " + response.getStatus());
            }
            
            String jsonLog = objectMapper.writeValueAsString(logData);
            apiLogger.info(jsonLog);
            
        } catch (Exception e) {
            apiLogger.error("Failed to log API request", e);
        }
    }
    
    /**
     * 提取请求参数
     */
    private Map<String, Object> extractParameters(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        
        if (formDataExtractor.supports(contentType)) {
            return formDataExtractor.extractParameters(request);
        } else if (jsonExtractor.supports(contentType)) {
            return jsonExtractor.extractParameters(request);
        } else {
            // 默认只提取查询参数
            return formDataExtractor.extractParameters(request);
        }
    }
    
    /**
     * 提取客户端ID
     */
    private String extractClientId(Map<String, Object> parameters, ContentCachingRequestWrapper request) {
        // 首先尝试从参数中获取client_id
        Object clientIdParam = parameters.get("client_id");
        if (clientIdParam != null) {
            return clientIdParam.toString(); // 不再脱敏client_id
        }
        
        // 从请求中提取（包括Basic Auth、查询字符串等）
        String requestBody = null;
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                requestBody = new String(content, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // 忽略读取错误
        }
        
        String authorizationHeader = request.getHeader("Authorization");
        String clientId = SensitiveDataMasker.extractClientId(requestBody, request.getQueryString(), authorizationHeader);
        return clientId;
    }
    
    /**
     * 判断是否应该记录请求日志
     * 覆盖Controller中定义的API端点
     */
    private boolean shouldLogRequest(String uri) {
        return uri.startsWith("/oauth/") ||         // LegacyOAuthController
               uri.startsWith("/oauth2/") ||        // OAuth2端点  
               uri.startsWith("/client") ||        // 客户端管理端点
               uri.startsWith("/api/") ||           // MigrationController等
               uri.startsWith("/.well-known/");   // OIDC发现端点

    }
}
