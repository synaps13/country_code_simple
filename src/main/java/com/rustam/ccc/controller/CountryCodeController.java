package com.rustam.ccc.controller;

import com.rustam.ccc.domain.CountryCode;
import com.rustam.ccc.service.CountryCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class CountryCodeController {
	private final CountryCodeService service;

	public CountryCodeController(CountryCodeService service) {
		this.service = service;
	}

	@GetMapping("/code")
	// There is currently no need to create separate classes for the request and response bodies
	// as the data is simple and does not require any additional processing.
	// whenever the need for hiding or extending the data exposed to consumers arises, then separate classes
	// should be created. I would do that a subdirs of controller package `request` and `response` for example.
	public Flux<CountryCode> getCode(@Valid @NotBlank @Pattern(regexp = "[\\d- ]{3,50}") String phoneNumber) {
		return service.processPhoneNumberAndGetCountryDetails(phoneNumber);
	}
}
