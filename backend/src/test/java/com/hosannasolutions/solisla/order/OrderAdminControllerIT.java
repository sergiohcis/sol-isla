package com.hosannasolutions.solisla.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.hosannasolutions.solisla.security.authorization.Role;
import com.hosannasolutions.solisla.user.dto.CreateUserRequest;
import com.hosannasolutions.solisla.user.providedService.UserService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** End-to-end admin order workflow: STAFF can advance status but not cancel (ORDER_CANCEL is
 *  ADMIN-only per {@code RolePermissions}), illegal transitions 409, and rejecting/cancelling an
 *  order restocks the inventory checkout already sold. */
@SpringBootTest
@Transactional
class OrderAdminControllerIT {

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
    private UserService userService;

    private MockMvc mockMvc;
    private String deliveryZoneId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        DeliveryZone zone = deliveryZoneService.create(new DeliveryZoneCreateRequest("Zone A", new BigDecimal("5.00")), null);
        deliveryZoneId = zone.getId().toString();

        userService.createUser(new CreateUserRequest("admin@test.com", "Password123!", "Ada", "Min", Role.ADMIN));
        userService.createUser(new CreateUserRequest("staff@test.com", "Password123!", "Stan", "Aff", Role.STAFF));
    }

    private String createActiveProduct(String sku, String name, int initialStock) throws Exception {
        Category category = categoryService.create(new CategoryCreateRequest(name + " Category", null, null, 0), null);
        Product product = productService.create(new ProductCreateRequest(
                sku, name, category.getId(), "Description", new BigDecimal("4.99"), "USD",
                DiscountType.NONE, null, null, null, false, initialStock), null);
        productService.changeStatus(product.getId(), ProductStatus.ACTIVE, null);
        return product.getId().toString();
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String placeOrder(String productId, int quantity) throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cartCookie = cartResult.getResponse().getCookie("sol_isla_cart");
        Assertions.assertNotNull(cartCookie);

        String checkoutJson = "{\"customerName\":\"Jane Doe\",\"customerPhone\":\"+15551234567\","
                + "\"customerEmail\":\"jane@example.com\",\"deliveryAddress\":\"123 Main St\","
                + "\"deliveryCity\":\"Orlando\",\"deliveryPostalCode\":\"32801\",\"deliveryNotes\":null,"
                + "\"deliveryZoneId\":\"" + deliveryZoneId + "\",\"paymentMethod\":\"CASH_ON_DELIVERY\"}";

        String orderJson = mockMvc.perform(post("/api/checkout").with(csrf()).cookie(cartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(orderJson, "$.id");
    }

    @Test
    void staffCanAdvanceStatusButNotCancel() throws Exception {
        String productId = createActiveProduct("SKU-ORD-1", "Coconut Water", 10);
        String orderId = placeOrder(productId, 2);
        MockHttpSession staffSession = loginAs("staff@test.com");

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status").with(csrf()).session(staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));

        mockMvc.perform(post("/api/admin/orders/" + orderId + "/cancel").with(csrf()).session(staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"changed my mind\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void illegalTransitionIsRejectedWithConflict() throws Exception {
        String productId = createActiveProduct("SKU-ORD-2", "Mango Juice", 10);
        String orderId = placeOrder(productId, 1);
        MockHttpSession staffSession = loginAs("staff@test.com");

        // PENDING_CONFIRMATION -> DELIVERED skips the whole pipeline.
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status").with(csrf()).session(staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCancellingAnOrderRestocksInventory() throws Exception {
        String productId = createActiveProduct("SKU-ORD-3", "Guava Nectar", 10);
        String orderId = placeOrder(productId, 3);

        mockMvc.perform(get("/api/products/guava-nectar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(7));

        MockHttpSession adminSession = loginAs("admin@test.com");
        mockMvc.perform(post("/api/admin/orders/" + orderId + "/cancel").with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"customer requested cancellation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        // The 3 sold units are back: 7 + 3 = 10.
        mockMvc.perform(get("/api/products/guava-nectar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10));

        // Cancelling twice is not a legal transition (CANCELLED is terminal) — no double-restock.
        mockMvc.perform(post("/api/admin/orders/" + orderId + "/cancel").with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"again\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectingAnOrderRestocksInventory() throws Exception {
        String productId = createActiveProduct("SKU-ORD-4", "Papaya Smoothie", 5);
        String orderId = placeOrder(productId, 2);
        MockHttpSession staffSession = loginAs("staff@test.com");

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status").with(csrf()).session(staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("REJECTED"));

        mockMvc.perform(get("/api/products/papaya-smoothie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void adminListSupportsStatusFilterAndSearch() throws Exception {
        String productId = createActiveProduct("SKU-ORD-5", "Tamarind Soda", 10);
        placeOrder(productId, 1);
        MockHttpSession adminSession = loginAs("admin@test.com");

        mockMvc.perform(get("/api/admin/orders").session(adminSession).param("status", "PENDING_CONFIRMATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/orders").session(adminSession).param("q", "Jane Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].customerName").value("Jane Doe"));
    }
}
