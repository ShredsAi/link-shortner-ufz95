package ai.shreds.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * General exception for URL shortening service operations.
 * This exception is used for general operational errors that don't fall into
 * specific categories like validation or not found errors.
 */
@Getter
public class SharedShortUrlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String errorCategory;
    private final Map<String, Object> errorDetails;
    private final String timestamp;
    private final String errorType;

    private SharedShortUrlException(Builder builder) {
        super(builder.message, builder.cause);
        this.errorCode = builder.errorCode;
        this.httpStatus = builder.httpStatus;
        this.errorCategory = builder.errorCategory;
        this.errorDetails = builder.errorDetails;
        this.timestamp = LocalDateTime.now().toString();
        this.errorType = "SHORT_URL_ERROR";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode = "GENERAL_ERROR";
        private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        private String errorCategory = "OPERATIONAL";
        private Map<String, Object> errorDetails = new HashMap<>();
        private Throwable cause;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
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

        public Builder addErrorDetail(String key, Object value) {
            this.errorDetails.put(key, value);
            return this;
        }

        public Builder errorDetails(Map<String, Object> errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public SharedShortUrlException build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            return new SharedShortUrlException(this);
        }
    }

    /**
     * Creates a new exception instance with a formatted message.
     *
     * @param messageFormat The message format string
     * @param args The arguments to be used in the message format
     * @return A new SharedShortUrlException instance
     */
    public static SharedShortUrlException fromFormat(String messageFormat, Object... args) {
        return builder()
                .message(String.format(messageFormat, args))
                .build();
    }

    /**
     * Creates a collision exception with appropriate status and category.
     *
     * @param shortKey The short key that caused the collision
     * @return A new SharedShortUrlException instance
     */
    public static SharedShortUrlException collisionException(String shortKey) {
        return builder()
                .message(String.format("Collision detected for short key: %s", shortKey))
                .errorCode("KEY_COLLISION")
                .errorCategory("DATA_INTEGRITY")
                .httpStatus(HttpStatus.CONFLICT)
                .addErrorDetail("shortKey", shortKey)
                .build();
    }

    /**
     * Creates an expired URL exception.
     *
     * @param shortKey The expired short key
     * @return A new SharedShortUrlException instance
     */
    public static SharedShortUrlException expiredUrlException(String shortKey) {
        return builder()
                .message(String.format("Short URL has expired: %s", shortKey))
                .errorCode("URL_EXPIRED")
                .errorCategory("VALIDATION")
                .httpStatus(HttpStatus.GONE)
                .addErrorDetail("shortKey", shortKey)
                .build();
    }
}