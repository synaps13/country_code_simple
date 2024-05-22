package com.rustam.ccc;

import com.rustam.ccc.service.CountryCodeParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CccApplication implements CommandLineRunner {
	private final CountryCodeParser parser;

	public CccApplication(CountryCodeParser parser) {
		this.parser = parser;
	}

	public static void main(String[] args) {
		SpringApplication.run(CccApplication.class, args);
	}

	// This also could have been done with @Scheduled or by handling an ApplicationReadyEvent
	@Override
	public void run(String... args) throws Exception {
		parser.parse();
	}
}
