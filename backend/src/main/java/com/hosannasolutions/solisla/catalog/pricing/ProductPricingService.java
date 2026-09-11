package com.hosannasolutions.solisla.catalog.pricing;

import com.hosannasolutions.solisla.catalog.DiscountType;
import com.hosannasolutions.solisla.catalog.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * The single place effective price is computed — reused by catalog responses now and by checkout
 * later (design doc §8: "the backend recalculates the price", CLAUDE.md rule 2: never trust a
 * client-supplied price). A discount only applies while {@code now} falls inside its effective
 * window; an unset bound on either side means "no limit" on that side.
 */
@Service
public class ProductPricingService {

    public BigDecimal effectivePrice(Product product, Instant now) {
        BigDecimal basePrice = product.getBasePrice();
        if (!isDiscountActive(product, now)) {
            return basePrice;
        }
        BigDecimal discounted = switch (product.getDiscountType()) {
            case NONE -> basePrice;
            case PERCENTAGE -> basePrice.subtract(
                    basePrice.multiply(product.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            case FIXED_AMOUNT -> basePrice.subtract(product.getDiscountValue());
        };
        return discounted.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isDiscountActive(Product product, Instant now) {
        if (product.getDiscountType() == DiscountType.NONE || product.getDiscountValue() == null) {
            return false;
        }
        if (product.getDiscountEffectiveFrom() != null && now.isBefore(product.getDiscountEffectiveFrom())) {
            return false;
        }
        return product.getDiscountEffectiveTo() == null || !now.isAfter(product.getDiscountEffectiveTo());
    }
}
