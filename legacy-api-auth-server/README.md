# Legacy API Authorization Server Demo

This is a demonstration project for an old OAuth2 authorization server based on Spring Cloud OAuth2 2.1.0.RELEASE.

## Project Features

- Based on `spring-cloud-starter-oauth2:2.1.0.RELEASE`
- Provides JWT token issuance functionality
- Supports `/oauth/token_key` and `/oauth/check_token` interfaces
- Supports database storage of tokens
- Provides client management REST API

## Project Structure

```
src/main/java/com/example/legacy_api_auth_server/
├── LegacyApiAuthServerApplication.java  # Main application entry point
├── config/                              # Configuration classes
│   ├── AuthServerConfig.java           # Authorization server configuration
│   └── SecurityConfig.java             # Security configuration
├── controller/                          # Controllers
│   ├── ApiDocController.java           # API documentation controller
│   └── ClientManagementController.java  # Client management controller
└── service/                             # Services
    ├── ClientInitializationService.java # Client initialization service
    └── ClientRegistrationService.java   # Client registration service
```

## Database Tables

The project uses the following tables to store OAuth2 related data:

- `oauth_client_details`: Stores client details
- `oauth_access_token`: Stores access tokens
- `oauth_refresh_token`: Stores refresh tokens
- `oauth_code`: Stores authorization codes
- `oauth_approvals`: Stores authorization approval information

## Client Configuration

The system automatically initializes the following clients at startup:

1. `opaque-client`: Obtains opaque tokens using the client_credentials authorization type
   - Client secret: opaque-secret
   - Resource ID: legacy-api
   - Scope: message.read
   - Token validity period: 1 hour

2. `jwt-client`: Obtains JWT tokens using the client_credentials authorization type
   - Client secret: jwt-secret
   - Resource ID: legacy-api
   - Scope: message.read
   - Token validity period: 1 hour

## Client Management API

Provides the following REST API for managing OAuth2 clients:

1. Add client: 
   - URL: `/api/clients`
   - Method: POST
   - Authentication: Basic Auth (admin/admin123)
   - Parameters:
     - clientId: Client ID (required)
     - clientSecret: Client secret (required)
     - resourceIds: Resource ID (required)
     - scope: Authorization scope (required)
     - authorizedGrantTypes: Authorization types (required)
     - webServerRedirectUri: Redirect URI (optional)
     - authorities: Authorities (optional)
     - accessTokenValidity: Access token validity period (seconds) (optional)
     - refreshTokenValidity: Refresh token validity period (seconds) (optional)
     - additionalInformation: Additional information (optional)
     - autoApprove: Auto approve (optional)

2. Check if client exists:
   - URL: `/api/clients/{clientId}/exists`
   - Method: GET
   - Authentication: Basic Auth (admin/admin123)

3. Delete client:
   - URL: `/api/clients/{clientId}`
   - Method: DELETE
   - Authentication: Basic Auth (admin/admin123)

4. API documentation:
   - URL: `/api/docs/clients`
   - Method: GET
   - Authentication: Basic Auth (admin/admin123)

## JWT Configuration

Uses asymmetric keys for JWT token signing, with key information configured in `application.yml`.

## Endpoint Information

- `/oauth/token`: Obtains access tokens
- `/oauth/token_key`: Obtains JWT signing key
- `/oauth/check_token`: Validates token validity

## Running the Project

```bash
mvn spring-boot:run
```

Listens on port 9001 by default.

## Obtaining Tokens Example

Using client_credentials to obtain JWT tokens:

```bash
curl -X POST \
  http://localhost:9001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=message.read&client_id=jwt-client&client_secret=jwt-secret"
```

Using client_credentials to obtain opaque tokens:

```bash
curl -X POST \
  http://localhost:9001/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=message.read&client_id=opaque-client&client_secret=opaque-secret"
```

## Adding New Client Example

```bash
curl -X POST \
  http://localhost:9001/api/clients \
  -u admin:admin123 \
  -d "clientId=my-client&clientSecret=my-secret&resourceIds=legacy-api&scope=message.read&authorizedGrantTypes=client_credentials&accessTokenValidity=3600"
```

## Integrating with Resource Server

Update the resource server's `application.yml`, configuring the following properties:

```yaml
security:
  oauth2:
    resource:
      jwt:
        key-uri: http://localhost:9001/oauth/token_key
      token-info-uri: http://localhost:9001/oauth/check_token
```

This will allow the resource server to use this authorization server to validate tokens. 