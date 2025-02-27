package ai.shreds.shared.exceptions;

public class SharedExceptionNotFound extends RuntimeException {
    public SharedExceptionNotFound(String message) {
        super(message);
    }

    public SharedExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}