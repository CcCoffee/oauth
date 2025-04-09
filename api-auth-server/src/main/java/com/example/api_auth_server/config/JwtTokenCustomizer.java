package com.example.api_auth_server.config;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        // method one
        List<String> modifiedAudiences = new ArrayList<>();
        modifiedAudiences.add(context.getRegisteredClient().getClientSettings().getSetting("resource.id")); // aud list includes resource id
        context.getClaims().audience(modifiedAudiences);

        // Method two, get the original audience information
        // List<String> audiences = new ArrayList<>(context.getClaims().build().getAudience());
        // if (!audiences.isEmpty()) {
        //     // Replace the original audience
        //     List<String> modifiedAudiences = new ArrayList<>(audiences);
        //     modifiedAudiences.add(context.getRegisteredClient().getClientSettings().getSetting("resource.id")); // aud list includes resource id
        //     context.getClaims().audience(modifiedAudiences);
        // }
    }
} 