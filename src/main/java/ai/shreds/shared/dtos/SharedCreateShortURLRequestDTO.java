package ai.shreds.shared.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object for creating a short URL request.
 * Contains validation for URL format and optional expiration time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedCreateShortURLRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Original URL is required")
    @Pattern(
        regexp = "^(https?://)?((([\w-]+\.)+[\w-]+)|localhost)(:\d+)?(/[\w-./?%&=]*)?$",
        message = "Invalid URL format. URL must be a valid HTTP/HTTPS URL"
    )
    private String originalUrl;

    @Positive(message = "Expiration time must be positive")
    private Long expirationTimeInSeconds;

    @Pattern(
        regexp = "^[a-zA-Z0-9-_]{1,50}$",
        message = "Custom alias must be alphanumeric and can include hyphens and underscores, max 50 characters"
    )
    private String customAlias;

    @Builder.Default
    private Boolean forceCustomAlias = false;

    @Builder.Default
    private Boolean trackMetrics = true;

    /**
     * Validates if the custom alias should be considered.
     * @return true if custom alias should be used
     */
    public boolean hasValidCustomAlias() {
        return customAlias != null && !customAlias.trim().isEmpty();
    }

    /**
     * Checks if the URL should expire.
     * @return true if expiration time is set
     */
    public boolean hasExpiration() {
        return expirationTimeInSeconds != null && expirationTimeInSeconds > 0;
    }

    /**
     * Normalizes the original URL by ensuring it has a protocol.
     * @return normalized URL string
     */
    public String getNormalizedUrl() {
        if (originalUrl == null) return null;
        return originalUrl.startsWith("http") ? originalUrl : "http://" + originalUrl;
    }
}