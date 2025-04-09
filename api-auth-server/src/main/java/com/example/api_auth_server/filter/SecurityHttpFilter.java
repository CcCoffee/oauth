package com.example.api_auth_server.filter;

import com.example.api_auth_server.config.AdminProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHttpFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AdminProperties adminProperties;
    
    private final List<String> publicPaths = List.of(
            "/api-docs/**", 
            "/oauth/token", 
            "/oauth/token_key"
    );
    
    private final List<String> adminPaths = List.of(
            "/api/admin/**", 
            "/clients/**"
    );

    public SecurityHttpFilter(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Check the request path
        String requestURI = request.getRequestURI();
        
        // Allow access to public paths
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check permissions for admin paths
        if (isAdminPath(requestURI)) {
            if (isAdmin(request)) {
                filterChain.doFilter(request, response);
                return;
            } else {
                // If no basic authentication is provided, send a 401 response and request authentication
                response.setHeader("WWW-Authenticate", "Basic realm=\"Admin Area\"");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        // Allow all other requests to pass
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String requestURI) {
        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }
    
    private boolean isAdminPath(String requestURI) {
        return adminPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }
    
    private boolean isAdmin(HttpServletRequest request) {
        // Get basic authentication information from the Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Decode Base64 encoded credentials
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            
            // The format of the credentials is "username:password"
            final String[] values = credentials.split(":", 2);
            if (values.length == 2) {
                String username = values[0];
                String password = values[1];
                
                // Check if the username and password match the configuration
                return adminProperties.getUsername().equals(username) && 
                       adminProperties.getPassword().equals(password) && 
                       "ADMIN".equals(adminProperties.getRole());
            }
        }
        return false;
    }

    // To avoid CORS issues, add a filter configuration
    @Component
    public static class CorsFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            
            if ("OPTIONS".equals(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            filterChain.doFilter(request, response);
        }
    }
} 