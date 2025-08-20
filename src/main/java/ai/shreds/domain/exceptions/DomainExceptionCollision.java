package ai.shreds.domain.exceptions;

public class DomainExceptionCollision extends RuntimeException {
    public DomainExceptionCollision(String message) {
        super(message);
    }
}