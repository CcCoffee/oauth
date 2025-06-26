package com.example.api_auth_server.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Request parameter extractor interface
 * Used for handling parameter extraction for different Content-Types
 */
public interface RequestParameterExtractor {
    
    /**
     * Extract parameters from HTTP request
     * @param request HTTP request
     * @return mapping of parameter names to values
     */
    Map<String, Object> extractParameters(HttpServletRequest request);
    
    /**
     * Check if specified Content-Type is supported
     * @param contentType content type
     * @return whether it's supported
     */
    boolean supports(String contentType);
    
    /**
     * Get list of supported Content-Types
     * @return Content-Type array
     */
    String[] getSupportedContentTypes();
} 