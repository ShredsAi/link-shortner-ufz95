package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;
import ai.shreds.shared.exceptions.SharedExceptionValidation;
import ai.shreds.shared.exceptions.SharedExceptionNotFound;
import ai.shreds.shared.exceptions.SharedShortUrlException;

/**
 * Application service port for URL shortening operations.
 * This interface defines the primary use cases for URL shortening functionality.
 * Implementations should ensure thread safety and proper error handling.
 */
public interface ApplicationInputPortShortURLService {

    /**
     * Creates a short URL from the original URL provided in the request.
     * The generated short key will be unique and follow the system's key generation rules.
     *
     * @param request DTO containing:
     *               - originalUrl: The URL to be shortened (required)
     *               - expirationTimeInSeconds: Optional TTL for the short URL
     *               - customMetadata: Optional map of metadata key-value pairs
     * @return DTO containing:
     *         - shortKey: The generated unique identifier
     *         - originalUrl: The input URL
     *         - shortUrl: The complete shortened URL
     *         - createdAt: Creation timestamp
     *         - expiresAt: Optional expiration timestamp
     *         - usageCount: Number of times the URL was accessed
     * @throws SharedExceptionValidation if:
     *         - URL is null or empty
     *         - URL format is invalid
     *         - URL uses blocked domains
     *         - URL length exceeds system limits
     * @throws SharedShortUrlException if:
     *         - Key generation fails after maximum retries
     *         - System cannot handle the request due to resource constraints
     */
    SharedShortURLResponseDTO createShortURL(SharedCreateShortURLRequestDTO request);

    /**
     * Creates a short URL with a custom alias.
     * The alias must be unique and meet system requirements.
     *
     * @param request DTO containing:
     *               - originalUrl: The URL to be shortened (required)
     *               - customAlias: The requested custom short key (required)
     *               - expirationTimeInSeconds: Optional TTL for the short URL
     * @return DTO with the same structure as createShortURL
     * @throws SharedExceptionValidation if:
     *         - Custom alias is null, empty, or invalid format
     *         - Custom alias contains restricted characters
     *         - Custom alias length is outside allowed range (3-32 chars)
     * @throws SharedShortUrlException if:
     *         - Custom alias is already in use
     *         - Custom alias is in restricted list
     */
    SharedShortURLResponseDTO createCustomShortURL(SharedCreateShortURLRequestDTO request);

    /**
     * Retrieves the original URL and metadata associated with a short key.
     * Increments usage counter and validates URL status.
     *
     * @param shortKey Unique identifier for the shortened URL (case-sensitive)
     * @return DTO containing URL information and current statistics
     * @throws SharedExceptionValidation if:
     *         - Short key format is invalid
     *         - Short key length is outside valid range
     * @throws SharedExceptionNotFound if:
     *         - No URL exists for the provided short key
     *         - URL was deleted or deactivated
     * @throws SharedShortUrlException if:
     *         - URL has expired
     *         - URL is blocked or flagged as malicious
     *         - Access rate limit exceeded
     */
    SharedShortURLResponseDTO getOriginalURL(String shortKey);

    /**
     * Retrieves detailed statistics for a short URL.
     * Includes creation time, expiration, usage patterns, and metadata.
     *
     * @param shortKey Unique identifier for the shortened URL
     * @return DTO containing:
     *         - All standard URL information
     *         - Extended usage statistics
     *         - Creation and expiration timestamps
     *         - Custom metadata
     *         - Access patterns and geographic data if available
     * @throws SharedExceptionNotFound if URL not found
     * @throws SharedShortUrlException if statistics are unavailable
     */
    SharedShortURLResponseDTO getUrlStatistics(String shortKey);

    /**
     * Validates a URL for shortening.
     * Performs comprehensive checks on URL format, content, and security.
     *
     * @param url The URL to validate
     * @return true if the URL is valid and can be shortened
     * @throws SharedExceptionValidation if:
     *         - URL is null or empty
     *         - URL format is invalid
     *         - URL uses non-HTTP/HTTPS protocol
     *         - URL contains malicious patterns
     *         - URL points to blocked domains
     *         - URL length exceeds system limits (2048 chars)
     */
    boolean validateUrl(String url);

    /**
     * Checks if a short URL has expired.
     * Considers both explicit expiration time and system validity rules.
     *
     * @param shortKey Unique identifier for the shortened URL
     * @return true if:
     *         - Explicit expiration time has passed
     *         - URL has been marked as expired by system rules
     *         - URL has exceeded maximum allowed lifetime
     * @throws SharedExceptionNotFound if URL not found
     * @throws SharedShortUrlException if expiration check fails
     */
    boolean isUrlExpired(String shortKey);
}
