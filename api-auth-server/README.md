# OAuth2 Authorization Server

This is a demonstration project for an OAuth2 authorization server based on Spring Authorization Server, focusing on providing client credential authorization flow support.

## Project Architecture

This project is the authorization server component in the OAuth2 authorization architecture, used in conjunction with the resource server:

- **Authorization Server(api-auth-server)**: This project, responsible for issuing and validating OAuth2 tokens
- **API Provider(api-provider-demo)**: Resource server, providing API resources protected by OAuth2
- **API Consumer(api-consumer-demo)**: Client application, accessing API using client credentials

### Technology Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring Authorization Server

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.6+
- PostgreSQL database

### Generating the Keystore

Before running the project, you need to generate the keystore used for JWT signing:

```bash
# Generate RSA key pair and store it in a JKS keystore
keytool -genkeypair -alias myalias -keyalg RSA -keysize 2048 -storetype JKS -keystore mykeystore.jks -storepass mykeystorepass -keypass mykeypass -dname "CN=localhost, OU=Development, O=Example, L=City, S=State, C=CN"

# Move the generated keystore file to the project resources directory
mv mykeystore.jks api-auth-server/src/main/resources/
```

### Building the Project

```bash
mvn clean package
```

### Running the Project

```bash
mvn spring-boot:run
```

Or start using the JAR file:

```bash
java -jar target/api-auth-server-0.0.1-SNAPSHOT.jar
```

The service will start on port 9000.

## Authorization Server Endpoints

The project provides the following standard OAuth2 endpoints:

- **Authorization Endpoint**: `/oauth2/authorize`
- **Token Endpoint**: `/oauth2/token`
- **Token Revocation Endpoint**: `/oauth2/revoke`
- **Token Introspection Endpoint**: `/oauth2/introspect`
- **JWK Set Endpoint**: `/oauth2/jwks`
- **OpenID Connect Discovery Endpoint**: `/.well-known/openid-configuration`

## Client Credentials Authorization Flow

The authorization server supports the client credentials authorization flow, mainly used for server-to-server API access:

1. The client requests an access token on its own behalf (not on behalf of a user)
2. The authorization server verifies the client credentials (ID and secret)
3. After verification, the authorization server issues an access token
4. The client uses the access token to access protected API resources

## Preconfigured Clients

The authorization server has preconfigured the following OAuth2 clients:

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
Purpose: Token Introspection
Scope: introspection
```

## Cluster Deployment Guide

This project supports deployment in a multi-instance cluster environment, with key configurations including:

### 1. Database Configuration

All instances must connect to the same PostgreSQL database. Configure the database connection in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-db-host:5432/oauth
    username: your-username
    password: your-password
    driver-class-name: org.postgresql.Driver
```

### 2. Database Table Structure

Before the first start, ensure the PostgreSQL database has the required table structure. The project will automatically initialize the table structure, with the specific SQL script located at:
`src/main/resources/schema/oauth2-schema.sql`

### 3. Key Management

All cluster nodes share the same JWT signing key. The system automatically manages keys through the database table `oauth2_jwt_keys`:
- The system generates a new key and stores it in the database on the first start
- All subsequent instances will retrieve the same key from the database
- Keys are automatically synchronized across the cluster, no manual configuration required

### 4. Load Balancer Configuration

When using a load balancer, you need to modify the authorization server issuer URL:

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://your-load-balancer-domain
```

All resource servers also need to use the same issuer URL.

### 5. Session Sharing

This project is a stateless service, with all state stored in the database, no additional configuration for session sharing is required.

## Testing Token Acquisition

You can use the following commands to test the client credentials authorization flow:

### Obtaining an Opaque Token
```bash
curl -X POST \
  http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'opaque-client:opaque-secret' | base64)" \
  -d "grant_type=client_credentials&scope=message.read"
```

### Obtaining a JWT Token
```bash
curl -X POST \
  http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'jwt-client:jwt-secret' | base64)" \
  -d "grant_type=client_credentials&scope=message.read"
```

## JWT Token Format

The JWT tokens issued by the authorization server contain the following standard claims:

- `iss`：Issuer, value is the authorization server URL
- `sub`：Subject, value is the client ID
- `aud`：Audience, value is the resource server identifier
- `exp`：Expiration Time
- `iat`：Issued At
- `scope`：Scope

## Project Structure

```
api-auth-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_auth_server/
│   │   │               ├── ApiAuthServerApplication.java    # Application entry point
│   │   │               └── config/
│   │   │                   └── AuthServerConfig.java        # Authorization server configuration
│   │   └── resources/
│   │       └── application.properties                       # Application configuration
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── ApiAuthServerApplicationTests.java   # Test class
└── pom.xml                                                  # Maven configuration
```

## Security Considerations

- Ensure all communication is protected by HTTPS in production environments
- Client secrets should be kept confidential to prevent leakage
- Set token validity periods appropriately and rotate keys periodically
- Configure client permissions according to the principle of least privilege

## License

This project is licensed under the MIT License