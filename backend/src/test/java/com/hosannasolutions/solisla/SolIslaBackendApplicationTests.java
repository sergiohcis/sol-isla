package com.hosannasolutions.solisla;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class SolIslaBackendApplicationTests {

	private static EmbeddedPostgres embeddedPostgres;

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) throws IOException {
		embeddedPostgres = EmbeddedPostgres.builder().start();
		registry.add("spring.datasource.url", () -> embeddedPostgres.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}

	@AfterAll
	static void stopEmbeddedPostgres() throws IOException {
		if (embeddedPostgres != null) {
			embeddedPostgres.close();
		}
	}

	@Test
	void contextLoadsAndFlywayBaselineMigrationApplies() {
	}

}
