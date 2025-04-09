package com.example.legacy_api_provider_demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/api")
@RestController
public class ApiController {

    @GetMapping("/jwt/message")
    public Map<String, Object> getJwtMessage(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is an API endpoint protected by a JWT token");
        
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oauth2Auth = (OAuth2Authentication) authentication;
            Object details = oauth2Auth.getDetails();
            
            response.put("token_type", "jwt");
            response.put("client_id", oauth2Auth.getOAuth2Request().getClientId());
            response.put("scope", oauth2Auth.getOAuth2Request().getScope());
            
            if (details instanceof OAuth2AuthenticationDetails) {
                OAuth2AuthenticationDetails oauthDetails = (OAuth2AuthenticationDetails) details;
                response.put("token_value", oauthDetails.getTokenValue());
            }
            
            if (oauth2Auth.getUserAuthentication() != null) {
                response.put("user_name", oauth2Auth.getUserAuthentication().getName());
                response.put("authorities", oauth2Auth.getUserAuthentication().getAuthorities());
            }
        }
        
        return response;
    }
} 