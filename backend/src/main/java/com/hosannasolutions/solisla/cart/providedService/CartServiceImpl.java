package com.hosannasolutions.solisla.cart.providedService;

import com.hosannasolutions.solisla.cart.Cart;
import com.hosannasolutions.solisla.cart.CartItem;
import com.hosannasolutions.solisla.cart.CartStatus;
import com.hosannasolutions.solisla.cart.dto.CartItemResponse;
import com.hosannasolutions.solisla.cart.dto.CartResponse;
import com.hosannasolutions.solisla.cart.dto.CartSessionResult;
import com.hosannasolutions.solisla.cart.exception.CartItemNotFoundException;
import com.hosannasolutions.solisla.cart.exception.CartNotFoundException;
import com.hosannasolutions.solisla.cart.exception.InvalidCartQuantityException;
import com.hosannasolutions.solisla.cart.exception.ProductNotAvailableException;
import com.hosannasolutions.solisla.cart.repository.CartItemRepository;
import com.hosannasolutions.solisla.cart.repository.CartRepository;
import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductImage;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.exception.ProductNotFoundException;
import com.hosannasolutions.solisla.catalog.pricing.ProductPricingService;
import com.hosannasolutions.solisla.catalog.repository.ProductImageRepository;
import com.hosannasolutions.solisla.catalog.repository.ProductRepository;
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductPricingService pricingService;
    private final InventoryService inventoryService;
    private final int maxQuantityPerItem;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                            ProductRepository productRepository, ProductImageRepository productImageRepository,
                            ProductPricingService pricingService, InventoryService inventoryService,
                            @Value("${sol-isla.cart.max-quantity-per-item}") int maxQuantityPerItem) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.maxQuantityPerItem = maxQuantityPerItem;
    }

    @Override
    public CartResponse getCart(String sessionToken) {
        return findLiveCart(sessionToken).map(this::buildResponse).orElseGet(CartResponse::empty);
    }

    @Override
    @Transactional
    public CartSessionResult addItem(String sessionToken, UUID productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductNotAvailableException(productId);
        }

        Cart cart = findLiveCart(sessionToken).orElseGet(() -> cartRepository.save(Cart.createNew()));
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
        int desiredQuantity = (item != null ? item.getQuantity() : 0) + quantity;
        requireQuantityPurchasable(productId, desiredQuantity);

        if (item != null) {
            item.setQuantity(desiredQuantity);
        } else {
            item = new CartItem(cart.getId(), productId, quantity);
        }
        cartItemRepository.save(item);

        cart.assignCurrencyIfUnset(product.getCurrency());
        cart.touch();
        cartRepository.save(cart);

        return new CartSessionResult(cart.getSessionToken(), buildResponse(cart));
    }

    @Override
    @Transactional
    public CartSessionResult updateItemQuantity(String sessionToken, UUID itemId, int quantity) {
        Cart cart = requireLiveCart(sessionToken);
        CartItem item = ownedItem(cart, itemId);
        requireQuantityPurchasable(item.getProductId(), quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        cart.touch();
        cartRepository.save(cart);

        return new CartSessionResult(cart.getSessionToken(), buildResponse(cart));
    }

    @Override
    @Transactional
    public CartSessionResult removeItem(String sessionToken, UUID itemId) {
        Cart cart = requireLiveCart(sessionToken);
        CartItem item = ownedItem(cart, itemId);

        cartItemRepository.delete(item);
        cart.touch();
        cartRepository.save(cart);

        return new CartSessionResult(cart.getSessionToken(), buildResponse(cart));
    }

    private CartItem ownedItem(Cart cart, UUID itemId) {
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new CartItemNotFoundException(itemId));
        if (!item.getCartId().equals(cart.getId())) {
            // Deliberately the same not-found error as a missing id: a mismatched cart token
            // shouldn't be able to probe for the existence of someone else's cart item.
            throw new CartItemNotFoundException(itemId);
        }
        return item;
    }

    private void requireQuantityPurchasable(UUID productId, int quantity) {
        if (quantity > maxQuantityPerItem) {
            throw new InvalidCartQuantityException("Quantity exceeds the maximum of " + maxQuantityPerItem + " per item");
        }
        int availableStock = inventoryService.availableQuantityOrZero(productId);
        if (quantity > availableStock) {
            throw new InvalidCartQuantityException("Only " + availableStock + " in stock");
        }
    }

    private Optional<Cart> findLiveCart(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return cartRepository.findBySessionTokenAndStatus(sessionToken, CartStatus.ACTIVE)
                .filter(cart -> !cart.isExpired());
    }

    private Cart requireLiveCart(String sessionToken) {
        return findLiveCart(sessionToken).orElseThrow(CartNotFoundException::new);
    }

    private CartResponse buildResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        if (items.isEmpty()) {
            return new CartResponse(cart.getId(), List.of(), 0, BigDecimal.ZERO, cart.getCurrency());
        }

        List<UUID> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<UUID, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<UUID, String> primaryImageByProductId = productImageRepository.findByProductIdInOrderBySortOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId,
                        Collectors.collectingAndThen(Collectors.toList(), CartServiceImpl::firstDisplayableImageUrl)));
        Map<UUID, Integer> stockByProductId = inventoryService.availableQuantitiesOrZero(productIds);
        Instant now = Instant.now();

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<CartItemResponse> itemResponses = new ArrayList<>(items.size());
        for (CartItem item : items) {
            Product product = productsById.get(item.getProductId());
            BigDecimal unitPrice = pricingService.effectivePrice(product, now);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();
            itemResponses.add(new CartItemResponse(
                    item.getId(), product.getId(), product.getName(), product.getSlug(),
                    primaryImageByProductId.get(product.getId()), unitPrice, item.getQuantity(), lineTotal,
                    stockByProductId.getOrDefault(product.getId(), 0)));
        }

        return new CartResponse(cart.getId(), itemResponses, totalQuantity, subtotal, cart.getCurrency());
    }

    private static String firstDisplayableImageUrl(List<ProductImage> images) {
        return images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
