#!/bin/bash

# Environment variables
AUTH_SERVER_URL="http://localhost:9000"
API_PROVIDER_URL="http://localhost:8090"

# Opaque token client configuration
OPAQUE_CLIENT_ID="opaque-client"
OPAQUE_CLIENT_SECRET="opaque-secret"

# JWT token client configuration
JWT_CLIENT_ID="jwt-client"
JWT_CLIENT_SECRET="jwt-secret"

# Colored output functions
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
  print_error "jq tool not found, this script requires jq to parse JSON. Please install jq before running this script."
  print_info "Installation command: brew install jq (Mac) or apt-get install jq (Ubuntu/Debian)"
  exit 1
fi

print_separator
print_info "OAuth2.0 Test Script - Test Authorization Server and API Provider"
print_separator
echo ""

# 1. Test opaque token client credentials flow
test_opaque_client_credentials() {
  print_info "1. Test opaque token client credentials flow"
  print_info "Getting opaque access token from the authorization server..."

  # Get access token
  ACCESS_TOKEN_RESPONSE=$(curl -s -X POST -u "${OPAQUE_CLIENT_ID}:${OPAQUE_CLIENT_SECRET}" \
    "${AUTH_SERVER_URL}/oauth2/token" \
    -d "grant_type=client_credentials&scope=message.read" \
    -H "Content-Type: application/x-www-form-urlencoded")

  # Check if token is successfully obtained
  if [ -z "$ACCESS_TOKEN_RESPONSE" ]; then
    print_error "Failed to get token, please make sure the authorization server is running."
    return 1
  fi

  # Extract token
  ACCESS_TOKEN=$(echo $ACCESS_TOKEN_RESPONSE | jq -r '.access_token')
  
  if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    print_error "Failed to get token, response content:"
    echo $ACCESS_TOKEN_RESPONSE | jq .
    return 1
  fi

  print_success "Opaque access token obtained!"
  print_info "Token type: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.token_type')"
  print_info "Expires in: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.expires_in') seconds"
  print_info "Scope: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.scope')"
  
  # Display the first 20 characters of the token
  TOKEN_PREVIEW="${ACCESS_TOKEN:0:20}..."
  print_info "Access token (partial): $TOKEN_PREVIEW"

  echo ""
  print_info "Accessing protected API using opaque access token..."
  
  # Access protected API
  API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/opaque/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # Check API response
  if [ -z "$API_RESPONSE" ]; then
    print_error "Failed to access API, please make sure the API provider is running."
    return 1
  fi

  print_success "Successfully accessed protected API using opaque token! Response content:"
  echo "$API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# 2. Test JWT token client credentials flow
test_jwt_client_credentials() {
  print_info "2. Test JWT token client credentials flow"
  print_info "Getting JWT access token from the authorization server..."

  # Get access token
  ACCESS_TOKEN_RESPONSE=$(curl -s -X POST -u "${JWT_CLIENT_ID}:${JWT_CLIENT_SECRET}" \
    "${AUTH_SERVER_URL}/oauth2/token" \
    -d "grant_type=client_credentials&scope=message.read" \
    -H "Content-Type: application/x-www-form-urlencoded")

  # Check if token is successfully obtained
  if [ -z "$ACCESS_TOKEN_RESPONSE" ]; then
    print_error "Failed to get token, please make sure the authorization server is running."
    return 1
  fi

  # Extract token
  ACCESS_TOKEN=$(echo $ACCESS_TOKEN_RESPONSE | jq -r '.access_token')
  
  if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    print_error "Failed to get token, response content:"
    echo $ACCESS_TOKEN_RESPONSE | jq .
    return 1
  fi

  print_success "JWT access token obtained!"
  print_info "Token type: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.token_type')"
  print_info "Expires in: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.expires_in') seconds"
  print_info "Scope: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.scope')"
  
  # Display the first 20 characters of the token
  TOKEN_PREVIEW="${ACCESS_TOKEN:0:20}..."
  print_info "Access token (partial): $TOKEN_PREVIEW"

  echo ""
  print_info "Accessing protected API using JWT access token..."
  
  # Access protected API
  API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/jwt/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # Check API response
  if [ -z "$API_RESPONSE" ]; then
    print_error "Failed to access API, please make sure the API provider is running."
    return 1
  fi

  print_success "Successfully accessed protected API using JWT token! Response content:"
  echo "$API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# Main function
main() {
  # Check if the authorization server is accessible
  print_info "Checking if the authorization server is accessible..."
  if curl -s --head "${AUTH_SERVER_URL}" >/dev/null; then
    print_success "Authorization server is accessible"
  else
    print_error "Unable to access the authorization server, please make sure the server is running at ${AUTH_SERVER_URL}"
    exit 1
  fi

  # Check if the API provider is accessible
  print_info "Checking if the API provider is accessible..."
  if curl -s --head "${API_PROVIDER_URL}" >/dev/null; then
    print_success "API provider is accessible"
  else
    print_error "Unable to access the API provider, please make sure the server is running at ${API_PROVIDER_URL}"
    exit 1
  fi

  echo ""
  # Run client credentials flow tests
  test_opaque_client_credentials
  test_jwt_client_credentials
  
  # Display completion information
  print_separator
  print_success "Testing completed!"
  print_separator
}

# Execute the main function
main 