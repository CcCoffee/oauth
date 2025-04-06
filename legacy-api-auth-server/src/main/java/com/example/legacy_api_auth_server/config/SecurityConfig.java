package com.example.legacy_api_auth_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                // 保护客户端管理API，只允许ADMIN角色访问
                .antMatchers("/api/clients/**").hasRole("ADMIN")
                // 保护令牌导出API，只允许ADMIN角色访问
                .antMatchers("/api/admin/token/**").hasRole("ADMIN")
                // 允许访问OAuth相关端点
                .antMatchers("/oauth/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            .and()
                .httpBasic();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new StandardPasswordEncoder();
    }
}