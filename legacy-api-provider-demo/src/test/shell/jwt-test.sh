#!/bin/bash

# 环境变量设置
AUTH_SERVER_URL="http://localhost:9001"
API_PROVIDER_URL="http://localhost:8091"

# JWT令牌客户端配置
JWT_CLIENT_ID="jwt-client"
JWT_CLIENT_SECRET="jwt-secret"

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
print_info "JWT令牌测试脚本 - 测试JWT消息API"
print_separator
echo ""

# 测试JWT令牌客户端凭证授权流程
test_jwt_message_api() {
  print_info "正在从授权服务器获取JWT访问令牌..."

  # 获取访问令牌
  ACCESS_TOKEN_RESPONSE=$(curl -s -X POST -u "${JWT_CLIENT_ID}:${JWT_CLIENT_SECRET}" \
    "${AUTH_SERVER_URL}/oauth/token" \
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

  print_success "已获取JWT访问令牌！"
  print_info "令牌类型: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.token_type')"
  print_info "有效期: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.expires_in') 秒"
  print_info "作用域: $(echo $ACCESS_TOKEN_RESPONSE | jq -r '.scope')"
  
  # 显示令牌的前20个字符
  TOKEN_PREVIEW="${ACCESS_TOKEN:0:20}..."
  print_info "访问令牌 (部分): $TOKEN_PREVIEW"

  echo ""
  print_info "正在使用JWT访问令牌访问受保护的API..."
  
  # 访问受保护的API
  API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/jwt/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # 检查API响应
  if [ -z "$API_RESPONSE" ]; then
    print_error "访问API失败，请确保API提供者正在运行。"
    return 1
  fi

  print_success "成功使用JWT令牌访问受保护的API！响应内容："
  echo "$API_RESPONSE" | jq .
  echo ""
  
  return 0
}

# 主函数
main() {
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
  # 运行JWT消息API测试
  test_jwt_message_api
  
  # 显示完成信息
  print_separator
  print_success "测试完成！"
  print_separator
}

# 执行主函数
main 