package com.hosannasolutions.solisla.whatsapp.providedService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.providedService.OrderService;
import com.hosannasolutions.solisla.whatsapp.MessageOutbox;
import com.hosannasolutions.solisla.whatsapp.MessageOutboxStatus;
import com.hosannasolutions.solisla.whatsapp.dto.WhatsAppSendResult;
import com.hosannasolutions.solisla.whatsapp.repository.MessageOutboxRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Covers the message template (design doc §21) and the retry-on-failure path — success against
 *  the real Meta Graph API isn't something a test suite can exercise, so what's testable here is
 *  everything up to and around that call: rendering, and what happens when the send fails, which
 *  it always will in this test environment since sol-isla.whatsapp.* is unset — the same as any
 *  fresh dev/test install per application.yml's defaults. */
@SpringBootTest
@Transactional
class WhatsAppOutboxIT {

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
    private OrderService orderService;

    @Autowired
    private MessageOutboxRepository messageOutboxRepository;

    @Autowired
    private WhatsAppServiceImpl whatsAppServiceImpl;

    @Autowired
    private OutboxProcessingService outboxProcessingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private Order placeOneOrder(String skuSuffix) throws Exception {
        Category category = categoryService.create(new CategoryCreateRequest("Snacks", null, null, 0), null);
        Product product = productService.create(new ProductCreateRequest(
                "SKU-WA-" + skuSuffix, "Plantain Chips", category.getId(), "Crunchy", new BigDecimal("4.99"), "USD",
                DiscountType.NONE, null, null, null, false, 10), null);
        productService.changeStatus(product.getId(), ProductStatus.ACTIVE, null);
        DeliveryZone zone = deliveryZoneService.create(new DeliveryZoneCreateRequest("Zone A", new BigDecimal("5.00")), null);

        var addResult = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + product.getId() + "\",\"quantity\":1}"))
                .andReturn();
        Cookie cartCookie = addResult.getResponse().getCookie("sol_isla_cart");

        var checkoutResult = mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Jane Doe\",\"customerPhone\":\"+15551234567\","
                                + "\"customerEmail\":\"jane@example.com\",\"deliveryAddress\":\"123 Main St\","
                                + "\"deliveryCity\":\"Orlando\",\"deliveryPostalCode\":\"32801\","
                                + "\"deliveryNotes\":\"Ring the bell\",\"deliveryZoneId\":\"" + zone.getId() + "\","
                                + "\"paymentMethod\":\"CASH_ON_DELIVERY\"}"))
                .andReturn();
        String orderNumber = com.jayway.jsonpath.JsonPath.read(checkoutResult.getResponse().getContentAsString(), "$.orderNumber");
        return orderService.getByOrderNumber(orderNumber);
    }

    @Test
    void renderOrderCreatedMessageIncludesKeyOrderDetails() throws Exception {
        Order order = placeOneOrder("1");

        String message = whatsAppServiceImpl.renderOrderCreatedMessage(order);

        assertTrue(message.contains(order.getOrderNumber()));
        assertTrue(message.contains("Jane Doe"));
        assertTrue(message.contains("+15551234567"));
        assertTrue(message.contains("123 Main St"));
        assertTrue(message.contains("Plantain Chips"));
        assertTrue(message.contains("Cash on Delivery"));
        assertTrue(message.contains("Pending confirmation"));
    }

    @Test
    void isNotConfiguredByDefaultAndSendFailsGracefully() throws Exception {
        assertFalse(whatsAppServiceImpl.isConfigured());

        Order order = placeOneOrder("2");
        WhatsAppSendResult result = whatsAppServiceImpl.sendOrderCreatedMessage(order);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
    }

    @Test
    void checkoutEnqueuesAnOutboxRowAndProcessOneRecordsARetryableFailure() throws Exception {
        Order order = placeOneOrder("3");

        List<MessageOutbox> outboxRows = messageOutboxRepository.findAll().stream()
                .filter(m -> m.getAggregateId().equals(order.getId()))
                .toList();
        assertEquals(1, outboxRows.size());
        MessageOutbox outbox = outboxRows.get(0);
        assertEquals(MessageOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(0, outbox.getAttemptCount());

        outboxProcessingService.processOne(outbox.getId());

        MessageOutbox reloaded = messageOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertEquals(MessageOutboxStatus.PENDING, reloaded.getStatus()); // retryable, not terminal yet
        assertEquals(1, reloaded.getAttemptCount());
        assertNotNull(reloaded.getLastError());
        assertNotNull(reloaded.getNextAttemptAt());
    }
}
