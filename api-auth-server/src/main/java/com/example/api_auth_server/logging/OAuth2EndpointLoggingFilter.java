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

@Component
public class OAuth2EndpointLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger apiLogger = LoggerFactory.getLogger("API_LOG");
    private final ObjectMapper objectMapper;
    
    public OAuth2EndpointLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Only process OAuth2 endpoints
        if (!isOAuth2Endpoint(request.getRequestURI())) {
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
            
            // Extract client ID from request
            String requestBody = getRequestBody(request);
            String clientId = SensitiveDataMasker.extractClientId(requestBody, request.getQueryString());
            logData.setClientId(clientId);
            
            // Add error message for non-successful responses
            if (response.getStatus() >= 400) {
                logData.setErrorMessage("HTTP " + response.getStatus());
            }
            
            String jsonLog = objectMapper.writeValueAsString(logData);
            apiLogger.info(jsonLog);
            
        } catch (Exception e) {
            apiLogger.error("Failed to log OAuth2 endpoint request", e);
        }
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return SensitiveDataMasker.maskSensitiveData(body);
        }
        return null;
    }
    
    private boolean isOAuth2Endpoint(String uri) {
        // Only intercept Spring Boot OAuth2 default endpoints
        // Custom controller endpoints will be handled by AOP
        return uri.startsWith("/oauth2/") || 
               uri.startsWith("/.well-known/");
    }
}
