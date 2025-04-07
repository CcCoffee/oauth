package com.example.api_auth_server.config;

import com.example.api_auth_server.filter.CsrfDisableFilter;
import com.example.api_auth_server.filter.SecurityHttpFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SecurityHttpFilter> securityHttpFilterRegistration(SecurityHttpFilter filter) {
        FilterRegistrationBean<SecurityHttpFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SecurityHttpFilter.CorsFilter> corsFilterRegistration(SecurityHttpFilter.CorsFilter filter) {
        FilterRegistrationBean<SecurityHttpFilter.CorsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CsrfDisableFilter> csrfDisableFilterRegistration(CsrfDisableFilter filter) {
        FilterRegistrationBean<CsrfDisableFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.addUrlPatterns("/*");
        return registration;
    }
} 