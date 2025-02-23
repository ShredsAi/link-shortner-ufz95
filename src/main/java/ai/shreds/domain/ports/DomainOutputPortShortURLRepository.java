package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityShortURL;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository port for URL shortening persistence operations.
 * Defines data access operations for short URL entities.
 */
public interface DomainOutputPortShortURLRepository {

    /**
     * Saves or updates a short URL entity.
     *
     * @param entity The entity to save
     * @return The saved entity
     */
    DomainEntityShortURL saveShortURL(DomainEntityShortURL entity);

    /**
     * Retrieves a short URL entity by its key.
     *
     * @param shortKey The short key to look up
     * @return The found entity or null
     */
    DomainEntityShortURL findByShortKey(String shortKey);

    /**
     * Checks if a short key exists.
     *
     * @param shortKey The short key to check
     * @return true if the key exists
     */
    boolean exists(String shortKey);

    /**
     * Saves multiple short URL entities in batch.
     *
     * @param entities List of entities to save
     * @return List of saved entities
     */
    List<DomainEntityShortURL> saveBatch(List<DomainEntityShortURL> entities);

    /**
     * Finds all expired short URLs.
     *
     * @param expirationDate The date to check against
     * @return List of expired entities
     */
    List<DomainEntityShortURL> findExpired(LocalDateTime expirationDate);

    /**
     * Deletes expired short URLs.
     *
     * @param expirationDate The date to check against
     * @return Number of deleted entities
     */
    long deleteExpired(LocalDateTime expirationDate);

    /**
     * Updates usage statistics for a short URL.
     *
     * @param shortKey The short key to update
     * @return Updated entity
     */
    DomainEntityShortURL incrementUsageCount(String shortKey);

    /**
     * Finds URLs by usage count range.
     *
     * @param minUsage Minimum usage count
     * @param maxUsage Maximum usage count
     * @return List of matching entities
     */
    List<DomainEntityShortURL> findByUsageCountBetween(int minUsage, int maxUsage);

    /**
     * Invalidates a short URL.
     *
     * @param shortKey The short key to invalidate
     * @return true if successful
     */
    boolean invalidate(String shortKey);

    /**
     * Updates expiration time for a short URL.
     *
     * @param shortKey The short key to update
     * @param expirationTime New expiration time
     * @return Updated entity
     */
    DomainEntityShortURL updateExpiration(String shortKey, LocalDateTime expirationTime);

    /**
     * Performs cleanup of invalid or expired URLs.
     *
     * @return Number of cleaned up records
     */
    long cleanup();
}