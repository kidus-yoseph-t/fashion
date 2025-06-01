package com.project.Fashion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching // Enable Spring's caching capabilities
public class FashionApplication {

	public static void main(String[] args) {
		SpringApplication.run(FashionApplication.class, args);
	}

}