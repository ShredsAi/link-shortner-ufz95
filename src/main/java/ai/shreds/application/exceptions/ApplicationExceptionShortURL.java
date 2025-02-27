package ai.shreds.application.exceptions;

public class ApplicationExceptionShortURL extends RuntimeException {
    private final String errorCode;

    public ApplicationExceptionShortURL(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationExceptionShortURL(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}