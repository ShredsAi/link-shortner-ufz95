package ai.shreds.adapter.exceptions;

import ai.shreds.shared.exceptions.SharedExceptionNotFound;
import ai.shreds.shared.exceptions.SharedExceptionValidation;
import ai.shreds.shared.exceptions.SharedShortUrlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class AdapterShortURLExceptionHandler {

    @ExceptionHandler(SharedExceptionValidation.class)
    public ResponseEntity<Map<String, String>> handleValidationException(SharedExceptionValidation e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Validation Error");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SharedExceptionNotFound.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(SharedExceptionNotFound e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(SharedShortUrlException.class)
    public ResponseEntity<Map<String, String>> handleShortUrlException(SharedShortUrlException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", e.getErrorCode());
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleInternalError(Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
