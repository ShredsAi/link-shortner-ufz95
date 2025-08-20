package ai.shreds.shared.exceptions;

public class SharedExceptionValidation extends RuntimeException {
    public SharedExceptionValidation(String message) {
        super(message);
    }

    public SharedExceptionValidation(String message, Throwable cause) {
        super(message, cause);
    }
}