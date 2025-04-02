package com.example.api_auth_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ApiAuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAuthServerApplication.class, args);
	}

}
