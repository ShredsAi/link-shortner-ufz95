package ai.shreds.shared.exceptions;

import ai.shreds.application.exceptions.ApplicationExceptionShortURL;

public class SharedShortUrlException extends RuntimeException {
    private final String errorCode;

    public SharedShortUrlException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SharedShortUrlException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static SharedShortUrlException fromApplicationException(ApplicationExceptionShortURL ex) {
        return new SharedShortUrlException(ex.getMessage(), ex.getErrorCode());
    }
}