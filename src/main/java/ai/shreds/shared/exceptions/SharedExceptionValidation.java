package ai.shreds.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails in the URL shortening service.
 * This exception is used to indicate invalid input data or business rule violations.
 */
@Getter
public class SharedExceptionValidation extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, String> validationErrors;
    private final String errorType;
    private final String timestamp;

    private SharedExceptionValidation(Builder builder) {
        super(builder.message);
        this.errorCode = builder.errorCode;
        this.httpStatus = builder.httpStatus;
        this.validationErrors = builder.validationErrors;
        this.errorType = "VALIDATION_ERROR";
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode = "VALIDATION_ERROR";
        private HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        private Map<String, String> validationErrors = new HashMap<>();

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

        public Builder addValidationError(String field, String error) {
            this.validationErrors.put(field, error);
            return this;
        }

        public Builder validationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public SharedExceptionValidation build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            return new SharedExceptionValidation(this);
        }
    }
}