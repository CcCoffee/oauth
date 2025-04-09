# API Consumer Demo Project

This is a Spring Boot based OAuth2 client demo project, demonstrating how to use the client credentials grant flow to access protected API resources.

## Project Architecture

This project is a client component in the OAuth2 authorization architecture, working with the authorization server and resource server:

- **Authorization Server (api-auth-server)**: Responsible for user authentication and issuing access tokens
- **Resource Server (api-provider-demo)**: Provides OAuth2 protected API resources
- **Client (api-consumer-demo)**: This project, using OAuth2 to access protected API resources

### Tech Stack

- Java 21
- Spring Boot 3.4.4
- Spring Security
- Spring OAuth2 Client
- WebClient

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.6+
- Running Authorization Server (default port 9000)
- Running Resource Server (default port 8090)

### Build the Project

```bash
mvn clean package
```

### Run the Project

```bash
mvn spring-boot:run
```

Or start with the JAR file:

```bash
java -jar target/api-consumer-demo-0.0.1-SNAPSHOT.jar
```

The application will start on port 8080.

## API Endpoints

This project provides the following REST API endpoints:

### OAuth2 Endpoints
1. **GET /api/opaque**
   - Access protected resources using opaque token
   - Automatically handle token acquisition and API calls
   - Return attribute information of the opaque token

2. **GET /api/jwt**
   - Access protected resources using JWT token
   - Automatically handle token acquisition and API calls
   - Return claim information of the JWT token

### Other Endpoints
1. **GET /api/direct**
   - Directly access protected resources using opaque token
   - Demonstrate manual token acquisition and API calls

2. **GET /api/info**
   - Return basic service information
   - No authorization required to access

## Project Structure

```
api-consumer-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── api_consumer_demo/
│   │   │               ├── ApiConsumerDemoApplication.java     # Application entry
│   │   │               ├── config/
│   │   │               │   ├── SecurityConfig.java             # Security configuration
│   │   │               │   └── WebClientConfig.java            # WebClient configuration
│   │   │               ├── controller/
│   │   │               │   └── ApiController.java              # API controller
│   │   │               └── service/
│   │   │                   └── ApiService.java                 # API service
│   │   └── resources/
│   │       └── application.yml                                 # Application configuration
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java                            # Test class
│       └── shell/
│           └── api-test.sh                                     # API test script
└── pom.xml                                                     # Maven configuration
```

## Configuration

The main configuration file is located at `src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          opaque-client:                                # Opaque token client configuration
            client-id: opaque-client
            client-secret: opaque-secret
            authorization-grant-type: client_credentials
            scope: message.read
            provider: spring
          jwt-client:                                   # JWT token client configuration
            client-id: jwt-client
            client-secret: jwt-secret
            authorization-grant-type: client_credentials
            scope: message.read
            provider: spring
        provider:
          spring:
            token-uri: http://localhost:9000/oauth2/token
```

## Client Credentials Flow

This project supports two types of token in the client credentials flow:

### Opaque Token Flow
1. The client uses the `opaque-client` credential to request an opaque access token
2. The authorization server validates the client and issues an opaque token
3. The client uses this token to access the `/api/opaque/message` endpoint
4. The resource server validates the token through token introspection

### JWT Token Flow
1. The client uses the `jwt-client` credential to request a JWT access token
2. The authorization server validates the client and issues a JWT token
3. The client uses this token to access the `/api/jwt/message` endpoint
4. The resource server directly validates the JWT token

## Testing Methods

### Using the Test Script

The project includes a shell script that can automatically test all API endpoints:

```bash
cd src/test/shell
chmod +x api-test.sh
./api-test.sh
```

This script will execute the following tests:
1. Test the API info endpoint
2. Test the direct API access endpoint
3. Test the OAuth2 opaque token endpoint
4. Test the OAuth2 JWT token endpoint
5. Directly test the authorization server token endpoint

### Using the curl Command

You can also manually test the API endpoints using the curl command:

```bash
# Test the info endpoint
curl -v http://localhost:8080/api/info

# Test the opaque token endpoint
curl -v http://localhost:8080/api/opaque

# Test the JWT token endpoint
curl -v http://localhost:8080/api/jwt

# Test the direct access endpoint
curl -v http://localhost:8080/api/direct
```

## Preconfigured Clients

Use the following OAuth2 clients to access the authorization server:

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

## Notes

- Ensure that the authorization server and resource server are running and accessible
- Use HTTPS to protect all communication in production
- Safeguard client credentials to avoid leakage
- Regularly update client secrets
- Use the appropriate token type:
  - Opaque Token: For scenarios that require central validation
  - JWT Token: For scenarios that require distributed validation
