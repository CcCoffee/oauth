package com.example.api_auth_server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CsrfDisableFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 为每个请求生成一个CSRF令牌，但不进行验证，实际上禁用了CSRF保护
        CsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
} 