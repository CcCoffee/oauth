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
     * Extract request parameters
     */
    private Map<String, Object> extractParameters(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        
        if (formDataExtractor.supports(contentType)) {
            return formDataExtractor.extractParameters(request);
        } else if (jsonExtractor.supports(contentType)) {
            return jsonExtractor.extractParameters(request);
        } else {
            // Default to extracting only query parameters
            return formDataExtractor.extractParameters(request);
        }
    }
    
    /**
     * Extract client ID
     */
    private String extractClientId(Map<String, Object> parameters, ContentCachingRequestWrapper request) {
        // First try to get client_id from parameters
        Object clientIdParam = parameters.get("client_id");
        if (clientIdParam != null) {
            return clientIdParam.toString(); // No longer mask client_id
        }
        
        // Extract from request (including Basic Auth, query string, etc.)
        String requestBody = null;
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                requestBody = new String(content, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // Ignore read errors
        }
        
        String authorizationHeader = request.getHeader("Authorization");
        String clientId = SensitiveDataMasker.extractClientId(requestBody, request.getQueryString(), authorizationHeader);
        return clientId;
    }
    
    /**
     * Determine whether to log request
     * Covers API endpoints defined in Controllers
     */
    private boolean shouldLogRequest(String uri) {
        return uri.startsWith("/oauth/") ||         // LegacyOAuthController
               uri.startsWith("/oauth2/") ||        // OAuth2 endpoints  
               uri.startsWith("/client") ||        // Client management endpoints
               uri.startsWith("/api/") ||           // MigrationController etc.
               uri.startsWith("/.well-known/");   // OIDC discovery endpoints

    }
}
