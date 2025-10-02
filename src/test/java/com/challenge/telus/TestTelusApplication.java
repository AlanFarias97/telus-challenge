package com.challenge.telus;

import org.springframework.boot.SpringApplication;

public class TestTelusApplication {

	public static void main(String[] args) {
		SpringApplication.from(TelusApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
