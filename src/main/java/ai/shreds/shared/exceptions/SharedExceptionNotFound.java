package ai.shreds.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when a requested resource is not found in the URL shortening service.
 * This exception is used specifically for cases where a short URL key doesn't exist.
 */
@Getter
public class SharedExceptionNotFound extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String resourceType;
    private final String resourceId;
    private final Map<String, Object> additionalInfo;
    private final String errorType;
    private final String timestamp;

    private SharedExceptionNotFound(Builder builder) {
        super(builder.message);
        this.errorCode = builder.errorCode;
        this.httpStatus = builder.httpStatus;
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
        this.additionalInfo = builder.additionalInfo;
        this.errorType = "RESOURCE_NOT_FOUND";
        this.timestamp = LocalDateTime.now().toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private String errorCode = "RESOURCE_NOT_FOUND";
        private HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        private String resourceType;
        private String resourceId;
        private Map<String, Object> additionalInfo = new HashMap<>();

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

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder addAdditionalInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
            return this;
        }

        public SharedExceptionNotFound build() {
            if (message == null) {
                throw new IllegalStateException("Message cannot be null");
            }
            if (resourceType == null) {
                throw new IllegalStateException("Resource type cannot be null");
            }
            if (resourceId == null) {
                throw new IllegalStateException("Resource ID cannot be null");
            }
            return new SharedExceptionNotFound(this);
        }
    }
}