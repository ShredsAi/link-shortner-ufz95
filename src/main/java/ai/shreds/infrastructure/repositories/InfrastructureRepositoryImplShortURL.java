package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;
import ai.shreds.domain.ports.DomainOutputPortShortURLRepository;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionDatabase;

import org.springframework.stereotype.Repository;

@Repository
public class InfrastructureRepositoryImplShortURL implements DomainOutputPortShortURLRepository {

    private final InfrastructureNoSqlOperations noSqlOperations;

    public InfrastructureRepositoryImplShortURL(InfrastructureNoSqlOperations noSqlOperations) {
        this.noSqlOperations = noSqlOperations;
    }

    @Override
    public DomainEntityShortURL saveShortURL(DomainEntityShortURL entity) {
        try {
            InfrastructureNoSqlRecord record = mapToNoSqlRecord(entity);
            noSqlOperations.save(entity.getShortKey(), record);
            return entity;
        } catch (Exception e) {
            handleDatabaseException(e);
            return null; // Never reached due to exception handling
        }
    }

    @Override
    public DomainEntityShortURL findByShortKey(String shortKey) {
        try {
            Object record = noSqlOperations.findByKey(shortKey);
            if (record == null) {
                return null;
            }
            return mapFromNoSqlRecord((InfrastructureNoSqlRecord) record);
        } catch (Exception e) {
            handleDatabaseException(e);
            return null; // Never reached due to exception handling
        }
    }

    @Override
    public boolean exists(String shortKey) {
        try {
            return noSqlOperations.exists(shortKey);
        } catch (Exception e) {
            handleDatabaseException(e);
            return false; // Never reached due to exception handling
        }
    }

    private InfrastructureNoSqlRecord mapToNoSqlRecord(DomainEntityShortURL entity) {
        return InfrastructureNoSqlRecord.builder()
                .shortKey(entity.getShortKey())
                .originalUrl(entity.getOriginalUrl())
                .createTimestamp(entity.getCreateTimestamp())
                .usageCount(entity.getUsageCount())
                .validityChecks(entity.isValidityChecks())
                .optionalMetadata(entity.getOptionalMetadata())
                .build();
    }

    private DomainEntityShortURL mapFromNoSqlRecord(InfrastructureNoSqlRecord record) {
        return DomainEntityShortURL.builder()
                .shortKey(record.getShortKey())
                .originalUrl(record.getOriginalUrl())
                .createTimestamp(record.getCreateTimestamp())
                .usageCount(record.getUsageCount())
                .validityChecks(record.isValidityChecks())
                .optionalMetadata(record.getOptionalMetadata())
                .build();
    }

    private void handleDatabaseException(Exception e) {
        if (e instanceof InfrastructureExceptionDatabase) {
            throw ((InfrastructureExceptionDatabase) e).toDomainException();
        }
        throw new DomainExceptionInvalidKey("Database error: " + e.getMessage());
    }
}
