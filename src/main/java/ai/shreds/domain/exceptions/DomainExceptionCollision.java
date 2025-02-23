package ai.shreds.domain.exceptions;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when a collision occurs in the domain layer.
 * Typically used when generating short keys or handling concurrent modifications.
 */
@Getter
public class DomainExceptionCollision extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    // Key Generation Error Codes
    public static final String ERROR_CODE_KEY_GENERATION = "COLLISION_001";
    public static final String ERROR_CODE_KEY_EXHAUSTED = "COLLISION_002";
    public static final String ERROR_CODE_CUSTOM_ALIAS_EXISTS = "COLLISION_003";

    // Storage Operation Error Codes
    public static final String ERROR_CODE_SAVE_OPERATION = "COLLISION_004";
    public static final String ERROR_CODE_UPDATE_OPERATION = "COLLISION_005";
    public static final String ERROR_CODE_DELETE_OPERATION = "COLLISION_006";

    // Concurrent Operation Error Codes
    public static final String ERROR_CODE_CONCURRENT_MODIFICATION = "COLLISION_007";
    public static final String ERROR_CODE_RACE_CONDITION = "COLLISION_008";
    public static final String ERROR_CODE_LOCK_ACQUISITION = "COLLISION_009";

    private final String errorCode;
    private final String errorCategory;
    private final Map<String, Object> errorContext;
    private final String timestamp;
    private final int retryAttempt;
    private final String collisionKey;

    private DomainExceptionCollision(Builder builder) {
        super(builder.message, builder.cause);
        this.errorCode = builder.errorCode;
        this.errorCategory = builder.errorCategory;
        this.errorContext = new HashMap<>(builder.errorContext);
        this.timestamp = LocalDateTime.now().toString();
        this.retryAttempt = builder.retryAttempt;
        this.collisionKey = builder.collisionKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode = ERROR_CODE_KEY_GENERATION;
        private String errorCategory = "KEY_COLLISION";
        private Map<String, Object> errorContext = new HashMap<>();
        private Throwable cause;
        private int retryAttempt;
        private String collisionKey;

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

        public Builder retryAttempt(int retryAttempt) {
            this.retryAttempt = retryAttempt;
            return this;
        }

        public Builder collisionKey(String collisionKey) {
            this.collisionKey = collisionKey;
            return this;
        }

        public Builder addContextInfo(String key, Object value) {
            if (key != null && value != null) {
                this.errorContext.put(key, value);
            }
            return this;
        }

        public DomainExceptionCollision build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            return new DomainExceptionCollision(this);
        }
    }

    /**
     * Creates a key generation collision exception.
     *
     * @param key The colliding key
     * @param attempt The retry attempt number
     * @return DomainExceptionCollision
     */
    public static DomainExceptionCollision keyGenerationCollision(String key, int attempt) {
        return builder()
                .message("Collision detected while generating key")
                .errorCode(ERROR_CODE_KEY_GENERATION)
                .errorCategory("KEY_GENERATION")
                .collisionKey(key)
                .retryAttempt(attempt)
                .addContextInfo("collisionKey", key)
                .addContextInfo("attemptNumber", attempt)
                .build();
    }

    /**
     * Creates a custom alias collision exception.
     *
     * @param alias The custom alias that already exists
     * @return DomainExceptionCollision
     */
    public static DomainExceptionCollision customAliasCollision(String alias) {
        return builder()
                .message("Custom alias already exists")
                .errorCode(ERROR_CODE_CUSTOM_ALIAS_EXISTS)
                .errorCategory("CUSTOM_ALIAS")
                .collisionKey(alias)
                .addContextInfo("customAlias", alias)
                .build();
    }

    /**
     * Creates a concurrent modification collision exception.
     *
     * @param key The key being modified
     * @param operation The operation being performed
     * @return DomainExceptionCollision
     */
    public static DomainExceptionCollision concurrentModification(String key, String operation) {
        return builder()
                .message("Concurrent modification detected")
                .errorCode(ERROR_CODE_CONCURRENT_MODIFICATION)
                .errorCategory("CONCURRENCY")
                .collisionKey(key)
                .addContextInfo("key", key)
                .addContextInfo("operation", operation)
                .build();
    }

    @Override
    public String toString() {
        return String.format("DomainExceptionCollision[code=%s, category=%s, message=%s, context=%s]", 
            errorCode, errorCategory, getMessage(), errorContext);
    }
}