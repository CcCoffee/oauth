package com.example.api_auth_server.service;

import com.example.api_auth_server.model.TokenInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    /**
     * 根据token值查找对应的客户端ID
     * @param tokenValue token值
     * @return 客户端ID，如果未找到则返回empty
     */
    public Optional<String> findClientIdByToken(String tokenValue) {
        try {
            String sql = "SELECT registered_client_id FROM oauth2_authorization WHERE access_token_value = ?";
            String clientId = jdbcTemplate.queryForObject(sql, String.class, tokenValue);
            return Optional.ofNullable(clientId);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据token值删除token
     * @param tokenValue token值
     * @return 是否删除成功
     */
    public boolean deleteTokenByValue(String tokenValue) {
        String sql = "DELETE FROM oauth2_authorization WHERE access_token_value = ?";
        int rowsAffected = jdbcTemplate.update(sql, tokenValue);
        return rowsAffected > 0;
    }

    /**
     * 验证token是否存在且有效
     * @param tokenValue token值
     * @return 是否存在且有效
     */
    public boolean isTokenValid(String tokenValue) {
        try {
            String sql = "SELECT COUNT(*) FROM oauth2_authorization WHERE access_token_value = ? AND access_token_expires_at > CURRENT_TIMESTAMP";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenValue);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
} 