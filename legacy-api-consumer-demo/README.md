# Legacy API Consumer Demo

This is a sample application using the old Spring Cloud OAuth2 client to demonstrate how to interact with OAuth2 protected APIs.

## Technology Stack

- Spring Boot 2.6.4
- Spring Security
- Spring Cloud OAuth2 (2.2.0.RELEASE)
- Java 11

## Features

- Uses client credentials flow to obtain OAuth2 tokens
- Uses JWT tokens to access protected resources
- Provides RESTful interfaces to demonstrate interaction with protected resources

## API Documentation

The application provides the following API endpoints:

- `GET /api/jwt`: Accesses protected API using JWT token
- `GET /api/info`: Retrieves service information

## Configuration Explanation

The configuration file `application.yml` includes the following key configurations:

```yaml
oauth2:
  server:
    token-uri: http://localhost:9001/oauth/token  # OAuth2 server token endpoint
  client:
    jwt:
      client-id: jwt-client         # JWT token client ID
      client-secret: jwt-secret     # JWT token client secret

api:
  server:
    url: http://localhost:8091      # API server URL
```

## Running Instructions

1. Ensure Java 11 or higher is installed
2. Ensure the OAuth2 authorization server is running
3. Ensure the API provider service is running
4. Use the following command to build and run the application:

```bash
mvn clean package
java -jar target/legacy-api-consumer-demo-0.0.1-SNAPSHOT.jar
```

Or run directly using Maven:

```bash
mvn spring-boot:run
```

The application will start on port 8081. 