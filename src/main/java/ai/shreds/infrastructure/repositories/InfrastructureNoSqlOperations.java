package ai.shreds.infrastructure.repositories;

import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabase;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Defines the core operations for NoSQL database interactions.
 * This interface abstracts the underlying NoSQL database operations,
 * providing a consistent API regardless of the implementation.
 */
public interface InfrastructureNoSqlOperations {

    /**
     * Saves a record in the NoSQL database.
     *
     * @param key The unique identifier for the record
     * @param record The record to be saved
     * @throws InfrastructureExceptionDatabase if the save operation fails
     */
    void save(String key, Object record) throws InfrastructureExceptionDatabase;

    /**
     * Saves a record with an expiration time.
     *
     * @param key The unique identifier
     * @param record The record to save
     * @param ttl The time-to-live duration
     * @throws InfrastructureExceptionDatabase if the save operation fails
     */
    void saveWithExpiration(String key, Object record, Duration ttl) throws InfrastructureExceptionDatabase;

    /**
     * Saves multiple records in batch.
     *
     * @param records Map of keys to records
     * @throws InfrastructureExceptionDatabase if the batch save fails
     */
    void saveBatch(Map<String, Object> records) throws InfrastructureExceptionDatabase;

    /**
     * Retrieves a record by its key.
     *
     * @param key The unique identifier
     * @return The retrieved record, or null if not found
     * @throws InfrastructureExceptionDatabase if the retrieval fails
     */
    Object findByKey(String key) throws InfrastructureExceptionDatabase;

    /**
     * Retrieves multiple records by their keys.
     *
     * @param keys List of keys to retrieve
     * @return Map of keys to found records
     * @throws InfrastructureExceptionDatabase if the retrieval fails
     */
    Map<String, Object> findByKeys(List<String> keys) throws InfrastructureExceptionDatabase;

    /**
     * Checks if a key exists in the database.
     *
     * @param key The unique identifier to check
     * @return true if exists, false otherwise
     * @throws InfrastructureExceptionDatabase if the check fails
     */
    boolean exists(String key) throws InfrastructureExceptionDatabase;

    /**
     * Deletes a record by its key.
     *
     * @param key The key to delete
     * @return true if deleted, false if not found
     * @throws InfrastructureExceptionDatabase if the deletion fails
     */
    boolean delete(String key) throws InfrastructureExceptionDatabase;

    /**
     * Deletes multiple records by their keys.
     *
     * @param keys The keys to delete
     * @return Number of records deleted
     * @throws InfrastructureExceptionDatabase if the deletion fails
     */
    long deleteBatch(List<String> keys) throws InfrastructureExceptionDatabase;

    /**
     * Updates the expiration time for a key.
     *
     * @param key The key to update
     * @param ttl New time-to-live duration
     * @return true if updated, false if not found
     * @throws InfrastructureExceptionDatabase if the update fails
     */
    boolean updateExpiration(String key, Duration ttl) throws InfrastructureExceptionDatabase;

    /**
     * Gets the remaining time-to-live for a key.
     *
     * @param key The key to check
     * @return Remaining TTL in seconds, -1 if no TTL, -2 if key doesn't exist
     * @throws InfrastructureExceptionDatabase if the check fails
     */
    long getTimeToLive(String key) throws InfrastructureExceptionDatabase;

    /**
     * Atomically increments a counter value.
     *
     * @param key The counter key
     * @return New counter value
     * @throws InfrastructureExceptionDatabase if the increment fails
     */
    long increment(String key) throws InfrastructureExceptionDatabase;

    /**
     * Finds keys matching a pattern.
     *
     * @param pattern The search pattern (e.g., "user:*")
     * @return List of matching keys
     * @throws InfrastructureExceptionDatabase if the search fails
     */
    List<String> findKeysByPattern(String pattern) throws InfrastructureExceptionDatabase;

    /**
     * Performs cleanup of expired records.
     *
     * @return Number of records cleaned up
     * @throws InfrastructureExceptionDatabase if the cleanup fails
     */
    long cleanup() throws InfrastructureExceptionDatabase;
}
