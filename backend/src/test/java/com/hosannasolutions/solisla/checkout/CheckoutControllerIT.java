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
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import java.util.UUID;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** End-to-end guest checkout: add to cart -> POST /api/checkout -> order created, inventory
 *  decremented, cart emptied, and a second submission with the same Idempotency-Key returns the
 *  same order rather than creating a duplicate (design doc §23). */
@SpringBootTest
@Transactional
class CheckoutControllerIT {

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

    @Autowired
    private InventoryService inventoryService;

    private MockMvc mockMvc;
    private String activeProductId;
    private String deliveryZoneId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Category category = categoryService.create(new CategoryCreateRequest("Snacks", null, null, 0), null);
        Product product = productService.create(new ProductCreateRequest(
                "SKU-CHECKOUT-1", "Plantain Chips", category.getId(), "Crunchy", new BigDecimal("4.99"), "USD",
                DiscountType.NONE, null, null, null, false, 10), null);
        productService.changeStatus(product.getId(), ProductStatus.ACTIVE, null);
        activeProductId = product.getId().toString();

        DeliveryZone zone = deliveryZoneService.create(new DeliveryZoneCreateRequest("Zone A", new BigDecimal("5.00")), null);
        deliveryZoneId = zone.getId().toString();
    }

    private Cookie addOneItemToCart() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + activeProductId + "\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cartCookie = result.getResponse().getCookie("sol_isla_cart");
        Assertions.assertNotNull(cartCookie);
        return cartCookie;
    }

    private String checkoutJson() {
        return "{\"customerName\":\"Jane Doe\",\"customerPhone\":\"+15551234567\","
                + "\"customerEmail\":\"jane@example.com\",\"deliveryAddress\":\"123 Main St\","
                + "\"deliveryCity\":\"Orlando\",\"deliveryPostalCode\":\"32801\",\"deliveryNotes\":\"Ring the bell\","
                + "\"deliveryZoneId\":\"" + deliveryZoneId + "\",\"paymentMethod\":\"CASH_ON_DELIVERY\"}";
    }

    @Test
    void checkoutCreatesOrderDecrementsStockAndEmptiesCart() throws Exception {
        Cookie cartCookie = addOneItemToCart();

        String orderJson = mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.subtotal").value(9.98))
                .andExpect(jsonPath("$.deliveryFee").value(5.00))
                .andExpect(jsonPath("$.grandTotal").value(14.98))
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderStatus").value("PENDING_CONFIRMATION"))
                .andReturn().getResponse().getContentAsString();
        String orderNumber = com.jayway.jsonpath.JsonPath.read(orderJson, "$.orderNumber");

        // Stock decremented: 10 initial - 2 sold = 8.
        mockMvc.perform(get("/api/products/plantain-chips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(8));

        // Cart is now empty (converted, not just cleared).
        mockMvc.perform(get("/api/cart").cookie(cartCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        // Public tracking finds the same order.
        mockMvc.perform(get("/api/orders/track/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));
    }

    @Test
    void repeatingTheSameIdempotencyKeyReturnsTheSameOrderNotADuplicate() throws Exception {
        Cookie cartCookie = addOneItemToCart();

        String first = mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .header("Idempotency-Key", "duplicate-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstOrderNumber = com.jayway.jsonpath.JsonPath.read(first, "$.orderNumber");

        // Second submission with the SAME key and a fresh (now-empty) cart cookie must return
        // the original order, not fail because the cart is empty.
        String second = mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .header("Idempotency-Key", "duplicate-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondOrderNumber = com.jayway.jsonpath.JsonPath.read(second, "$.orderNumber");

        Assertions.assertEquals(firstOrderNumber, secondOrderNumber);

        // Stock only decremented once (10 - 2 = 8), not twice.
        mockMvc.perform(get("/api/products/plantain-chips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(8));
    }

    @Test
    void checkoutWithEmptyCartIsRejected() throws Exception {
        mockMvc.perform(post("/api/checkout").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutIsRejectedIfStockDroppedBelowCartQuantitySinceItWasAdded() throws Exception {
        // Cart-add itself already blocks requesting more than available stock, so the only way
        // to exercise checkout's own re-validation is a race: add while stock is sufficient (10),
        // then have stock drop (an admin correction, or another customer's checkout) before this
        // checkout transaction runs.
        Cookie cartCookie = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + activeProductId + "\",\"quantity\":8}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("sol_isla_cart");

        inventoryService.adjust(UUID.fromString(activeProductId), -5, "simulated concurrent sale", null);

        mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isConflict());
    }
}
