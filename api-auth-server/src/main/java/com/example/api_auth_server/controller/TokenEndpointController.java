package com.example.api_auth_server.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * 兼容旧版OAuth2端点的控制器
 * 将/oauth/token请求转发到/oauth2/token
 */
@RestController
public class TokenEndpointController {

    /**
     * 拦截/oauth/token请求并转发到/oauth2/token
     */
    @RequestMapping(value = "/oauth/token", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleTokenRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/oauth2/token");
        dispatcher.forward(request, response);
    }
}