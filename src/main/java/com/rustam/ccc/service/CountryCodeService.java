package com.rustam.ccc.service;

import com.rustam.ccc.controller.NotFoundException;
import com.rustam.ccc.domain.CountryCode;
import com.rustam.ccc.persistence.CountryCodePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class CountryCodeService {
	private final CountryCodePersistence persistence;
	private final static int LONGEST_CODE_LENGTH = 2 + 5; // 2 for country code, 5 for code specification
	private final Logger log = LoggerFactory.getLogger(CountryCodeService.class);

	public CountryCodeService(CountryCodePersistence persistence) {
		this.persistence = persistence;
	}

	public void saveToDB(Collection<CountryCode> codes) {
		persistence.addCountryCodes(codes).subscribe();
	}

	/**
	 * Validate that input is a valid phone number and return the country details with code
	 * @param phoneNumber that's a user input string, be careful here
	 * @return CountryCode domain object if the phone number is valid and country is known
	 */
	public Flux<CountryCode> processPhoneNumberAndGetCountryDetails(String phoneNumber) {
		// Many of "interesting" validations of user input
		if (phoneNumber.isBlank()) {
			throw new IllegalArgumentException("Phone number cannot be empty");
		}
		var phoneNumberOnly = phoneNumber.replaceAll("\\D", "");
		var cutLength = Math.min(phoneNumberOnly.length(), LONGEST_CODE_LENGTH);
		return persistence.findByCode(getSearchCodes(phoneNumberOnly.substring(0, cutLength)))
				.switchIfEmpty(Flux.error(new NotFoundException("Country code not found")));
	}

	/**
	 * That definitely may be improved. Wanted to do in functional style with reducer and recursive call like in Haskell or Rust
	 * but honestly not often use such things in Java, and it's not that readable for most of the developers.
	 */
	private List<Integer> getSearchCodes(String numberStr) {
		var acc = new ArrayList<Integer>();
		numberStr = numberStr.trim();
		while (!numberStr.isEmpty()) {
			try {
				var code = Integer.parseInt(numberStr);
				if (code > 0) {
					acc.add(code);
				}
			} catch (NumberFormatException e) {
				log.error("Failed to parse number {}", numberStr);
			}
			numberStr = numberStr.substring(0, numberStr.length() - 1);
		}
		return acc;
	}
}
