package com.example.api_auth_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration class, adds CORS support
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Adds CORS support for all API paths
        registry.addMapping("/**")
                .allowedOrigins("https://editor.swagger.io") // Allows Swagger Editor access
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // No need to re-preflight (send OPTIONS request) within 1 hour
    }

    /**
     * Creates a CORS filter for handling preflight and actual requests
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allows Swagger Editor
        config.addAllowedOrigin("https://editor.swagger.io");
        // Allows all header information
        config.addAllowedHeader("*");
        // Allows all methods
        config.addAllowedMethod("*");
        // Allows sending Cookie
        config.setAllowCredentials(true);
        // Caches preflight request results for 1 hour
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 