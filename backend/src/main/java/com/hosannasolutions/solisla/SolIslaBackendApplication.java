package com.hosannasolutions.solisla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SolIslaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolIslaBackendApplication.class, args);
	}

}
