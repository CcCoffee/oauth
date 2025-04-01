#!/bin/bash

# 环境变量设置
API_CONSUMER_URL="http://localhost:8080"
AUTH_SERVER_URL="http://localhost:9000"
API_PROVIDER_URL="http://localhost:8090"
CLIENT_ID="messaging-client"
CLIENT_SECRET="secret"

# 彩色输出函数
print_info() {
  echo -e "\033[36m[信息]\033[0m $1"
}

print_success() {
  echo -e "\033[32m[成功]\033[0m $1"
}

print_error() {
  echo -e "\033[31m[错误]\033[0m $1"
}

print_separator() {
  echo -e "\033[33m-------------------------------------------\033[0m"
}

# 检查jq是否安装
if ! command -v jq &> /dev/null; then
  print_error "未找到jq工具，这个脚本需要jq来解析JSON。请安装jq后再运行此脚本。"
  print_info "安装命令: brew install jq (Mac) 或 apt-get install jq (Ubuntu/Debian)"
  exit 1
fi

print_separator
print_info "API消费者测试脚本 - 测试客户端凭证授权流程"
print_separator
echo ""

# 测试API信息端点
test_api_info() {
  print_info "1. 测试API信息端点"
  
  # 获取API信息
  API_INFO_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/info")
  
  # 检查是否成功获取信息
  if [ -z "$API_INFO_RESPONSE" ]; then
    print_error "获取API信息失败，请确保API消费者服务正在运行。"
    return 1
  fi
  
  print_success "已成功获取API信息！"
  echo "$API_INFO_RESPONSE" | jq .
  echo ""
  
  return 0
}

# 测试直接API访问端点
test_direct_api() {
  print_info "2. 测试直接API访问端点"
  
  # 访问直接API端点
  DIRECT_API_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/direct")
  
  # 检查是否成功获取响应
  if [ -z "$DIRECT_API_RESPONSE" ]; then
    print_error "访问直接API端点失败，请确保API消费者、授权服务器和API提供者都正在运行。"
    return 1
  fi
  
  # 检查响应是否包含错误信息
  if echo "$DIRECT_API_RESPONSE" | jq -e 'has("error")' > /dev/null; then
    print_error "访问直接API端点返回错误："
    echo "$DIRECT_API_RESPONSE" | jq .
    return 1
  fi
  
  print_success "已成功访问直接API端点！"
  echo "$DIRECT_API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# 测试客户端凭证授权API端点
test_oauth2_api() {
  print_info "3. 测试OAuth2客户端凭证授权API端点"
  
  # 访问OAuth2 API端点
  OAUTH2_API_RESPONSE=$(curl -s -X GET "${API_CONSUMER_URL}/api/test")
  
  # 检查是否成功获取响应
  if [ -z "$OAUTH2_API_RESPONSE" ]; then
    print_error "访问OAuth2 API端点失败，请确保API消费者、授权服务器和API提供者都正在运行。"
    return 1
  fi
  
  # 检查响应是否包含错误信息
  if echo "$OAUTH2_API_RESPONSE" | jq -e 'has("error")' > /dev/null; then
    print_error "访问OAuth2 API端点返回错误："
    echo "$OAUTH2_API_RESPONSE" | jq .
    return 1
  fi
  
  print_success "已成功访问OAuth2 API端点！"
  echo "$OAUTH2_API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# 直接测试授权服务器令牌端点
test_auth_server_token() {
  print_info "4. 直接测试授权服务器令牌端点"
  print_info "正在从授权服务器获取访问令牌..."

  # 获取访问令牌
  ACCESS_TOKEN_RESPONSE=$(curl -s -X POST -u "${CLIENT_ID}:${CLIENT_SECRET}" \
    "${AUTH_SERVER_URL}/oauth2/token" \
    -d "grant_type=client_credentials&scope=message.read" \
    -H "Content-Type: application/x-www-form-urlencoded")

  # 检查是否成功获取令牌
  if [ -z "$ACCESS_TOKEN_RESPONSE" ]; then
    print_error "获取令牌失败，请确保授权服务器正在运行。"
    return 1
  fi

  # 提取令牌
  ACCESS_TOKEN=$(echo $ACCESS_TOKEN_RESPONSE | jq -r '.access_token')
  
  if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    print_error "获取令牌失败，响应内容："
    echo $ACCESS_TOKEN_RESPONSE | jq .
    return 1
  fi

  print_success "已获取访问令牌！"
  print_info "令牌类型: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.token_type')"
  print_info "有效期: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.expires_in') 秒"
  print_info "作用域: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.scope')"
  
  # 显示令牌的前20个字符
  TOKEN_PREVIEW="${ACCESS_TOKEN:0:20}..."
  print_info "访问令牌 (部分): $TOKEN_PREVIEW"

  echo ""
  print_info "正在使用获取的令牌直接访问API提供者..."
  
  # 直接访问API提供者
  PROVIDER_API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # 检查API响应
  if [ -z "$PROVIDER_API_RESPONSE" ]; then
    print_error "访问API提供者失败，请确保API提供者正在运行。"
    return 1
  fi

  print_success "成功直接访问API提供者！响应内容："
  echo "$PROVIDER_API_RESPONSE" | jq .
  
  return 0
}

# 主函数
main() {
  # 检查API消费者是否可访问
  print_info "检查API消费者是否可访问..."
  if curl -s --head "${API_CONSUMER_URL}" >/dev/null; then
    print_success "API消费者可访问"
  else
    print_error "无法访问API消费者，请确保服务在 ${API_CONSUMER_URL} 上运行"
    exit 1
  fi

  # 检查授权服务器是否可访问
  print_info "检查授权服务器是否可访问..."
  if curl -s --head "${AUTH_SERVER_URL}" >/dev/null; then
    print_success "授权服务器可访问"
  else
    print_error "无法访问授权服务器，请确保服务器在 ${AUTH_SERVER_URL} 上运行"
    exit 1
  fi

  # 检查API提供者是否可访问
  print_info "检查API提供者是否可访问..."
  if curl -s --head "${API_PROVIDER_URL}" >/dev/null; then
    print_success "API提供者可访问"
  else
    print_error "无法访问API提供者，请确保服务器在 ${API_PROVIDER_URL} 上运行"
    exit 1
  fi

  echo ""
  
  # 运行测试
  test_api_info
  test_direct_api
  test_oauth2_api
  test_auth_server_token
  
  # 显示完成信息
  print_separator
  print_success "测试完成！"
  print_separator
}

# 执行主函数
main 