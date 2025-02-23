package ai.shreds.infrastructure.repositories;

import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfrastructureRedisTemplate implements InfrastructureNoSqlOperations {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    public InfrastructureRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }

    @Override
    public void save(String key, Object record) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Saving record with key: {}", key);
            valueOps.set(key, record);
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to save record to Redis: %s", e.getMessage()),
                "REDIS_SAVE_ERROR",
                e
            );
        }
    }

    @Override
    public void saveWithExpiration(String key, Object record, Duration ttl) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Saving record with key: {} and TTL: {}", key, ttl);
            valueOps.set(key, record, ttl);
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to save record with TTL to Redis: %s", e.getMessage()),
                "REDIS_SAVE_TTL_ERROR",
                e
            );
        }
    }

    @Override
    public void saveBatch(Map<String, Object> records) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Saving batch of {} records", records.size());
            redisTemplate.execute((RedisConnection connection) -> {
                records.forEach((key, value) -> {
                    byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
                    byte[] rawValue = redisTemplate.getValueSerializer().serialize(value);
                    connection.stringCommands().set(rawKey, rawValue);
                });
                return null;
            });
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to save batch to Redis: %s", e.getMessage()),
                "REDIS_BATCH_SAVE_ERROR",
                e
            );
        }
    }

    @Override
    public Object findByKey(String key) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Finding record by key: {}", key);
            return valueOps.get(key);
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to retrieve record from Redis: %s", e.getMessage()),
                "REDIS_GET_ERROR",
                e
            );
        }
    }

    @Override
    public Map<String, Object> findByKeys(List<String> keys) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Finding records for {} keys", keys.size());
            return valueOps.multiGet(keys)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    k -> keys.get(keys.indexOf(k)),
                    v -> v,
                    (v1, v2) -> v1
                ));
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to retrieve multiple records from Redis: %s", e.getMessage()),
                "REDIS_MULTI_GET_ERROR",
                e
            );
        }
    }

    @Override
    public boolean exists(String key) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Checking existence of key: {}", key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to check key existence in Redis: %s", e.getMessage()),
                "REDIS_EXISTS_ERROR",
                e
            );
        }
    }

    @Override
    public boolean delete(String key) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Deleting key: {}", key);
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to delete key from Redis: %s", e.getMessage()),
                "REDIS_DELETE_ERROR",
                e
            );
        }
    }

    @Override
    public long deleteBatch(List<String> keys) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Deleting batch of {} keys", keys.size());
            Long deleted = redisTemplate.delete(keys);
            return deleted != null ? deleted : 0L;
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to delete batch from Redis: %s", e.getMessage()),
                "REDIS_BATCH_DELETE_ERROR",
                e
            );
        }
    }

    @Override
    public boolean updateExpiration(String key, Duration ttl) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Updating expiration for key: {} to {}", key, ttl);
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to update expiration in Redis: %s", e.getMessage()),
                "REDIS_EXPIRE_ERROR",
                e
            );
        }
    }

    @Override
    public long getTimeToLive(String key) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Getting TTL for key: {}", key);
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : -2L;
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to get TTL from Redis: %s", e.getMessage()),
                "REDIS_TTL_ERROR",
                e
            );
        }
    }

    @Override
    public long increment(String key) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Incrementing counter for key: {}", key);
            Long value = valueOps.increment(key);
            return value != null ? value : 0L;
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to increment counter in Redis: %s", e.getMessage()),
                "REDIS_INCREMENT_ERROR",
                e
            );
        }
    }

    @Override
    public List<String> findKeysByPattern(String pattern) throws InfrastructureExceptionDatabase {
        try {
            log.debug("Finding keys by pattern: {}", pattern);
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? new ArrayList<>(keys) : new ArrayList<>();
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to find keys by pattern in Redis: %s", e.getMessage()),
                "REDIS_PATTERN_ERROR",
                e
            );
        }
    }

    @Override
    public long cleanup() throws InfrastructureExceptionDatabase {
        try {
            log.debug("Performing Redis cleanup");
            Set<String> expiredKeys = redisTemplate.keys("*");
            if (expiredKeys == null || expiredKeys.isEmpty()) {
                return 0L;
            }
            
            long cleaned = 0;
            for (String key : expiredKeys) {
                if (getTimeToLive(key) <= 0) {
                    if (delete(key)) {
                        cleaned++;
                    }
                }
            }
            return cleaned;
        } catch (Exception e) {
            throw new InfrastructureExceptionDatabase(
                String.format("Failed to perform Redis cleanup: %s", e.getMessage()),
                "REDIS_CLEANUP_ERROR",
                e
            );
        }
    }
}
