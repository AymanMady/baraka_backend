package neyan.tech.baraka_backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 4xx Client Errors ====================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "You don't have permission to access this resource",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // ==================== Validation Errors ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.withValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "One or more fields have validation errors",
                        request.getRequestURI(),
                        errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (existing, replacement) -> existing));

        log.warn("Constraint violation: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.withValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "One or more constraints were violated",
                        request.getRequestURI(),
                        errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        String.format("Required parameter '%s' is missing", ex.getParameterName()),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String errorMessage = "Invalid request body format";
        Throwable rootCause = ex.getRootCause();
        
        log.warn("Invalid request body at {}: {}", request.getRequestURI(), ex.getMessage());
        
        if (rootCause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) rootCause;
            String fieldName = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .collect(Collectors.joining("."));
            
            if (ife.getTargetType() != null && java.time.temporal.Temporal.class.isAssignableFrom(ife.getTargetType())) {
                errorMessage = String.format("Invalid date/time format for field '%s'. Expected format: ISO-8601 (e.g., 2026-01-09T19:14:00.000Z)", fieldName);
            } else {
                errorMessage = String.format("Invalid format for field '%s'. Expected type: %s", 
                        fieldName, ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "unknown");
            }
        } else if (rootCause instanceof MismatchedInputException) {
            MismatchedInputException mie = (MismatchedInputException) rootCause;
            String fieldName = mie.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .collect(Collectors.joining("."));
            errorMessage = String.format("Invalid input for field '%s'", fieldName);
        } else if (rootCause != null) {
            errorMessage = "Invalid request body: " + rootCause.getMessage();
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        errorMessage,
                        request.getRequestURI()));
    }

    // ==================== Database Errors ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String errorMessage = "Data integrity constraint violation";
        Throwable rootCause = ex.getRootCause();
        
        if (rootCause != null) {
            String rootCauseMessage = rootCause.getMessage();
            log.warn("Data integrity violation at {}: {}", request.getRequestURI(), rootCauseMessage);
            
            // Extract constraint name and provide user-friendly message
            if (rootCauseMessage != null) {
                // Check for common constraint violations
                if (rootCauseMessage.contains("chk_baskets_price_discount_lte_original")) {
                    errorMessage = "Discount price cannot be greater than original price";
                } else if (rootCauseMessage.contains("chk_baskets_pickup_end_after_start")) {
                    errorMessage = "Pickup end time must be after pickup start time";
                } else if (rootCauseMessage.contains("chk_baskets_quantity_left_lte_total")) {
                    errorMessage = "Quantity left cannot exceed total quantity";
                } else if (rootCauseMessage.contains("chk_baskets_title_not_empty")) {
                    errorMessage = "Title must be at least 2 characters long";
                } else if (rootCauseMessage.contains("fk_baskets_shop")) {
                    errorMessage = "Invalid shop ID";
                } else if (rootCauseMessage.contains("UNIQUE") || rootCauseMessage.contains("unique")) {
                    errorMessage = "A record with this information already exists";
                } else if (rootCauseMessage.contains("NOT NULL") || rootCauseMessage.contains("null value")) {
                    errorMessage = "Required field is missing";
                } else {
                    // Use a sanitized version of the root cause message
                    errorMessage = "Invalid data provided. Please check your input.";
                }
            }
        } else {
            log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        errorMessage,
                        request.getRequestURI()));
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ApiError> handleLazyInitialization(LazyInitializationException ex, HttpServletRequest request) {
        log.error("Lazy initialization exception at {}: Entity relationship accessed outside transaction context. Message: {}", 
                request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An error occurred while processing the request. Please try again later.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpServletRequest request) {
        log.error("Failed to write HTTP message (JSON serialization error) at {}: {}", 
                request.getRequestURI(), ex.getMessage(), ex);
        Throwable rootCause = ex.getRootCause();
        if (rootCause != null) {
            log.error("Root cause: {} - {}", rootCause.getClass().getName(), rootCause.getMessage());
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An error occurred while serializing the response. Please try again later.",
                        request.getRequestURI()));
    }

    // ==================== 5xx Server Errors ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred at {} - Exception type: {}, Message: {}", 
                request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
        
        // Log full stack trace for debugging
        if (log.isDebugEnabled()) {
            log.debug("Full stack trace for error at {}", request.getRequestURI(), ex);
        } else {
            // In production, still log the first few lines of stack trace
            StackTraceElement[] stackTrace = ex.getStackTrace();
            if (stackTrace.length > 0) {
                log.error("Stack trace (first 5 frames):");
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    log.error("  at {}", stackTrace[i]);
                }
            }
        }
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI()));
    }
}
