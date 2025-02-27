package ai.shreds.infrastructure.repositories;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface InfrastructureNoSqlOperations {
    void save(String key, Object record);
    Object findByKey(String key);
    boolean exists(String key);
    void saveWithExpiration(String key, Object record, Duration ttl);
    void saveBatch(Map<String, Object> records);
    Map<String, Object> findByKeys(List<String> keys);
    boolean delete(String key);
    long deleteBatch(List<String> keys);
    boolean updateExpiration(String key, Duration ttl);
    long getTimeToLive(String key);
    long increment(String key);
    List<String> findKeysByPattern(String pattern);
    long cleanup();
}