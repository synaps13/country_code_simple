package com.rustam.ccc.configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class Database {
	@Bean
	public R2dbcEntityTemplate R2dbcEntityTemplate(ConnectionFactory connectionFactory) {
		return new R2dbcEntityTemplate(connectionFactory);
	}
}
