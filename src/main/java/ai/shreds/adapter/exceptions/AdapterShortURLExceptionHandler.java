package ai.shreds.adapter.exceptions;

import ai.shreds.shared.exceptions.SharedExceptionValidation;
import ai.shreds.shared.exceptions.SharedExceptionNotFound;
import ai.shreds.shared.exceptions.SharedShortUrlException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class AdapterShortURLExceptionHandler {

    private final MeterRegistry meterRegistry;

    public AdapterShortURLExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(SharedExceptionValidation.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            SharedExceptionValidation e,
            WebRequest request) {
        log.warn("Validation error: {}", e.getMessage());
        meterRegistry.counter("shorturl.errors", "type", "validation").increment();

        Map<String, Object> body = buildErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );
        body.put("validationErrors", e.getValidationErrors());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(SharedExceptionNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            SharedExceptionNotFound e,
            WebRequest request) {
        log.warn("Resource not found: {}", e.getMessage());
        meterRegistry.counter("shorturl.errors", "type", "not_found").increment();

        Map<String, Object> body = buildErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getDescription(false)
        );
        body.put("resourceType", e.getResourceType());
        body.put("resourceId", e.getResourceId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(SharedShortUrlException.class)
    public ResponseEntity<Map<String, Object>> handleShortUrlException(
            SharedShortUrlException e,
            WebRequest request) {
        log.error("Short URL error: {}", e.getMessage());
        meterRegistry.counter("shorturl.errors", "type", e.getErrorCategory()).increment();

        HttpStatus status = determineHttpStatus(e.getErrorCode());
        Map<String, Object> body = buildErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                status,
                request.getDescription(false)
        );
        body.put("errorCategory", e.getErrorCategory());
        body.put("errorDetails", e.getErrorDetails());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException e,
            WebRequest request) {
        log.warn("Method argument validation failed: {}", e.getMessage());
        meterRegistry.counter("shorturl.errors", "type", "validation").increment();

        Map<String, String> validationErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = buildErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );
        body.put("validationErrors", validationErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            NoHandlerFoundException e,
            WebRequest request) {
        log.warn("No handler found for: {} {}", e.getHttpMethod(), e.getRequestURL());
        meterRegistry.counter("shorturl.errors", "type", "not_found").increment();

        return buildErrorResponse(
                "ENDPOINT_NOT_FOUND",
                "The requested endpoint does not exist",
                HttpStatus.NOT_FOUND,
                request.getDescription(false)
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleInternalError(
            Exception e,
            WebRequest request) {
        log.error("Internal server error", e);
        meterRegistry.counter("shorturl.errors", "type", "internal").increment();

        return buildErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getDescription(false)
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String errorCode,
            String message,
            HttpStatus status,
            String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("path", path);
        
        return new ResponseEntity<>(body, status);
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "URL_COLLISION" -> HttpStatus.CONFLICT;
            case "INVALID_URL_FORMAT" -> HttpStatus.BAD_REQUEST;
            case "URL_EXPIRED" -> HttpStatus.GONE;
            case "URL_BLOCKED" -> HttpStatus.FORBIDDEN;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "INVALID_CUSTOM_ALIAS" -> HttpStatus.BAD_REQUEST;
            case "ALIAS_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}