package com.rustam.ccc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestCccApplication {
	public static void main(String[] args) {
		SpringApplication.from(CccApplication::main).with(TestCccApplication.class).run(args);
	}
}
