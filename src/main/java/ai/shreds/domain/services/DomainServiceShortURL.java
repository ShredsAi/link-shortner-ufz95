package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;
import ai.shreds.domain.ports.DomainInputPortShortURL;
import ai.shreds.domain.ports.DomainOutputPortShortURLRepository;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
public class DomainServiceShortURL implements DomainInputPortShortURL {

    private final DomainOutputPortShortURLRepository repository;

    public DomainServiceShortURL(DomainOutputPortShortURLRepository repository) {
        this.repository = repository;
    }

    @Override
    public DomainEntityShortURL generateShortKey(String originalUrl) {
        validateUrl(originalUrl);
        
        String shortKey = generateUniqueKey();
        while (repository.exists(shortKey)) {
            shortKey = generateUniqueKey();
        }

        DomainEntityShortURL entity = DomainEntityShortURL.builder()
                .shortKey(shortKey)
                .originalUrl(originalUrl)
                .createTimestamp(new Date())
                .usageCount(0)
                .validityChecks(true)
                .build();

        return repository.saveShortURL(entity);
    }

    @Override
    public DomainEntityShortURL retrieveOriginalURL(String shortKey) {
        if (shortKey == null || shortKey.trim().isEmpty()) {
            throw new DomainExceptionInvalidKey("Short key cannot be null or empty");
        }

        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }

        entity.incrementUsageCount();
        return repository.saveShortURL(entity);
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new DomainExceptionInvalidKey("URL cannot be null or empty");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new DomainExceptionInvalidKey("Invalid URL format: " + url);
        }
    }

    private String generateUniqueKey() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
