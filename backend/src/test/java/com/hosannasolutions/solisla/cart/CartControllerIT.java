package com.hosannasolutions.solisla.cart;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.jayway.jsonpath.JsonPath;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
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

/** End-to-end guest-cart flow: add -> get -> update quantity -> remove, all through the real
 *  cookie mechanism CartController uses (no shortcuts) — see ProductControllerIT's Javadoc for
 *  why this project now insists on hitting real endpoints for every module, not just services.
 *  {@code @Transactional} rolls each test's fixture data back automatically — every test's own
 *  {@code setUp()} otherwise collides on the same SKU against the one shared embedded database. */
@SpringBootTest
@Transactional
class CartControllerIT {

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

    private MockMvc mockMvc;
    private String activeProductId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Category category = categoryService.create(new CategoryCreateRequest("Snacks", null, null, 0), null);
        Product product = productService.create(new ProductCreateRequest(
                "SKU-CART-1", "Plantain Chips", category.getId(), "Crunchy", new BigDecimal("4.99"), "USD",
                DiscountType.NONE, null, null, null, false, 10), null);
        productService.changeStatus(product.getId(), ProductStatus.ACTIVE, null);
        activeProductId = product.getId().toString();
    }

    @Test
    void guestCanAddUpdateAndRemoveACartItem() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + activeProductId + "\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(9.98))
                .andReturn();

        Cookie cartCookie = addResult.getResponse().getCookie("sol_isla_cart");
        org.junit.jupiter.api.Assertions.assertNotNull(cartCookie, "cart cookie must be set after adding an item");

        String cartJson = mockMvc.perform(get("/api/cart").cookie(cartCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(cartJson, "$.items[0].id");

        mockMvc.perform(put("/api/cart/items/" + itemId).with(csrf()).cookie(cartCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(5));

        mockMvc.perform(delete("/api/cart/items/" + itemId).with(csrf()).cookie(cartCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    @Test
    void addingMoreThanAvailableStockIsRejected() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + activeProductId + "\",\"quantity\":999}"))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingCartWithNoCookieReturnsEmptyCartNotAnError() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalQuantity", equalTo(0)));
    }
}
