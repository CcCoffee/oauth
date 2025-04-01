#!/bin/bash

# 环境变量设置
AUTH_SERVER_URL="http://localhost:9000"
API_PROVIDER_URL="http://localhost:8090"
CLIENT_ID="messaging-client"
CLIENT_SECRET="secret"
REDIRECT_URI="http://127.0.0.1:8080/authorized"

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
print_info "OAuth2.0测试脚本 - 测试授权服务器和API提供者"
print_separator
echo ""

# 1. 客户端凭证授权流程
test_client_credentials() {
  print_info "1. 测试客户端凭证授权流程"
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
  print_info "正在使用访问令牌访问受保护的API..."
  
  # 访问受保护的API
  API_RESPONSE=$(curl -s -X GET \
    "${API_PROVIDER_URL}/api/message" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")

  # 检查API响应
  if [ -z "$API_RESPONSE" ]; then
    print_error "访问API失败，请确保API提供者正在运行。"
    return 1
  fi

  print_success "成功访问受保护的API！响应内容："
  echo "$API_RESPONSE" | jq .
  
  return 0
}

# 2. 授权码授权流程的说明（无法在脚本中完全自动化）
show_authorization_code_instructions() {
  print_separator
  print_info "2. 授权码授权流程说明"
  print_separator
  
  AUTH_URL="${AUTH_SERVER_URL}/oauth2/authorize?response_type=code&client_id=${CLIENT_ID}&scope=message.read%20openid&redirect_uri=${REDIRECT_URI}"
  
  echo ""
  print_info "授权码流程需要用户交互，无法在此脚本中完全自动化。请按照以下步骤手动操作："
  echo ""
  print_info "步骤 1: 在浏览器中打开以下URL获取授权码"
  echo "$AUTH_URL"
  echo ""
  print_info "步骤 2: 登录并授权应用访问"
  print_info "用户名: user"
  print_info "密码: password"
  echo ""
  print_info "步骤 3: 授权后，您将被重定向到类似的URL:"
  echo "${REDIRECT_URI}?code=YOUR_AUTHORIZATION_CODE"
  echo ""
  print_info "步骤 4: 复制code参数的值，然后运行以下命令获取访问令牌:"
  echo "curl -X POST -u \"${CLIENT_ID}:${CLIENT_SECRET}\" \\"
  echo "  \"${AUTH_SERVER_URL}/oauth2/token\" \\"
  echo "  -d \"grant_type=authorization_code&code=YOUR_AUTHORIZATION_CODE&redirect_uri=${REDIRECT_URI}\" \\"
  echo "  -H \"Content-Type: application/x-www-form-urlencoded\""
  echo ""
  print_info "步骤 5: 使用获取的access_token访问API:"
  echo "curl -X GET \"${API_PROVIDER_URL}/api/message\" -H \"Authorization: Bearer YOUR_ACCESS_TOKEN\""
  echo ""
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
  # 运行客户端凭证流程测试
  test_client_credentials
  
  # 显示授权码流程说明
  show_authorization_code_instructions
}

# 执行主函数
main 