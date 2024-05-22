package com.rustam.ccc;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest
class CccApplicationTests {
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			"postgres:15-alpine"
	);

	@BeforeAll
	static void beforeAll() {
		postgres.start();
	}

	@AfterAll
	static void afterAll() {
		postgres.stop();
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> postgres.getJdbcUrl().replace("jdbc", "r2dbc"));
		registry.add("spring.r2dbc.username", postgres::getUsername);
		registry.add("spring.r2dbc.password", postgres::getPassword);
		registry.add("spring.flyway.url", postgres::getJdbcUrl);
		registry.add("spring.flyway.user", postgres::getUsername);
		registry.add("spring.flyway.password", postgres::getPassword);
	}
	WebTestClient client;
	@Autowired
	private DatabaseClient databaseClient;

	@BeforeEach
	void setUp(ApplicationContext context) {
		client = WebTestClient.bindToApplicationContext(context).build();
	}

	// These 2 tests could have been combined into one, but they are logically different
	@Test
	void invalid_hasChars() {
		client.get().uri("/code?phoneNumber=+1-800-Test")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest();
	}

	@ParameterizedTest
	@ValueSource(strings = {"+1", "+1-800-123-4567-890-12345-6789"})
	void invalid_length(String number) {
		client.get().uri("/code?phoneNumber=+1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isBadRequest();
	}

	private static Stream<Arguments> singleCountry() {
		return Stream.of(
				Arguments.of("12423222931", "Bahamas", 1242),
				Arguments.of("71423423412", "Russia", 7),
				Arguments.of("77112227231", "Kazakhstan", 77),
				Arguments.of("711", "Russia", 7),
				Arguments.of("+442079460958", "United Kingdom", 44),
				Arguments.of("+49-30-123456", "Germany", 49),
				Arguments.of("+33 1 23 45 67 89", "France", 33),
				Arguments.of("+81 3-1234-5678", "Japan", 81)
		);
	}

	@ParameterizedTest
	@MethodSource("com.rustam.ccc.CccApplicationTests#singleCountry")
	void singleCountryFound(String number, String country, int foundCode) {
		client.get().uri("/code?phoneNumber=%s".formatted(number))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectAll(
						spec -> spec.expectStatus().isOk(),
						spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
						spec -> spec.expectBody()
								.jsonPath("$[*].code").isEqualTo(foundCode)
								.jsonPath("$[*].country").isEqualTo(country));
	}

	private static Stream<Arguments> multipleCountries() {
		return Stream.of(
				Arguments.of("11165384765", List.of("United States", "Canada"), 1),
				Arguments.of("18696621234", List.of("Saint Kitts and Nevis", "Nevis"), 1869),
				Arguments.of("47791234975", List.of("Jan Mayen", "Svalbard"), 4779)
		);
	}

	@ParameterizedTest
	@MethodSource("com.rustam.ccc.CccApplicationTests#multipleCountries")
	void multipleCountriesFound(String number, List<String> country, int foundCode) {
		var countryCodes = IntStream.range(0, country.size()).map(i -> foundCode).boxed().toList();

		client.get().uri("/code?phoneNumber=%s".formatted(number))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectAll(
						spec -> spec.expectStatus().isOk(),
						spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
						spec -> spec.expectBody()
								.jsonPath("$[*].code", Matchers.containsInAnyOrder(countryCodes)).isNotEmpty()
								.jsonPath("$[*].code", Matchers.hasSize(countryCodes.size())).isNotEmpty()
								.jsonPath("$[*].country", Matchers.containsInAnyOrder(country)).isNotEmpty()
								.jsonPath("$[*].country", Matchers.hasSize(country.size()))
				);
	}

	/**
	 * Actually this one is not doing what it looks like. Test application is run in a single thread, so it's not
	 * possible to test parallelism here. But it's a good example of how to test parallelism in general.
	 */
	@Test
	void verifyDatabaseState() {
		Mono<Boolean> waitUntilConditionMet = Mono.defer(
						() -> databaseClient.sql("SELECT COUNT(*) FROM country_code")
									.map(row -> row.get(0, Integer.class) > 0)
									.one())
				.filter(Boolean::booleanValue)
				.repeatWhenEmpty(it -> it.delayElements(Duration.ofSeconds(1)).take(30)) // Retry up to 30 times (30 seconds)
				.switchIfEmpty(Mono.error(new RuntimeException("Condition not met within the timeout")));

		StepVerifier.create(waitUntilConditionMet)
				.expectNext(true)
				.verifyComplete();
	}
}
