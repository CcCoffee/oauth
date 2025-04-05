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
        // 方法一
        List<String> modifiedAudiences = new ArrayList<>();
        modifiedAudiences.add(context.getRegisteredClient().getClientSettings().getSetting("resource.id")); // aud list 包含 resource id 即可
        context.getClaims().audience(modifiedAudiences);

        // 方法二，获取原有的audience信息
//        List<String> audiences = new ArrayList<>(context.getClaims().build().getAudience());
//        if (!audiences.isEmpty()) {
//            // 替换原有的audience
//            List<String> modifiedAudiences = new ArrayList<>(audiences);
//            modifiedAudiences.add(context.getRegisteredClient().getClientSettings().getSetting("resource.id")); // aud list 包含 resource id 即可
//            context.getClaims().audience(modifiedAudiences);
//        }
    }
} 