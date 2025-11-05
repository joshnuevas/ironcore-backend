package com.ironcore.ironcorebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IronCoreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IronCoreBackendApplication.class, args);
	}

}