package com.example.api_auth_server.service;

import com.example.api_auth_server.model.TokenInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JdbcTemplate jdbcTemplate;

    public TokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TokenInfo> getTokensByClientId(String clientId, List<String> scopes) {
        String sql = "SELECT * FROM oauth2_authorization WHERE registered_client_id = ?";
        if (scopes != null && !scopes.isEmpty()) {
            sql += " AND scope IN (?)";
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setTokenId(rs.getString("id"));
            tokenInfo.setClientId(rs.getString("registered_client_id"));
            tokenInfo.setScope(List.of(rs.getString("scope").split(" ")));
            tokenInfo.setExpiresAt(rs.getTimestamp("access_token_expires_at").toInstant());
            tokenInfo.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return tokenInfo;
        }, clientId, scopes != null && !scopes.isEmpty() ? String.join(" ", scopes) : null);
    }
} 