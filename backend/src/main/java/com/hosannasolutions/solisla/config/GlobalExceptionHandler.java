package com.hosannasolutions.solisla.config;

import com.hosannasolutions.solisla.user.exception.DuplicateUserEmailException;
import com.hosannasolutions.solisla.user.exception.UserNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
            UserNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            DuplicateUserEmailException.class
    })
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
