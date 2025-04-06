package com.example.legacy_api_consumer_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableOAuth2Client
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${oauth2.server.token-uri}")
    private String tokenUri;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .anyRequest().permitAll()
                .and()
            .csrf().disable();
    }
    
    @Bean
    @ConfigurationProperties("oauth2.client.jwt")
    public ClientCredentialsResourceDetails jwtClientCredentialsResourceDetails() {
        ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setAccessTokenUri(tokenUri);
        details.setScope(Arrays.asList("message.read"));
        return details;
    }
    
    @Bean
    public OAuth2RestTemplate jwtOAuth2RestTemplate(OAuth2ClientContext oauth2ClientContext) {
        return new OAuth2RestTemplate(jwtClientCredentialsResourceDetails(), oauth2ClientContext);
    }
} 