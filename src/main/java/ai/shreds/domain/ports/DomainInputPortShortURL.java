package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;

/**
 * Domain port for URL shortening operations.
 * Defines core business operations for URL shortening service.
 */
public interface DomainInputPortShortURL {

    /**
     * Generates a unique short key for a given URL.
     *
     * @param originalUrl The URL to be shortened
     * @return Domain entity containing the short key and original URL
     * @throws DomainExceptionCollision if a collision occurs during key generation
     * @throws DomainExceptionInvalidKey if the generated key is invalid
     */
    DomainEntityShortURL generateShortKey(String originalUrl);

    /**
     * Creates a short URL with a custom alias.
     *
     * @param originalUrl The URL to be shortened
     * @param customAlias The requested custom alias
     * @return Domain entity containing the custom alias and original URL
     * @throws DomainExceptionCollision if the custom alias is already in use
     * @throws DomainExceptionInvalidKey if the custom alias is invalid
     */
    DomainEntityShortURL createCustomShortURL(String originalUrl, String customAlias);

    /**
     * Retrieves the original URL for a given short key.
     *
     * @param shortKey The short key to look up
     * @return Domain entity containing the URL information
     * @throws DomainExceptionInvalidKey if the short key is invalid
     */
    DomainEntityShortURL retrieveOriginalURL(String shortKey);

    /**
     * Validates a URL against domain rules.
     *
     * @param url The URL to validate
     * @return true if the URL is valid according to domain rules
     */
    boolean validateUrl(String url);

    /**
     * Checks if a short URL has expired.
     *
     * @param shortKey The short key to check
     * @return true if the URL has expired
     */
    boolean isExpired(String shortKey);

    /**
     * Retrieves usage statistics for a short URL.
     *
     * @param shortKey The short key to get statistics for
     * @return Domain entity containing usage statistics
     */
    DomainEntityShortURL getStatistics(String shortKey);

    /**
     * Invalidates a short URL.
     *
     * @param shortKey The short key to invalidate
     * @return true if the URL was successfully invalidated
     */
    boolean invalidateUrl(String shortKey);

    /**
     * Updates the expiration time for a short URL.
     *
     * @param shortKey The short key to update
     * @param expirationTimeInSeconds New expiration time in seconds
     * @return Updated domain entity
     */
    DomainEntityShortURL updateExpiration(String shortKey, Long expirationTimeInSeconds);
}