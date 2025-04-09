# API Provider Demo Project

This is a Spring Boot-based OAuth2 resource server demo project, demonstrating how to create API endpoints protected by client credentials authorization.

## Project Architecture

This project is the resource server component in the OAuth2 authorization architecture, working in conjunction with the authorization server:

- **Authorization Server (api-auth-server)**: responsible for issuing and validating OAuth2 tokens
- **API Provider (api-provider-demo)**: this project, providing API resources protected by OAuth2
- **API Consumer (api-consumer-demo)**: client application, accessing API using client credentials

### Technology Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring OAuth2 Resource Server

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.6+
- Authorization server running (default port 9000)

### Building the Project

```bash
mvn clean package
```

### Running the Project

```bash
mvn spring-boot:run
```

Or use the JAR file to start:

```bash
java -jar target/api-provider-demo-0.0.1-SNAPSHOT.jar
```

The service will start on port 8090.

## API Endpoints

The project provides the following API endpoints:

### Opaque Token Endpoint
- **GET /api/opaque/message**: API endpoint protected by opaque token
- Requires a valid opaque access token
- Returns token attribute information and message

### JWT Token Endpoint
- **GET /api/jwt/message**: API endpoint protected by JWT token
- Requires a valid JWT access token
- Returns JWT claim information and message

## Security Configuration

The resource server is configured with two security mechanisms:

### Opaque Token Configuration
```java
.securityMatcher("/api/opaque/**")
.oauth2ResourceServer(oauth2 -> oauth2
    .opaqueToken(opaque -> opaque
        .introspectionUri("http://localhost:9000/oauth2/introspect")
        .introspectionClientCredentials("resource-server", "secret"))
)
```

### JWT Token Configuration
```java
.securityMatcher("/api/jwt/**")
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(Customizer.withDefaults())
)
```

## Configuration Explanation

The main configuration file is located at `src/main/resources/application.yml`:

```yaml
server:
  port: 8090  # Project port

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000  # Authorization server address
```

## Client Credentials Flow

The API provider supports two token validation flows:

### Opaque Token Flow
1. Client carries opaque token to access API
2. Resource server uses `resource-server` credentials to call introspection endpoint
3. Authorization server validates token and returns token information
4. Resource server validates permissions and processes request

### JWT Token Flow
1. Client carries JWT token to access API
2. Resource server uses public key to validate JWT signature
3. Resource server validates JWT claims (expiration time, scope, etc.)
4. After validation, processes request

## Testing API

You can use curl commands to test API endpoints:

### Testing Opaque Token Endpoint
```bash
# First, get an opaque access token
curl -X POST -u "opaque-client:opaque-secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"

# Use the obtained token to access API
curl -X GET "http://localhost:8090/api/opaque/message" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Testing JWT Token Endpoint
```bash
# First, get a JWT access token
curl -X POST -u "jwt-client:jwt-secret" \
  "http://localhost:9000/oauth2/token" \
  -d "grant_type=client_credentials&scope=message.read" \
  -H "Content-Type: application/x-www-form-urlencoded"

# Use the obtained token to access API
curl -X GET "http://localhost:8090/api/jwt/message" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Project Structure

```
api-provider-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_provider_demo/
│   │   │               ├── ApiProviderDemoApplication.java    # Application entry point
│   │   │               ├── config/
│   │   │               │   └── SecurityConfig.java            # Security configuration
│   │   │               └── controller/
│   │   │                   └── ApiController.java             # API controller
│   │   └── resources/
│   │       └── application.yml                                # Application configuration
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java                           # Test class
└── pom.xml                                                    # Maven configuration
```

## Token Validation

The resource server validates the following information for tokens:

### Opaque Token Validation
- Token is active
- Token is within its validity period
- Token has the required scope
- Token's client ID is correct

### JWT Token Validation
- Token signature is valid
- Issuer is correct
- Token is within its validity period
- Token includes necessary scope

## Notes

- Ensure the authorization server is running and accessible
- Use HTTPS to protect API communication in production
- Configure CORS appropriately to support client requests from different origins
- Periodically update JWT signing keys
