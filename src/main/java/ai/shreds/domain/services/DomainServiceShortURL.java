package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;
import ai.shreds.domain.ports.DomainInputPortShortURL;
import ai.shreds.domain.ports.DomainOutputPortShortURLRepository;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
public class DomainServiceShortURL implements DomainInputPortShortURL {

    private static final int MAX_COLLISION_RETRIES = 5;
    private static final int DEFAULT_KEY_LENGTH = 8;
    private static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$"
    );
    private static final String[] BLOCKED_PROTOCOLS = {"ftp", "telnet", "file"};

    private final DomainOutputPortShortURLRepository repository;
    private final SecureRandom random;
    private final MessageDigest digest;

    public DomainServiceShortURL(DomainOutputPortShortURLRepository repository) {
        this.repository = repository;
        this.random = new SecureRandom();
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize SHA-256 digest", e);
        }
    }

    @Override
    public DomainEntityShortURL generateShortKey(String originalUrl) {
        log.debug("Generating short key for URL: {}", originalUrl);
        validateUrl(originalUrl);
        
        int attempts = 0;
        DomainExceptionCollision lastException = null;

        while (attempts < MAX_COLLISION_RETRIES) {
            try {
                String shortKey = generateUniqueKey(originalUrl, attempts);
                if (repository.exists(shortKey)) {
                    attempts++;
                    continue;
                }

                DomainEntityShortURL entity = DomainEntityShortURL.builder()
                    .shortKey(shortKey)
                    .originalUrl(originalUrl)
                    .createTimestamp(LocalDateTime.now())
                    .usageCount(new AtomicInteger(0))
                    .validityChecks(true)
                    .optionalMetadata(createInitialMetadata())
                    .build();

                log.info("Successfully generated short key: {} for URL: {}", shortKey, originalUrl);
                return repository.saveShortURL(entity);
            } catch (DomainExceptionCollision e) {
                lastException = e;
                attempts++;
                log.warn("Collision occurred on attempt {}: {}", attempts, e.getMessage());
            }
        }

        throw new DomainExceptionCollision("Failed to generate unique key after " + 
            MAX_COLLISION_RETRIES + " attempts", lastException);
    }

    @Override
    public DomainEntityShortURL createCustomShortURL(String originalUrl, String customAlias) {
        log.debug("Creating custom short URL with alias: {} for URL: {}", customAlias, originalUrl);
        validateUrl(originalUrl);
        validateCustomAlias(customAlias);

        if (repository.exists(customAlias)) {
            throw new DomainExceptionCollision("Custom alias already exists: " + customAlias);
        }

        DomainEntityShortURL entity = DomainEntityShortURL.builder()
            .shortKey(customAlias)
            .originalUrl(originalUrl)
            .createTimestamp(LocalDateTime.now())
            .usageCount(new AtomicInteger(0))
            .validityChecks(true)
            .isCustomAlias(true)
            .optionalMetadata(createInitialMetadata())
            .build();

        log.info("Successfully created custom short URL with alias: {}", customAlias);
        return repository.saveShortURL(entity);
    }

    @Override
    public DomainEntityShortURL retrieveOriginalURL(String shortKey) {
        log.debug("Retrieving original URL for short key: {}", shortKey);
        validateShortKey(shortKey);

        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }

        if (!entity.isValid()) {
            throw new DomainExceptionInvalidKey("URL is no longer valid or has expired");
        }

        entity.incrementUsageCount("UNKNOWN", "UNKNOWN", "SYSTEM");
        log.info("Successfully retrieved original URL for key: {}", shortKey);
        return repository.saveShortURL(entity);
    }

    @Override
    public boolean validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new DomainExceptionInvalidKey("URL cannot be null or empty");
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            throw new DomainExceptionInvalidKey("Invalid URL format");
        }

        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol().toLowerCase();

            for (String blockedProtocol : BLOCKED_PROTOCOLS) {
                if (protocol.equals(blockedProtocol)) {
                    throw new DomainExceptionInvalidKey(
                        "Protocol '" + protocol + "' is not allowed"
                    );
                }
            }

            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new DomainExceptionInvalidKey("URL must use HTTP or HTTPS protocol");
            }

            return true;
        } catch (MalformedURLException e) {
            throw new DomainExceptionInvalidKey("Invalid URL format: " + e.getMessage());
        }
    }

    @Override
    public boolean isExpired(String shortKey) {
        log.debug("Checking expiration for short key: {}", shortKey);
        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }

        LocalDateTime expirationTime = entity.getExpirationTime();
        return expirationTime != null && LocalDateTime.now().isAfter(expirationTime);
    }

    @Override
    public DomainEntityShortURL getStatistics(String shortKey) {
        log.debug("Retrieving statistics for short key: {}", shortKey);
        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }
        return entity;
    }

    @Override
    public boolean invalidateUrl(String shortKey) {
        log.debug("Invalidating URL for short key: {}", shortKey);
        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }

        entity.setValidityChecks(false);
        repository.saveShortURL(entity);
        log.info("Successfully invalidated URL for key: {}", shortKey);
        return true;
    }

    @Override
    public DomainEntityShortURL updateExpiration(String shortKey, Long expirationTimeInSeconds) {
        log.debug("Updating expiration for short key: {}", shortKey);
        DomainEntityShortURL entity = repository.findByShortKey(shortKey);
        if (entity == null) {
            throw new DomainExceptionInvalidKey("Short key not found: " + shortKey);
        }

        LocalDateTime newExpirationTime = LocalDateTime.now().plusSeconds(expirationTimeInSeconds);
        DomainEntityShortURL updatedEntity = DomainEntityShortURL.builder()
            .shortKey(entity.getShortKey())
            .originalUrl(entity.getOriginalUrl())
            .createTimestamp(entity.getCreateTimestamp())
            .usageCount(entity.getUsageCount())
            .validityChecks(entity.isValidityChecks())
            .optionalMetadata(entity.getOptionalMetadata())
            .expirationTime(newExpirationTime)
            .build();

        log.info("Successfully updated expiration for key: {}", shortKey);
        return repository.saveShortURL(updatedEntity);
    }

    private String generateUniqueKey(String originalUrl, int attempt) {
        byte[] urlBytes = (originalUrl + System.nanoTime() + attempt).getBytes();
        byte[] hashBytes = digest.digest(urlBytes);
        String base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        return base64Hash.substring(0, DEFAULT_KEY_LENGTH);
    }

    private void validateShortKey(String shortKey) {
        if (shortKey == null || shortKey.trim().isEmpty()) {
            throw new DomainExceptionInvalidKey("Short key cannot be null or empty");
        }
        if (shortKey.length() > DEFAULT_KEY_LENGTH) {
            throw new DomainExceptionInvalidKey(
                "Short key length exceeds maximum of " + DEFAULT_KEY_LENGTH
            );
        }
    }

    private void validateCustomAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new DomainExceptionInvalidKey("Custom alias cannot be null or empty");
        }
        if (!alias.matches("^[a-zA-Z0-9-_]{1," + DEFAULT_KEY_LENGTH + "}$")) {
            throw new DomainExceptionInvalidKey(
                "Custom alias must be alphanumeric and can include hyphens and underscores"
            );
        }
    }

    private HashMap<String, Object> createInitialMetadata() {
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("createdBy", "SYSTEM");
        metadata.put("creationTimestamp", LocalDateTime.now());
        metadata.put("lastModified", LocalDateTime.now());
        metadata.put("version", "1.0");
        return metadata;
    }
}