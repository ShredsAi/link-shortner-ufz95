package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;

public interface DomainInputPortShortURL {
    /**
     * Generates a short URL for the given original URL
     * @param originalUrl The URL to be shortened
     * @return DomainEntityShortURL containing the generated short key and original URL
     * @throws DomainExceptionCollision if there's a collision with existing short key
     */
    DomainEntityShortURL generateShortKey(String originalUrl);

    /**
     * Retrieves the original URL for a given short key
     * @param shortKey The short key to look up
     * @return DomainEntityShortURL containing the original URL and metadata
     * @throws DomainExceptionInvalidKey if the short key is invalid or not found
     */
    DomainEntityShortURL retrieveOriginalURL(String shortKey);
}
