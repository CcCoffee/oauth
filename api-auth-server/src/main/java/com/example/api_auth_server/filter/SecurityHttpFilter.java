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
        
        // 检查请求路径
        String requestURI = request.getRequestURI();
        
        // 允许公共路径访问
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 针对管理员路径进行权限检查
        if (isAdminPath(requestURI)) {
            if (isAdmin(request)) {
                filterChain.doFilter(request, response);
                return;
            } else {
                // 如果没有提供基本认证，则发送401响应并要求认证
                response.setHeader("WWW-Authenticate", "Basic realm=\"Admin Area\"");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        // 允许其他所有请求通过
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
        // 从Authorization头中获取基本认证信息
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // 解码Base64编码的凭证
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            
            // 凭证的格式是"username:password"
            final String[] values = credentials.split(":", 2);
            if (values.length == 2) {
                String username = values[0];
                String password = values[1];
                
                // 检查用户名和密码是否与配置匹配
                return adminProperties.getUsername().equals(username) && 
                       adminProperties.getPassword().equals(password) && 
                       "ADMIN".equals(adminProperties.getRole());
            }
        }
        return false;
    }

    // 为避免CORS问题，添加一个过滤器配置
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