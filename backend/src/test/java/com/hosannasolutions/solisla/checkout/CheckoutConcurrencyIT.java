package com.hosannasolutions.solisla.checkout;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hosannasolutions.solisla.catalog.DiscountType;
import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.dto.ProductCreateRequest;
import com.hosannasolutions.solisla.catalog.providedService.ProductService;
import com.hosannasolutions.solisla.category.Category;
import com.hosannasolutions.solisla.category.dto.CategoryCreateRequest;
import com.hosannasolutions.solisla.category.providedService.CategoryService;
import com.hosannasolutions.solisla.delivery.DeliveryZone;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneCreateRequest;
import com.hosannasolutions.solisla.delivery.providedService.DeliveryZoneService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Split out from {@code CheckoutControllerIT} because this test needs {@link TestTransaction#end()}
 * to commit its fixture data for real (see below) — sharing that class's {@code @BeforeEach setUp()}
 * would permanently commit its "SKU-CHECKOUT-1"/"Snacks" fixtures too, colliding with every other
 * test in that class relying on those same unique slugs/SKUs being rolled back afterward.
 * <p>
 * {@code CheckoutControllerIT}'s own concurrency test simulates the race (manually shrinking stock
 * before a single request). This one proves it under an actual race: two real HTTP checkouts, on
 * two real threads, both trying to buy the one remaining unit at the same time — exactly the
 * scenario CLAUDE.md's Phase 4 notes claim {@code Inventory}'s optimistic lock prevents.
 */
@SpringBootTest
@Transactional
class CheckoutConcurrencyIT {

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

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private DeliveryZoneService deliveryZoneService;

    @Test
    void concurrentCheckoutsForTheLastUnitOnlyOneSucceeds() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Category category = categoryService.create(new CategoryCreateRequest("Concurrency Category", null, null, 0), null);
        Product product = productService.create(new ProductCreateRequest(
                "SKU-CONCURRENT-1", "Concurrent Test Item", category.getId(), "Only one in stock", new BigDecimal("4.99"),
                "USD", DiscountType.NONE, null, null, null, false, 1), null);
        productService.changeStatus(product.getId(), ProductStatus.ACTIVE, null);
        String productId = product.getId().toString();

        DeliveryZone zone = deliveryZoneService.create(new DeliveryZoneCreateRequest("Zone A", new BigDecimal("5.00")), null);
        String checkoutJson = "{\"customerName\":\"Jane Doe\",\"customerPhone\":\"+15551234567\","
                + "\"customerEmail\":\"jane@example.com\",\"deliveryAddress\":\"123 Main St\","
                + "\"deliveryCity\":\"Orlando\",\"deliveryPostalCode\":\"32801\",\"deliveryNotes\":null,"
                + "\"deliveryZoneId\":\"" + zone.getId() + "\",\"paymentMethod\":\"CASH_ON_DELIVERY\"}";

        // The class-level @Transactional would otherwise hide everything above from the two
        // worker threads below (separate DB connections, READ COMMITTED) until this test method
        // returns and rolls back — commit it now so the race is real. This test's own fixture
        // names are unique to it (this is the only @Test in the class), so nothing else collides.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Cookie cartA = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("sol_isla_cart");
        Cookie cartB = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("sol_isla_cart");

        Callable<Integer> checkoutA = () -> mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartA)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutJson))
                .andReturn().getResponse().getStatus();
        Callable<Integer> checkoutB = () -> mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartB)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutJson))
                .andReturn().getResponse().getStatus();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Integer> statuses;
        try {
            List<Future<Integer>> futures = executor.invokeAll(List.of(checkoutA, checkoutB));
            statuses = futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        } finally {
            executor.shutdown();
        }

        Assertions.assertEquals(1, statuses.stream().filter(status -> status == 200).count(),
                "exactly one concurrent checkout should succeed, got statuses " + statuses);
        Assertions.assertEquals(1, statuses.stream().filter(status -> status == 409).count(),
                "the other should be rejected as a conflict, got statuses " + statuses);

        // Never oversold — exactly 0 left, not -1.
        mockMvc.perform(get("/api/products/concurrent-test-item"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }
}
