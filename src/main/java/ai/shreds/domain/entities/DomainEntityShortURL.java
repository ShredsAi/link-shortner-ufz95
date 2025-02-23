package ai.shreds.domain.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Domain entity representing a shortened URL with its metadata and validation rules.
 */
@Slf4j
@Getter
@Builder
public class DomainEntityShortURL {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$"
    );
    private static final int MAX_URL_LENGTH = 2048;
    private static final String[] BLOCKED_PROTOCOLS = {"ftp", "telnet", "file"};

    private final String shortKey;
    private final LocalDateTime createTimestamp;
    private final AtomicInteger usageCount;
    private final String originalUrl;
    private volatile boolean validityChecks;
    private final Map<String, Object> optionalMetadata;
    private final LocalDateTime expirationTime;
    private final boolean isCustomAlias;
    private final String createdBy;
    private final String lastAccessedBy;
    private volatile LocalDateTime lastAccessTime;

    @Builder.Default
    private final Map<String, Integer> accessByCountry = new HashMap<>();

    @Builder.Default
    private final Map<String, Integer> accessByDevice = new HashMap<>();

    public DomainEntityShortURL(String shortKey,
                               LocalDateTime createTimestamp,
                               AtomicInteger usageCount,
                               String originalUrl,
                               boolean validityChecks,
                               Map<String, Object> optionalMetadata,
                               LocalDateTime expirationTime,
                               boolean isCustomAlias,
                               String createdBy,
                               String lastAccessedBy,
                               LocalDateTime lastAccessTime,
                               Map<String, Integer> accessByCountry,
                               Map<String, Integer> accessByDevice) {
        validateInputs(shortKey, createTimestamp, originalUrl);
        
        this.shortKey = shortKey;
        this.createTimestamp = createTimestamp;
        this.usageCount = usageCount != null ? usageCount : new AtomicInteger(0);
        this.originalUrl = originalUrl;
        this.validityChecks = validityChecks;
        this.optionalMetadata = optionalMetadata != null ? 
            new HashMap<>(optionalMetadata) : new HashMap<>();
        this.expirationTime = expirationTime;
        this.isCustomAlias = isCustomAlias;
        this.createdBy = createdBy;
        this.lastAccessedBy = lastAccessedBy;
        this.lastAccessTime = lastAccessTime;
        this.accessByCountry.putAll(accessByCountry != null ? 
            accessByCountry : Collections.emptyMap());
        this.accessByDevice.putAll(accessByDevice != null ? 
            accessByDevice : Collections.emptyMap());
    }

    private void validateInputs(String shortKey, LocalDateTime createTimestamp, String originalUrl) {
        if (shortKey == null || shortKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Short key cannot be null or empty");
        }
        if (createTimestamp == null) {
            throw new IllegalArgumentException("Create timestamp cannot be null");
        }
        if (createTimestamp.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Create timestamp cannot be in the future");
        }
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original URL cannot be null or empty");
        }
        validateUrlFormat(originalUrl);
        validateUrlSecurity(originalUrl);
    }

    private void validateUrlFormat(String url) {
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException(
                "URL exceeds maximum length of " + MAX_URL_LENGTH + " characters"
            );
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }

    private void validateUrlSecurity(String url) {
        String lowercaseUrl = url.toLowerCase();
        for (String protocol : BLOCKED_PROTOCOLS) {
            if (lowercaseUrl.startsWith(protocol + "://")) {
                throw new IllegalArgumentException(
                    "Protocol '" + protocol + "' is not allowed"
                );
            }
        }

        if (!lowercaseUrl.startsWith("http://") && !lowercaseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
        }
    }

    public synchronized void incrementUsageCount(String country, String device, String accessedBy) {
        usageCount.incrementAndGet();
        lastAccessTime = LocalDateTime.now();

        accessByCountry.merge(country, 1, Integer::sum);
        accessByDevice.merge(device, 1, Integer::sum);
        optionalMetadata.put("lastAccessedBy", accessedBy);
        optionalMetadata.put("lastAccessTime", lastAccessTime);

        log.debug("Incremented usage count for key: {}, new count: {}", 
            shortKey, usageCount.get());
    }

    public boolean isValid() {
        if (!validityChecks) {
            log.debug("URL invalid due to failed validity checks: {}", shortKey);
            return false;
        }

        if (expirationTime != null && LocalDateTime.now().isAfter(expirationTime)) {
            log.debug("URL expired: {}", shortKey);
            return false;
        }

        try {
            validateUrlFormat(originalUrl);
            validateUrlSecurity(originalUrl);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("URL validation failed: {}, reason: {}", shortKey, e.getMessage());
            return false;
        }
    }

    public synchronized void setValidityChecks(boolean validityChecks) {
        this.validityChecks = validityChecks;
        optionalMetadata.put("lastValidityCheck", LocalDateTime.now());
        log.debug("Updated validity checks for key: {} to: {}", shortKey, validityChecks);
    }

    public synchronized void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
        optionalMetadata.put("lastAccessTime", lastAccessTime);
        log.debug("Updated last access time for key: {} to: {}", shortKey, lastAccessTime);
    }

    public synchronized void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            optionalMetadata.put(key, value);
            log.debug("Added metadata for key: {}, metadata key: {}", shortKey, key);
        }
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(optionalMetadata);
    }

    public Map<String, Integer> getAccessByCountry() {
        return Collections.unmodifiableMap(accessByCountry);
    }

    public Map<String, Integer> getAccessByDevice() {
        return Collections.unmodifiableMap(accessByDevice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainEntityShortURL)) return false;
        DomainEntityShortURL that = (DomainEntityShortURL) o;
        return Objects.equals(shortKey, that.shortKey) &&
               Objects.equals(originalUrl, that.originalUrl) &&
               Objects.equals(createTimestamp, that.createTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortKey, originalUrl, createTimestamp);
    }

    @Override
    public String toString() {
        return String.format("DomainEntityShortURL[shortKey=%s, originalUrl=%s, usageCount=%d, valid=%b]",
            shortKey, originalUrl, usageCount.get(), validityChecks);
    }
}
