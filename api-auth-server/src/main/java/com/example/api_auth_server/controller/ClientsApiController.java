package com.example.api_auth_server.controller;

import com.example.api_auth_server.model.*;
import com.example.api_auth_server.repository.CustomJdbcRegisteredClientRepository;
import com.example.api_auth_server.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clients")
@PreAuthorize("hasRole('ADMIN')")
public class ClientsApiController {

    private final CustomJdbcRegisteredClientRepository registeredClientRepository;
    private final TokenService tokenService;

    public ClientsApiController(
            CustomJdbcRegisteredClientRepository registeredClientRepository,
            TokenService tokenService) {
        this.registeredClientRepository = registeredClientRepository;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<Void> createClient(@RequestBody ClientRequest request) {
        RegisteredClient.Builder builder = RegisteredClient.withId(request.getClientId())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .clientAuthenticationMethods(clientAuthenticationMethods -> 
                    clientAuthenticationMethods.addAll(List.of(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST
                    )))
                .authorizationGrantTypes(authorizationGrantTypes -> 
                    authorizationGrantTypes.addAll(request.getGrantTypes().stream()
                        .map(org.springframework.security.oauth2.core.AuthorizationGrantType::new)
                        .collect(Collectors.toList())));

        // 添加scope
        request.getScope().forEach(builder::scope);

        RegisteredClient registeredClient = builder
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(true)
                    .requireProofKey(true)
                    .build())
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(request.getExpiresIn()))
                    .build())
                .build();

        registeredClientRepository.save(registeredClient);
        return ResponseEntity.status(201).build();
    }

    @GetMapping
    public ResponseEntity<List<RegisteredClient>> getClients(@RequestParam(required = false) String resourceId) {
        if (resourceId != null && !resourceId.isEmpty()) {
            return ResponseEntity.ok(registeredClientRepository.findByResourceId(resourceId));
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<RegisteredClient> getClient(@PathVariable String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<Void> updateClient(
            @PathVariable String clientId,
            @RequestBody ClientUpdateRequest request) {
        RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
        if (existingClient == null) {
            return ResponseEntity.notFound().build();
        }

        RegisteredClient.Builder builder = RegisteredClient.from(existingClient)
                .authorizationGrantTypes(authorizationGrantTypes -> {
                    authorizationGrantTypes.clear();
                    authorizationGrantTypes.addAll(request.getGrantTypes().stream()
                        .map(org.springframework.security.oauth2.core.AuthorizationGrantType::new)
                        .collect(Collectors.toList()));
                });

        // 更新scope
        builder.scopes(scopes -> {
            scopes.clear();
            request.getScope().forEach(scopes::add);
        });

        RegisteredClient updatedClient = builder
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(request.getExpiresIn()))
                    .build())
                .build();

        registeredClientRepository.save(updatedClient);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        registeredClientRepository.deleteByClientId(clientId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{clientId}/secret")
    public ResponseEntity<Void> updateClientSecret(
            @PathVariable String clientId,
            @RequestBody ClientSecretUpdateRequest request) {
        RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
        if (existingClient == null) {
            return ResponseEntity.notFound().build();
        }

        RegisteredClient updatedClient = RegisteredClient.from(existingClient)
                .clientSecret(request.getClientSecret())
                .build();

        registeredClientRepository.save(updatedClient);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clientId}/tokens")
    public ResponseEntity<Void> deleteClientTokens(
            @PathVariable String clientId,
            @RequestParam(required = false) List<String> scope) {
        registeredClientRepository.deleteTokensByClientId(clientId, scope);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clientId}/tokens")
    public ResponseEntity<List<TokenInfo>> getClientTokens(
            @PathVariable String clientId,
            @RequestParam(required = false) List<String> scope) {
        return ResponseEntity.ok(tokenService.getTokensByClientId(clientId, scope));
    }
} 