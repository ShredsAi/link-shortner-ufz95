package ai.shreds.domain.exceptions;

public class DomainExceptionInvalidKey extends RuntimeException {
    public DomainExceptionInvalidKey(String message) {
        super(message);
    }

    public DomainExceptionInvalidKey(String message, Throwable cause) {
        super(message, cause);
    }
}