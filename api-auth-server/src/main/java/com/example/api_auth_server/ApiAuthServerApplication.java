package com.example.api_auth_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties
@EnableAspectJAutoProxy
public class ApiAuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAuthServerApplication.class, args);
	}

}
