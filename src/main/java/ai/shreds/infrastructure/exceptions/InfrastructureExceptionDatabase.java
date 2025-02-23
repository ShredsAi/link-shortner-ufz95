package ai.shreds.infrastructure.exceptions;

import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception for database-related errors in the infrastructure layer.
 * Provides detailed error information and mapping to domain exceptions.
 */
@Slf4j
@Getter
public class InfrastructureExceptionDatabase extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Data Access Errors
    public static final String ERROR_COLLISION = "DB_COLLISION";
    public static final String ERROR_INVALID_KEY = "DB_INVALID_KEY";
    public static final String ERROR_NOT_FOUND = "DB_NOT_FOUND";
    public static final String ERROR_DUPLICATE = "DB_DUPLICATE";

    // Connection Errors
    public static final String ERROR_CONNECTION = "DB_CONNECTION";
    public static final String ERROR_TIMEOUT = "DB_TIMEOUT";
    public static final String ERROR_AUTH = "DB_AUTH";
    public static final String ERROR_NETWORK = "DB_NETWORK";

    // Operation Errors
    public static final String ERROR_OPERATION = "DB_OPERATION";
    public static final String ERROR_SERIALIZATION = "DB_SERIALIZATION";
    public static final String ERROR_TRANSACTION = "DB_TRANSACTION";
    public static final String ERROR_LOCK = "DB_LOCK";

    private final String errorCode;
    private final String errorCategory;
    private final Map<String, Object> errorDetails;
    private final String timestamp;
    private final String operation;

    private InfrastructureExceptionDatabase(Builder builder) {
        super(builder.message, builder.cause);
        this.errorCode = builder.errorCode;
        this.errorCategory = builder.errorCategory;
        this.errorDetails = builder.errorDetails;
        this.timestamp = LocalDateTime.now().toString();
        this.operation = builder.operation;
        log.error("Database exception occurred: {} - {}", errorCode, getMessage(), getCause());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode;
        private String errorCategory = "DATABASE";
        private Map<String, Object> errorDetails = new HashMap<>();
        private Throwable cause;
        private String operation;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorCategory(String errorCategory) {
            this.errorCategory = errorCategory;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder addErrorDetail(String key, Object value) {
            this.errorDetails.put(key, value);
            return this;
        }

        public InfrastructureExceptionDatabase build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            if (errorCode == null) {
                throw new IllegalStateException("Error code cannot be null");
            }
            return new InfrastructureExceptionDatabase(this);
        }
    }

    /**
     * Maps infrastructure exceptions to domain exceptions.
     *
     * @return appropriate domain exception
     */
    public RuntimeException toDomainException() {
        log.debug("Mapping database exception to domain exception: {}", errorCode);

        switch (errorCode) {
            case ERROR_COLLISION:
            case ERROR_DUPLICATE:
                return DomainExceptionCollision.builder()
                    .message(String.format("Key collision in database: %s", getMessage()))
                    .errorCode(DomainExceptionCollision.ERROR_CODE_KEY_GENERATION)
                    .errorCategory("DATABASE")
                    .addContextInfo("operation", operation)
                    .addContextInfo("timestamp", timestamp)
                    .cause(this)
                    .build();

            case ERROR_INVALID_KEY:
            case ERROR_NOT_FOUND:
                return DomainExceptionInvalidKey.builder()
                    .message(String.format("Invalid key operation: %s", getMessage()))
                    .errorCode(DomainExceptionInvalidKey.ERROR_CODE_NOT_FOUND)
                    .errorCategory("DATABASE")
                    .addContextInfo("operation", operation)
                    .addContextInfo("timestamp", timestamp)
                    .cause(this)
                    .build();

            case ERROR_CONNECTION:
            case ERROR_TIMEOUT:
            case ERROR_NETWORK:
                return DomainExceptionCollision.builder()
                    .message(String.format("Database connection error: %s", getMessage()))
                    .errorCode(DomainExceptionCollision.ERROR_CODE_SAVE_OPERATION)
                    .errorCategory("INFRASTRUCTURE")
                    .addContextInfo("operation", operation)
                    .addContextInfo("timestamp", timestamp)
                    .cause(this)
                    .build();

            default:
                return DomainExceptionCollision.builder()
                    .message(String.format("Database operation failed: %s", getMessage()))
                    .errorCode(DomainExceptionCollision.ERROR_CODE_SAVE_OPERATION)
                    .errorCategory("UNKNOWN")
                    .addContextInfo("operation", operation)
                    .addContextInfo("timestamp", timestamp)
                    .cause(this)
                    .build();
        }
    }

    /**
     * Creates a collision exception.
     *
     * @param key The key that caused the collision
     * @param operation The operation being performed
     * @return InfrastructureExceptionDatabase
     */
    public static InfrastructureExceptionDatabase collision(String key, String operation) {
        return builder()
                .message(String.format("Collision detected for key: %s", key))
                .errorCode(ERROR_COLLISION)
                .errorCategory("DATA_ACCESS")
                .operation(operation)
                .addErrorDetail("key", key)
                .build();
    }

    /**
     * Creates a connection error exception.
     *
     * @param details Connection error details
     * @param cause The underlying cause
     * @return InfrastructureExceptionDatabase
     */
    public static InfrastructureExceptionDatabase connectionError(String details, Throwable cause) {
        return builder()
                .message(String.format("Database connection error: %s", details))
                .errorCode(ERROR_CONNECTION)
                .errorCategory("CONNECTION")
                .operation("CONNECT")
                .cause(cause)
                .addErrorDetail("details", details)
                .build();
    }

    @Override
    public String toString() {
        return String.format("DatabaseException[code=%s, category=%s, operation=%s]: %s", 
            errorCode, errorCategory, operation, getMessage());
    }
}