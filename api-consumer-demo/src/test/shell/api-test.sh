#!/bin/bash

# Environment variable settings
API_CONSUMER_URL="http://localhost:8080"
AUTH_SERVER_URL="http://localhost:9000"
API_PROVIDER_URL="http://localhost:8090"

# Opaque token client configuration
OPAQUE_CLIENT_ID="opaque-client"
OPAQUE_CLIENT_SECRET="opaque-secret"

# JWT token client configuration
JWT_CLIENT_ID="jwt-client"
JWT_CLIENT_SECRET="jwt-secret"

# Color output functions
print_info() {
  echo -e "\033[36m[Info]\033[0m $1"
}

print_success() {
  echo -e "\033[32m[Success]\033[0m $1"
}

print_error() {
  echo -e "\033[31m[Error]\033[0m $1"
}

print_separator() {
  echo -e "\033[33m-------------------------------------------\033[0m"
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
  print_error "jq tool not found, this script requires jq to parse JSON. Please install jq and run this script again."
  print_info "Installation command: brew install jq (Mac) or apt-get install jq (Ubuntu/Debian)"
  exit 1
fi

print_separator
print_info "API Consumer Test Script - Testing Client Credential Authorization Flow"
print_separator
echo ""

# Test API information endpoint
test_api_info() {
  print_info "1. Testing API Information Endpoint"
  
  # Get API information
  API_INFO_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/info")
  
  # Check if information was successfully retrieved
  if [ -z "$API_INFO_RESPONSE" ]; then
    print_error "Failed to retrieve API information, ensure the API consumer service is running."
    return 1
  fi
  
  print_success "Successfully retrieved API information!"
  echo "$API_INFO_RESPONSE" | jq .
  echo ""
  
  return 0
}

# Test direct API access endpoint
test_direct_api() {
  print_info "2. Testing Direct API Access Endpoint"
  
  # Access direct API endpoint
  DIRECT_API_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/direct")
  
  # Check if response was successfully retrieved
  if [ -z "$DIRECT_API_RESPONSE" ]; then
    print_error "Failed to access direct API endpoint, ensure the API consumer, authorization server, and API provider are all running."
    return 1
  fi
  
  # Check if response contains error information
  if echo "$DIRECT_API_RESPONSE" | jq -e 'has("error")' > /dev/null; then
    print_error "Accessing direct API endpoint returned error:"
    echo "$DIRECT_API_RESPONSE" | jq .
    return 1
  fi
  
  print_success "Successfully accessed direct API endpoint!"
  echo "$DIRECT_API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# Test OAuth2 client credential authorization API endpoint (using opaque token)
test_oauth2_opaque_api() {
  print_info "3. Testing OAuth2 Client Credential Authorization API Endpoint (Opaque Token)"
  
  # Access OAuth2 API endpoint
  OAUTH2_API_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/opaque")
  
  # Check if response was successfully retrieved
  if [ -z "$OAUTH2_API_RESPONSE" ]; then
    print_error "Failed to access OAuth2 API endpoint, ensure the API consumer, authorization server, and API provider are all running."
    return 1
  fi
  
  # Check if response contains error information
  if echo "$OAUTH2_API_RESPONSE" | jq -e 'has("error")' > /dev/null; then
    print_error "Accessing OAuth2 API endpoint returned error:"
    echo "$OAUTH2_API_RESPONSE" | jq .
    return 1
  fi
  
  print_success "Successfully accessed OAuth2 API endpoint (Opaque Token)!"
  echo "$OAUTH2_API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# Test OAuth2 client credential authorization API endpoint (using JWT token)
test_oauth2_jwt_api() {
  print_info "4. Testing OAuth2 Client Credential Authorization API Endpoint (JWT Token)"
  
  # Access OAuth2 API endpoint
  OAUTH2_API_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/jwt")
  
  # Check if response was successfully retrieved
  if [ -z "$OAUTH2_API_RESPONSE" ]; then
    print_error "Failed to access OAuth2 API endpoint, ensure the API consumer, authorization server, and API provider are all running."
    return 1
  fi
  
  # Check if response contains error information
  if echo "$OAUTH2_API_RESPONSE" | jq -e 'has("error")' > /dev/null; then
    print_error "Accessing OAuth2 API endpoint returned error:"
    echo "$OAUTH2_API_RESPONSE" | jq .
    return 1
  fi
  
  print_success "Successfully accessed OAuth2 API endpoint (JWT Token)!"
  echo "$OAUTH2_API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# Directly test authorization server token endpoint
test_auth_server_token() {
  print_info "5. Directly Testing Authorization Server Token Endpoint"
  print_info "Attempting to get access token from authorization server..."

  # Get access token
  ACCESS_TOKEN_RESPONSE=$(curl -s -X POST -u "${OPAQUE_CLIENT_ID}:${OPAQUE_CLIENT_SECRET}" \
    "${AUTH_SERVER_URL}/oauth2/token" \
    -d "grant_type=client_credentials&scope=message.read" \
    -H "Content-Type: application/x-www-form-urlencoded")

  # Check if token was successfully retrieved
  if [ -z "$ACCESS_TOKEN_RESPONSE" ]; then
    print_error "Failed to get token, ensure the authorization server is running."
    return 1
  fi

  # Extract token
  ACCESS_TOKEN=$(echo $ACCESS_TOKEN_RESPONSE | jq -r '.access_token')
  
  if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    print_error "Failed to get token, response content:"
    echo $ACCESS_TOKEN_RESPONSE | jq .
    return 1
  fi

  print_success "Access token obtained!"
  print_info "Token type: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.token_type')"
  print_info "Expires in: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.expires_in') seconds"
  print_info "Scope: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.scope')"
  
  # Display the first 20 characters of the token
  TOKEN_PREVIEW="${ACCESS_TOKEN:0:20}..."
  print_info "Access token (partial): $TOKEN_PREVIEW"

  echo ""
  print_info "Using the obtained token to directly access the API provider..."
  
  # Directly access the API provider
  PROVIDER_API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/opaque/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # Check API response
  if [ -z "$PROVIDER_API_RESPONSE" ]; then
    print_error "Failed to access API provider, ensure the API provider is running."
    return 1
  fi

  print_success "Successfully directly accessed API provider! Response content:"
  echo "$PROVIDER_API_RESPONSE" | jq .
  
  return 0
}

# Main function
main() {
  # Check if API consumer is accessible
  print_info "Checking if API consumer is accessible..."
  if curl -s --head "${API_CONSUMER_URL}" >/dev/null; then
    print_success "API consumer is accessible"
  else
    print_error "Unable to access API consumer, ensure the service is running at ${API_CONSUMER_URL}"
    exit 1
  fi

  # Check if authorization server is accessible
  print_info "Checking if authorization server is accessible..."
  if curl -s --head "${AUTH_SERVER_URL}" >/dev/null; then
    print_success "Authorization server is accessible"
  else
    print_error "Unable to access authorization server, ensure the service is running at ${AUTH_SERVER_URL}"
    exit 1
  fi

  # Check if API provider is accessible
  print_info "Checking if API provider is accessible..."
  if curl -s --head "${API_PROVIDER_URL}" >/dev/null; then
    print_success "API provider is accessible"
  else
    print_error "Unable to access API provider, ensure the service is running at ${API_PROVIDER_URL}"
    exit 1
  fi

  echo ""
  
  # Run tests
  test_api_info
  test_direct_api
  test_oauth2_opaque_api
  test_oauth2_jwt_api
  test_auth_server_token
  
  # Display completion information
  print_separator
  print_success "Testing completed!"
  print_separator
}

# Execute main function
main