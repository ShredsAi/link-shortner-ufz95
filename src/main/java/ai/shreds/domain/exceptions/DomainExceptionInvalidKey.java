package ai.shreds.domain.exceptions;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when an invalid key is detected in the domain layer.
 * Handles various key validation and format issues.
 */
@Getter
public class DomainExceptionInvalidKey extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    // Key Validation Error Codes
    public static final String ERROR_CODE_NULL_KEY = "INVALID_KEY_001";
    public static final String ERROR_CODE_EMPTY_KEY = "INVALID_KEY_002";
    public static final String ERROR_CODE_FORMAT = "INVALID_KEY_003";
    public static final String ERROR_CODE_LENGTH = "INVALID_KEY_004";
    public static final String ERROR_CODE_CHARACTERS = "INVALID_KEY_005";

    // Key State Error Codes
    public static final String ERROR_CODE_NOT_FOUND = "INVALID_KEY_006";
    public static final String ERROR_CODE_EXPIRED = "INVALID_KEY_007";
    public static final String ERROR_CODE_INACTIVE = "INVALID_KEY_008";
    public static final String ERROR_CODE_BLOCKED = "INVALID_KEY_009";

    // Custom Alias Error Codes
    public static final String ERROR_CODE_INVALID_ALIAS = "INVALID_KEY_010";
    public static final String ERROR_CODE_RESERVED_ALIAS = "INVALID_KEY_011";
    public static final String ERROR_CODE_PROFANITY = "INVALID_KEY_012";

    private final String errorCode;
    private final String errorCategory;
    private final Map<String, Object> errorContext;
    private final String timestamp;
    private final String invalidKey;
    private final Map<String, String> validationErrors;

    private DomainExceptionInvalidKey(Builder builder) {
        super(builder.message, builder.cause);
        this.errorCode = builder.errorCode;
        this.errorCategory = builder.errorCategory;
        this.errorContext = new HashMap<>(builder.errorContext);
        this.timestamp = LocalDateTime.now().toString();
        this.invalidKey = builder.invalidKey;
        this.validationErrors = new HashMap<>(builder.validationErrors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode = ERROR_CODE_FORMAT;
        private String errorCategory = "KEY_VALIDATION";
        private Map<String, Object> errorContext = new HashMap<>();
        private Throwable cause;
        private String invalidKey;
        private Map<String, String> validationErrors = new HashMap<>();

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

        public Builder invalidKey(String invalidKey) {
            this.invalidKey = invalidKey;
            return this;
        }

        public Builder addContextInfo(String key, Object value) {
            if (key != null && value != null) {
                this.errorContext.put(key, value);
            }
            return this;
        }

        public Builder addValidationError(String field, String error) {
            if (field != null && error != null) {
                this.validationErrors.put(field, error);
            }
            return this;
        }

        public DomainExceptionInvalidKey build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            return new DomainExceptionInvalidKey(this);
        }
    }

    /**
     * Creates an exception for null or empty key.
     *
     * @param key The invalid key
     * @return DomainExceptionInvalidKey
     */
    public static DomainExceptionInvalidKey nullOrEmptyKey(String key) {
        return builder()
                .message("Key cannot be null or empty")
                .errorCode(key == null ? ERROR_CODE_NULL_KEY : ERROR_CODE_EMPTY_KEY)
                .errorCategory("KEY_VALIDATION")
                .invalidKey(key)
                .addValidationError("key", "must not be null or empty")
                .build();
    }

    /**
     * Creates an exception for invalid key format.
     *
     * @param key The invalid key
     * @param format The required format
     * @return DomainExceptionInvalidKey
     */
    public static DomainExceptionInvalidKey invalidFormat(String key, String format) {
        return builder()
                .message("Invalid key format")
                .errorCode(ERROR_CODE_FORMAT)
                .errorCategory("KEY_VALIDATION")
                .invalidKey(key)
                .addContextInfo("requiredFormat", format)
                .addValidationError("key", "must match format: " + format)
                .build();
    }

    /**
     * Creates an exception for expired key.
     *
     * @param key The expired key
     * @param expirationTime The expiration time
     * @return DomainExceptionInvalidKey
     */
    public static DomainExceptionInvalidKey expired(String key, LocalDateTime expirationTime) {
        return builder()
                .message("Key has expired")
                .errorCode(ERROR_CODE_EXPIRED)
                .errorCategory("KEY_STATE")
                .invalidKey(key)
                .addContextInfo("expirationTime", expirationTime)
                .addValidationError("key", "expired at " + expirationTime)
                .build();
    }

    @Override
    public String toString() {
        return String.format("DomainExceptionInvalidKey[code=%s, category=%s, message=%s, context=%s, validationErrors=%s]", 
            errorCode, errorCategory, getMessage(), errorContext, validationErrors);
    }
}