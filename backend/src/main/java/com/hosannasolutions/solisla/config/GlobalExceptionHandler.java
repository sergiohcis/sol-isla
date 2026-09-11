package com.hosannasolutions.solisla.config;

import com.hosannasolutions.solisla.cart.exception.CartItemNotFoundException;
import com.hosannasolutions.solisla.cart.exception.CartNotFoundException;
import com.hosannasolutions.solisla.cart.exception.InvalidCartQuantityException;
import com.hosannasolutions.solisla.cart.exception.ProductNotAvailableException;
import com.hosannasolutions.solisla.catalog.exception.DuplicateSkuException;
import com.hosannasolutions.solisla.catalog.exception.InvalidProductStatusTransitionException;
import com.hosannasolutions.solisla.catalog.exception.ProductImageNotFoundException;
import com.hosannasolutions.solisla.catalog.exception.ProductNotFoundException;
import com.hosannasolutions.solisla.category.exception.CategoryNotFoundException;
import com.hosannasolutions.solisla.checkout.exception.EmptyCartException;
import com.hosannasolutions.solisla.common.storage.FileStorageException;
import com.hosannasolutions.solisla.delivery.exception.DeliveryZoneNotFoundException;
import com.hosannasolutions.solisla.inventory.exception.InsufficientStockException;
import com.hosannasolutions.solisla.inventory.exception.InventoryNotFoundException;
import com.hosannasolutions.solisla.order.exception.OrderNotFoundException;
import com.hosannasolutions.solisla.user.exception.DuplicateUserEmailException;
import com.hosannasolutions.solisla.user.exception.UserNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Deliberately declares no catch-all {@code @ExceptionHandler(Exception.class)}: a broad handler
 * here would also match {@code AccessDeniedException} (a {@code RuntimeException} subclass) and
 * intercept it inside {@code DispatcherServlet}, before it can reach Spring Security's
 * {@code ExceptionTranslationFilter} and become a 403.
 * <p>
 * Add each new module's not-found/conflict/bad-request exceptions to the matching
 * {@code @ExceptionHandler} group below as those modules are built (catalog, inventory, cart,
 * checkout, order, payment, delivery, whatsapp, ...) — anything not explicitly handled here falls
 * through to Spring Boot's default error handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UserNotFoundException.class,
            CategoryNotFoundException.class,
            ProductNotFoundException.class,
            ProductImageNotFoundException.class,
            InventoryNotFoundException.class,
            CartNotFoundException.class,
            CartItemNotFoundException.class,
            DeliveryZoneNotFoundException.class,
            OrderNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            DuplicateUserEmailException.class,
            DuplicateSkuException.class,
            InvalidProductStatusTransitionException.class,
            InsufficientStockException.class,
            ProductNotAvailableException.class,
            InvalidCartQuantityException.class,
            EmptyCartException.class
    })
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** A concurrent sale of the last unit during checkout (CLAUDE.md rule 4) — the loser's
     *  transaction rolls back and lands here rather than overselling. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Stock changed while processing your request — please try again");
    }

    @ExceptionHandler(FileStorageException.class)
    public ProblemDetail handleFileStorage(FileStorageException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
