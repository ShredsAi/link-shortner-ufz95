package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.ports.DomainOutputPortShortURLRepository;
import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabase;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InfrastructureRepositoryImplShortURL implements DomainOutputPortShortURLRepository {

    private static final String CACHE_NAME = "shortUrls";
    private final InfrastructureNoSqlOperations noSqlTemplate;
    private final MeterRegistry meterRegistry;

    public InfrastructureRepositoryImplShortURL(InfrastructureNoSqlOperations noSqlTemplate,
                                               MeterRegistry meterRegistry) {
        this.noSqlTemplate = noSqlTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @CachePut(value = CACHE_NAME, key = "#entity.shortKey")
    public DomainEntityShortURL saveShortURL(DomainEntityShortURL entity) {
        try {
            log.debug("Saving short URL entity with key: {}", entity.getShortKey());
            meterRegistry.counter("shorturl.repository.save.attempts").increment();

            if (exists(entity.getShortKey())) {
                handleCollision(entity.getShortKey());
            }

            InfrastructureNoSqlRecord record = mapToNoSqlRecord(entity);
            noSqlTemplate.saveWithExpiration(
                entity.getShortKey(),
                record,
                java.time.Duration.between(LocalDateTime.now(), entity.getExpirationTime())
            );

            meterRegistry.counter("shorturl.repository.save.success").increment();
            return entity;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.save.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public List<DomainEntityShortURL> saveBatch(List<DomainEntityShortURL> entities) {
        try {
            log.debug("Saving batch of {} short URL entities", entities.size());
            meterRegistry.counter("shorturl.repository.savebatch.attempts").increment();

            Map<String, Object> records = entities.stream()
                .collect(Collectors.toMap(
                    DomainEntityShortURL::getShortKey,
                    this::mapToNoSqlRecord
                ));

            noSqlTemplate.saveBatch(records);
            meterRegistry.counter("shorturl.repository.savebatch.success").increment();
            return entities;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.savebatch.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#shortKey")
    public DomainEntityShortURL findByShortKey(String shortKey) {
        try {
            log.debug("Finding short URL entity by key: {}", shortKey);
            meterRegistry.counter("shorturl.repository.find.attempts").increment();

            Object record = noSqlTemplate.findByKey(shortKey);
            if (record == null) {
                return null;
            }

            if (!(record instanceof InfrastructureNoSqlRecord)) {
                throw new InfrastructureExceptionDatabase(
                    "Invalid record type retrieved from database",
                    "INVALID_RECORD_TYPE"
                );
            }

            meterRegistry.counter("shorturl.repository.find.success").increment();
            return mapFromNoSqlRecord((InfrastructureNoSqlRecord) record);
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.find.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public List<DomainEntityShortURL> findExpired(LocalDateTime expirationDate) {
        try {
            log.debug("Finding expired short URLs before: {}", expirationDate);
            meterRegistry.counter("shorturl.repository.findexpired.attempts").increment();

            List<String> expiredKeys = noSqlTemplate.findKeysByPattern("*");
            List<DomainEntityShortURL> expiredUrls = new ArrayList<>();

            for (String key : expiredKeys) {
                long ttl = noSqlTemplate.getTimeToLive(key);
                if (ttl <= 0) {
                    DomainEntityShortURL entity = findByShortKey(key);
                    if (entity != null) {
                        expiredUrls.add(entity);
                    }
                }
            }

            meterRegistry.counter("shorturl.repository.findexpired.success").increment();
            return expiredUrls;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.findexpired.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public long deleteExpired(LocalDateTime expirationDate) {
        try {
            log.debug("Deleting expired short URLs before: {}", expirationDate);
            meterRegistry.counter("shorturl.repository.deleteexpired.attempts").increment();

            List<DomainEntityShortURL> expiredUrls = findExpired(expirationDate);
            List<String> expiredKeys = expiredUrls.stream()
                .map(DomainEntityShortURL::getShortKey)
                .collect(Collectors.toList());

            long deletedCount = noSqlTemplate.deleteBatch(expiredKeys);
            meterRegistry.counter("shorturl.repository.deleteexpired.success").increment();
            return deletedCount;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.deleteexpired.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    @CachePut(value = CACHE_NAME, key = "#shortKey")
    public DomainEntityShortURL incrementUsageCount(String shortKey) {
        try {
            log.debug("Incrementing usage count for key: {}", shortKey);
            meterRegistry.counter("shorturl.repository.increment.attempts").increment();

            DomainEntityShortURL entity = findByShortKey(shortKey);
            if (entity != null) {
                entity.incrementUsageCount("UNKNOWN", "UNKNOWN", "SYSTEM");
                return saveShortURL(entity);
            }
            return null;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.increment.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public List<DomainEntityShortURL> findByUsageCountBetween(int minUsage, int maxUsage) {
        try {
            log.debug("Finding URLs with usage count between {} and {}", minUsage, maxUsage);
            meterRegistry.counter("shorturl.repository.findbyusage.attempts").increment();

            List<String> allKeys = noSqlTemplate.findKeysByPattern("*");
            return allKeys.stream()
                .map(this::findByShortKey)
                .filter(entity -> entity != null &&
                        entity.getUsageCount() >= minUsage &&
                        entity.getUsageCount() <= maxUsage)
                .collect(Collectors.toList());
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.findbyusage.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#shortKey")
    public boolean invalidate(String shortKey) {
        try {
            log.debug("Invalidating short URL with key: {}", shortKey);
            meterRegistry.counter("shorturl.repository.invalidate.attempts").increment();

            DomainEntityShortURL entity = findByShortKey(shortKey);
            if (entity != null) {
                entity.setValidityChecks(false);
                saveShortURL(entity);
                return true;
            }
            return false;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.invalidate.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    @CachePut(value = CACHE_NAME, key = "#shortKey")
    public DomainEntityShortURL updateExpiration(String shortKey, LocalDateTime expirationTime) {
        try {
            log.debug("Updating expiration for key: {} to {}", shortKey, expirationTime);
            meterRegistry.counter("shorturl.repository.updateexpiration.attempts").increment();

            DomainEntityShortURL entity = findByShortKey(shortKey);
            if (entity != null) {
                DomainEntityShortURL updatedEntity = DomainEntityShortURL.builder()
                    .shortKey(entity.getShortKey())
                    .originalUrl(entity.getOriginalUrl())
                    .createTimestamp(entity.getCreateTimestamp())
                    .usageCount(entity.getUsageCount())
                    .validityChecks(entity.isValidityChecks())
                    .optionalMetadata(entity.getOptionalMetadata())
                    .expirationTime(expirationTime)
                    .build();

                return saveShortURL(updatedEntity);
            }
            return null;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.updateexpiration.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public long cleanup() {
        try {
            log.debug("Performing repository cleanup");
            meterRegistry.counter("shorturl.repository.cleanup.attempts").increment();

            long cleanedUp = noSqlTemplate.cleanup();
            meterRegistry.counter("shorturl.repository.cleanup.success").increment();
            return cleanedUp;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.cleanup.errors").increment();
            return handleDatabaseException(e);
        }
    }

    @Override
    public boolean exists(String shortKey) {
        try {
            log.debug("Checking existence of key: {}", shortKey);
            meterRegistry.counter("shorturl.repository.exists.attempts").increment();

            boolean exists = noSqlTemplate.exists(shortKey);
            meterRegistry.counter("shorturl.repository.exists.success").increment();
            return exists;
        } catch (Exception e) {
            meterRegistry.counter("shorturl.repository.exists.errors").increment();
            return handleDatabaseException(e);
        }
    }

    private void handleCollision(String shortKey) {
        log.error("Collision detected for short key: {}", shortKey);
        throw new InfrastructureExceptionDatabase(
            String.format("Collision detected for shortKey: %s", shortKey),
            "COLLISION"
        );
    }

    private InfrastructureNoSqlRecord mapToNoSqlRecord(DomainEntityShortURL entity) {
        return InfrastructureNoSqlRecord.builder()
            .shortKey(entity.getShortKey())
            .createTimestamp(entity.getCreateTimestamp())
            .usageCount(entity.getUsageCount())
            .originalUrl(entity.getOriginalUrl())
            .validityChecks(entity.isValidityChecks())
            .optionalMetadata(entity.getOptionalMetadata())
            .expirationTime(entity.getExpirationTime())
            .isCustomAlias(entity.isCustomAlias())
            .accessByCountry(entity.getAccessByCountry())
            .accessByDevice(entity.getAccessByDevice())
            .build();
    }

    private DomainEntityShortURL mapFromNoSqlRecord(InfrastructureNoSqlRecord record) {
        return DomainEntityShortURL.builder()
            .shortKey(record.getShortKey())
            .createTimestamp(record.getCreateTimestamp())
            .usageCount(record.getUsageCount())
            .originalUrl(record.getOriginalUrl())
            .validityChecks(record.isValidityChecks())
            .optionalMetadata(record.getOptionalMetadata())
            .expirationTime(record.getExpirationTime())
            .isCustomAlias(record.isCustomAlias())
            .accessByCountry(record.getAccessByCountry())
            .accessByDevice(record.getAccessByDevice())
            .build();
    }

    private <T> T handleDatabaseException(Exception e) {
        if (e instanceof InfrastructureExceptionDatabase) {
            throw (InfrastructureExceptionDatabase) e;
        } else {
            log.error("Database operation failed", e);
            throw new InfrastructureExceptionDatabase(
                String.format("Database operation failed: %s", e.getMessage()),
                "DB_ERROR",
                e
            );
        }
    }
}