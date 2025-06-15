package com.example.api_auth_server.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class TokenResponseFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TokenResponseFilter.class);
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public TokenResponseFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        // Only process token endpoints
        if (!isTokenEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(request, responseWrapper);
            
            // Get response content
            byte[] content = responseWrapper.getContentAsByteArray();
            if (content.length > 0) {
                String json = new String(content);
                Map<String, Object> responseMap = objectMapper.readValue(json, Map.class);
                
                // Get token and expires_in
                String token = (String) responseMap.get("access_token");
                Integer expiresIn = (Integer) responseMap.get("expires_in");
                
                if (token != null && expiresIn != null) {
                    // Calculate expiration time
                    Instant expirationTime = Instant.now().plusSeconds(expiresIn);
                    String formattedExpiration = formatter.format(expirationTime);
                    
                    // Create masked token
                    String maskedToken = maskToken(token);
                    
                    // Extract client id from JWT if possible
                    String clientId = extractClientIdFromJwt(token);
                    
                    // Log the information
                    if (clientId != null) {
                        logger.info("[Service token generated] - Client id: {}, Masked token: {}, Expires in: {}s, Expires at: {}", 
                            clientId, maskedToken, expiresIn, formattedExpiration);
                    } else {
                        logger.info("[Profile token generated] - Masked token: {}, Expires in: {}s, Expires at: {}", 
                            maskedToken, expiresIn, formattedExpiration);
                    }
                }
            }
            
            // Write the original content back
            responseWrapper.copyBodyToResponse();
        } catch (Exception e) {
            logger.error("Error processing token response", e);
            responseWrapper.copyBodyToResponse();
        }
    }
    
    private boolean isTokenEndpoint(String path) {
        return path != null && (path.endsWith("/oauth2/token") || path.endsWith("/oauth/token"));
    }
    
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        // Keep first 4 and last 4 characters, mask the rest
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
    
    private String extractClientIdFromJwt(String token) {
        try {
            // Split the JWT token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            
            // Get the audience (aud) claim
            Object aud = claims.get("aud");
            if (aud instanceof String) {
                return (String) aud;
            } else if (aud instanceof List) {
                List<?> audList = (List<?>) aud;
                if (!audList.isEmpty() && audList.get(0) instanceof String) {
                    return (String) audList.get(0);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract client id from JWT token", e);
        }
        return null;
    }
} 