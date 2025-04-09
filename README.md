# OAuth2 Authorization Service Architecture Example

This is an example project based on Spring Boot and Spring Security, showcasing a complete OAuth2 authorization service architecture.

## Project Structure

This project consists of the following three main components:

- **[api-auth-server](api-auth-server/README.md)**: OAuth2 authorization server, responsible for authentication and issuing access tokens
- **[api-provider-demo](api-provider-demo/README.md)**: OAuth2 resource server, providing protected API resources
- **[api-consumer-demo](api-consumer-demo/README.md)**: OAuth2 client, demonstrating how to obtain tokens and access protected resources

## Technology Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Authorization Server
- Spring OAuth2 Resource Server
- Spring OAuth2 Client

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.6+

### Start All Services

1. First, start the authorization server:

```bash
cd api-auth-server
mvn spring-boot:run
```

The authorization server will start on port 9000.

2. Then, start the resource server:

```bash
cd api-provider-demo
mvn spring-boot:run
```

The resource server will start on port 8090.

3. Finally, start the client:

```bash
cd api-consumer-demo
mvn spring-boot:run
```

The client will start on port 8080.

## Test API Access

### Using Test Scripts

Run the provided test scripts to validate all functionalities:

```bash
cd api-consumer-demo/src/test/shell
./api-test.sh
```

This script will test:
1. API information endpoint
2. Direct API access
3. OAuth2 opaque token endpoint
4. OAuth2 JWT token endpoint
5. Authorization server token endpoint

### Manual Endpoint Testing

Alternatively, manually access the following endpoints:

```
http://localhost:8080/api/opaque  # Access using opaque token
http://localhost:8080/api/jwt     # Access using JWT token
http://localhost:8080/api/info    # View service information
```

## Preconfigured Clients

The system has preconfigured the following OAuth2 clients:

### Opaque Token Client
```
Client ID: opaque-client
Client Secret: opaque-secret
Authorization Type: client_credentials
Scope: message.read
Token Format: Opaque Token
Validity Period: 30 minutes
```

### JWT Token Client
```
Client ID: jwt-client
Client Secret: jwt-secret
Authorization Type: client_credentials
Scope: message.read
Token Format: JWT
Validity Period: 1 hour
```

### Resource Server Client
```
Client ID: resource-server
Client Secret: secret
Purpose: Token introspection
Scope: introspection
```

## Service Ports

- Authorization Server: 9000
- Resource Server: 8090
- Client: 8080
