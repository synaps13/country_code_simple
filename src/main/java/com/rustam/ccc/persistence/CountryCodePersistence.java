package com.rustam.ccc.persistence;

import com.rustam.ccc.domain.CountryCode;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

@Component
// I am not using spring JDBC convenience functions as it hides SQL generation and having low level access to queries
// construction simplifies maintenance and extension as well as helps to find problems without debugging in some cases.
public class CountryCodePersistence {
	private final Logger log = LoggerFactory.getLogger(CountryCodePersistence.class);
	private final R2dbcEntityTemplate template;
	private final ConnectionFactory connectionFactory;

	public CountryCodePersistence(R2dbcEntityTemplate template, ConnectionFactory connectionFactory) {
		this.template = template;
		this.connectionFactory = connectionFactory;
	}

	public Mono<Void> addCountryCodes(Collection<CountryCode> codes) {
		// R2DBC does not support batch insert just yet, so using a prepared statement
		return Mono.usingWhen(
				connectionFactory.create(),
				connection -> clearCodeTable()
						.thenEmpty(Flux.fromIterable(codes)
								.flatMap(code -> template.getDatabaseClient()
										.sql("INSERT INTO country_code (phone_code, country) VALUES ($1, $2)")
										.bind(0, code.code())
										.bind(1, code.country())
										.then())
								.then()),
				Connection::close);
	}

	public Flux<CountryCode> findByCode(List<Integer> code) {
		if (code.isEmpty()) {
			return Flux.empty();
		}

		return template.getDatabaseClient()
				// simple trick is to supply a list of codes with longest/largest and therefore most specific
				// while dealing with numbers and not strings math rules save us a lot of processing
				// FIXME: there is a problem with InParameter, it's cannot bind List<Integer> directly
				.sql("""
						SELECT country, phone_code
						FROM (
							SELECT country, phone_code, DENSE_RANK() OVER (ORDER BY phone_code DESC) as rank
							FROM country_code
							WHERE phone_code IN (%s)
						) ranked
						WHERE rank = 1;
						""".formatted(String.join(", ", IntStream
							.range(0, code.size()).boxed()
							.map(i -> code.get(i).toString())
							.toList())))
				.fetch()
				.all()
				.map(row -> new CountryCode((String) row.get("country"), (Integer) row.get("phone_code")));
	}

	private Mono<Void> clearCodeTable() {
		return template.getDatabaseClient().sql("TRUNCATE country_code").then();
	}
}
