package com.example.legacy_api_auth_server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClientRegistrationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 向数据库添加客户端
     *
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @param resourceIds 资源ID列表
     * @param scope 授权范围
     * @param authorizedGrantTypes 授权类型
     * @param webServerRedirectUri 重定向URI
     * @param authorities 权限
     * @param accessTokenValidity 访问令牌有效期（秒）
     * @param refreshTokenValidity 刷新令牌有效期（秒）
     * @param additionalInformation 附加信息
     * @param autoApprove 自动批准
     */
    public void addClientDetails(
            String clientId,
            String clientSecret,
            String resourceIds,
            String scope,
            String authorizedGrantTypes,
            String webServerRedirectUri,
            String authorities,
            Integer accessTokenValidity,
            Integer refreshTokenValidity,
            String additionalInformation,
            String autoApprove) {

        // 检查客户端是否已存在
        if (clientExists(clientId)) {
            throw new IllegalArgumentException("客户端 '" + clientId + "' 已存在");
        }

        // 加密客户端密钥
        String encodedSecret = passwordEncoder.encode(clientSecret);

        // 插入新客户端
        jdbcTemplate.update(
                "INSERT INTO oauth_client_details " +
                        "(client_id, resource_ids, client_secret, scope, authorized_grant_types, " +
                        "web_server_redirect_uri, authorities, access_token_validity, refresh_token_validity, " +
                        "additional_information, autoapprove) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clientId,
                resourceIds,
                encodedSecret,
                scope,
                authorizedGrantTypes,
                webServerRedirectUri,
                authorities,
                accessTokenValidity,
                refreshTokenValidity,
                additionalInformation,
                autoApprove
        );
    }

    /**
     * 检查客户端是否存在
     *
     * @param clientId 客户端ID
     * @return 如果客户端存在返回true，否则返回false
     */
    public boolean clientExists(String clientId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth_client_details WHERE client_id = ?",
                Integer.class,
                clientId
        );
        return count != null && count > 0;
    }

    /**
     * 删除客户端
     *
     * @param clientId 客户端ID
     * @return 是否成功删除
     */
    public boolean removeClientDetails(String clientId) {
        int result = jdbcTemplate.update(
                "DELETE FROM oauth_client_details WHERE client_id = ?",
                clientId
        );
        return result > 0;
    }
} 