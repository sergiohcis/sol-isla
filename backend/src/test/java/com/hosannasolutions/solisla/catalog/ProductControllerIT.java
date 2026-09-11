package com.hosannasolutions.solisla.catalog;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regression test for a real bug caught only by hand against a live dev server: {@code GET
 * /api/products} with no filters at all returned 401 (masking a 500) because
 * {@code ProductRepository.search}'s {@code :q} bind, used only inside {@code lower(concat(...))},
 * was passed as a Java {@code null} — PostgreSQL can't infer that parameter's type from context and
 * defaulted it to {@code bytea}, so {@code lower(:q)} failed with "function lower(bytea) does not
 * exist". The failure surfaced as 401 because the resulting 500 triggered an internal {@code
 * /error} re-dispatch, which isn't in the public security matcher list. Fixed by never binding
 * {@code null} for {@code q} (empty string instead) — see {@code ProductServiceImpl.blankToEmpty}.
 * <p>
 * Builds {@link MockMvc} from the {@link WebApplicationContext} directly (with the real security
 * filter chain applied) rather than {@code @AutoConfigureMockMvc}, which needs
 * spring-boot-test-autoconfigure — not pulled in by this project's granular
 * spring-boot-starter-*-test dependencies.
 */
@SpringBootTest
class ProductControllerIT {

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    void searchWithNoFiltersReturnsOkNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void searchWithQueryTextReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "anything"))
                .andExpect(status().isOk());
    }

    @Test
    void categoriesEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
    }
}
