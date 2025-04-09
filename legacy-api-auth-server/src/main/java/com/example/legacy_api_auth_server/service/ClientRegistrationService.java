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
     * Add a client to the database
     *
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param resourceIds Resource ID list
     * @param scope Authorization scope
     * @param authorizedGrantTypes Authorized grant types
     * @param webServerRedirectUri Redirect URI
     * @param authorities Authorities
     * @param accessTokenValidity Access token validity (seconds)
     * @param refreshTokenValidity Refresh token validity (seconds)
     * @param additionalInformation Additional information
     * @param autoApprove Auto approve
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

        // Check if the client already exists
        if (clientExists(clientId)) {
            throw new IllegalArgumentException("Client '" + clientId + "' already exists");
        }

        // Encrypt the client secret
        String encodedSecret = passwordEncoder.encode(clientSecret);

        // Insert new client
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
     * Check if the client exists
     *
     * @param clientId Client ID
     * @return Returns true if the client exists, otherwise false
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
     * Delete the client
     *
     * @param clientId Client ID
     * @return Whether the deletion was successful
     */
    public boolean removeClientDetails(String clientId) {
        int result = jdbcTemplate.update(
                "DELETE FROM oauth_client_details WHERE client_id = ?",
                clientId
        );
        return result > 0;
    }
} 