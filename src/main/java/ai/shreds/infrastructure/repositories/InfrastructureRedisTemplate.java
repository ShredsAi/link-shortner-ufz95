package ai.shreds.infrastructure.repositories;

import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabase;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InfrastructureRedisTemplate implements InfrastructureNoSqlOperations {

    private final RedisTemplate<String, Object> redisTemplate;

    public InfrastructureRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String key, Object record) {
        try {
            redisTemplate.opsForValue().set(key, record);
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to save record", "REDIS_SAVE_ERROR"));
        }
    }

    @Override
    public Object findByKey(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to find record", "REDIS_FIND_ERROR"));
            return null;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to check existence", "REDIS_EXISTS_ERROR"));
            return false;
        }
    }

    @Override
    public void saveWithExpiration(String key, Object record, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, record, ttl);
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to save record with expiration", "REDIS_SAVE_TTL_ERROR"));
        }
    }

    @Override
    public void saveBatch(Map<String, Object> records) {
        try {
            records.forEach((key, value) -> redisTemplate.opsForValue().set(key, value));
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to save batch records", "REDIS_SAVE_BATCH_ERROR"));
        }
    }

    @Override
    public Map<String, Object> findByKeys(List<String> keys) {
        try {
            Map<String, Object> results = new HashMap<>();
            redisTemplate.opsForValue().multiGet(keys).forEach(value -> {
                if (value != null) {
                    results.put(keys.get(results.size()), value);
                }
            });
            return results;
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to find records by keys", "REDIS_FIND_BATCH_ERROR"));
            return new HashMap<>();
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to delete record", "REDIS_DELETE_ERROR"));
            return false;
        }
    }

    @Override
    public long deleteBatch(List<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            return deleted != null ? deleted : 0L;
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to delete batch records", "REDIS_DELETE_BATCH_ERROR"));
            return 0L;
        }
    }

    @Override
    public boolean updateExpiration(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to update expiration", "REDIS_UPDATE_TTL_ERROR"));
            return false;
        }
    }

    @Override
    public long getTimeToLive(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : -1L;
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to get TTL", "REDIS_GET_TTL_ERROR"));
            return -1L;
        }
    }

    @Override
    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            return value != null ? value : 0L;
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to increment value", "REDIS_INCREMENT_ERROR"));
            return 0L;
        }
    }

    @Override
    public List<String> findKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.stream().toList() : List.of();
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to find keys by pattern", "REDIS_FIND_KEYS_ERROR"));
            return List.of();
        }
    }

    @Override
    public long cleanup() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                return deleted != null ? deleted : 0L;
            }
            return 0L;
        } catch (Exception e) {
            handleError(new InfrastructureExceptionDatabase("Failed to cleanup database", "REDIS_CLEANUP_ERROR"));
            return 0L;
        }
    }

    private void handleError(InfrastructureExceptionDatabase e) {
        throw e;
    }
}