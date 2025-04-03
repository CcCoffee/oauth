# Legacy API Provider Demo

这是一个基于Spring Cloud OAuth2 2.1.0.RELEASE的旧版OAuth2资源服务器示例项目。

## 项目特点

- 基于`spring-cloud-starter-oauth2:2.1.0-RELEASE`
- 提供`/jwt/message` API端点
- 配置了`security.oauth2.resource`相关属性，包括`jwt.key-uri`

## 项目结构

```
src/main/java/com/example/legacy_api_provider_demo/
├── LegacyApiProviderDemoApplication.java  # 主应用程序入口
├── config/                                # 配置类
│   └── ResourceServerConfig.java         # 资源服务器配置
└── controller/                           # 控制器
    └── ApiController.java                # API控制器
```

## 配置说明

该项目使用了Spring Cloud OAuth2 2.1.0.RELEASE，相比新版本的OAuth2实现有以下不同：

1. 使用`@EnableResourceServer`注解来启用资源服务器功能
2. 使用`ResourceServerConfigurerAdapter`来配置资源服务器
3. 使用`security.oauth2.resource`相关配置而不是Spring Boot 2.x以上版本的`spring.security.oauth2.resourceserver`

## JWT配置

在`application.yml`中配置了：

```yaml
security:
  oauth2:
    resource:
      jwt:
        key-uri: http://localhost:9001/oauth/token_key
```

这将允许资源服务器从授权服务器获取公钥来验证JWT令牌。

## 运行项目

```bash
mvn spring-boot:run
```

服务启动后，可以通过带有有效JWT令牌的请求访问`http://localhost:8091/jwt/message`接口。 