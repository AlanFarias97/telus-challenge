package com.challenge.telus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TelusApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelusApplication.class, args);
	}

}
