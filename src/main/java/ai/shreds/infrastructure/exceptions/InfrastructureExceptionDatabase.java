package ai.shreds.infrastructure.exceptions;

import ai.shreds.domain.exceptions.DomainExceptionCollision;

public class InfrastructureExceptionDatabase extends RuntimeException {
    private final String message;
    private final String errorCode;

    public InfrastructureExceptionDatabase(String message, String errorCode) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public DomainExceptionCollision toDomainException() {
        return new DomainExceptionCollision(this.message);
    }
}