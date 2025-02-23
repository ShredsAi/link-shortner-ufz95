package ai.shreds.application.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Application-level exception for URL shortening service.
 * Provides detailed error information and categorization.
 */
@Getter
public class ApplicationExceptionShortURL extends RuntimeException {

    private final String errorCode;
    private final String errorCategory;
    private final HttpStatus httpStatus;
    private final Map<String, Object> errorDetails;
    private final String timestamp;

    private ApplicationExceptionShortURL(Builder builder) {
        super(builder.message, builder.cause);
        this.errorCode = builder.errorCode;
        this.errorCategory = builder.errorCategory;
        this.httpStatus = builder.httpStatus;
        this.errorDetails = builder.errorDetails;
        this.timestamp = LocalDateTime.now().toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode;
        private String errorCategory = "APPLICATION_ERROR";
        private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
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

        public Builder errorCategory(String errorCategory) {
            this.errorCategory = errorCategory;
            return this;
        }

        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
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

        public ApplicationExceptionShortURL build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            if (errorCode == null) {
                throw new IllegalStateException("Error code cannot be null");
            }
            return new ApplicationExceptionShortURL(this);
        }
    }

    /**
     * Creates a validation error exception.
     *
     * @param message Error message
     * @param details Validation error details
     * @return ApplicationExceptionShortURL
     */
    public static ApplicationExceptionShortURL validationError(String message, Map<String, Object> details) {
        return builder()
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .errorCategory("VALIDATION")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .addErrorDetail("validationErrors", details)
                .build();
    }

    /**
     * Creates a business rule violation exception.
     *
     * @param message Error message
     * @param rule Violated rule name
     * @return ApplicationExceptionShortURL
     */
    public static ApplicationExceptionShortURL businessRuleViolation(String message, String rule) {
        return builder()
                .message(message)
                .errorCode("BUSINESS_RULE_VIOLATION")
                .errorCategory("BUSINESS_RULES")
                .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .addErrorDetail("violatedRule", rule)
                .build();
    }

    /**
     * Creates a system error exception.
     *
     * @param message Error message
     * @param cause Original exception
     * @return ApplicationExceptionShortURL
     */
    public static ApplicationExceptionShortURL systemError(String message, Throwable cause) {
        return builder()
                .message(message)
                .errorCode("SYSTEM_ERROR")
                .errorCategory("SYSTEM")
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .cause(cause)
                .build();
    }
}