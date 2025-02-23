package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationExceptionShortURL;
import ai.shreds.application.ports.ApplicationInputPortShortURLService;
import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.exceptions.DomainExceptionCollision;
import ai.shreds.domain.exceptions.DomainExceptionInvalidKey;
import ai.shreds.domain.ports.DomainInputPortShortURL;
import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;
import ai.shreds.shared.exceptions.SharedExceptionNotFound;
import ai.shreds.shared.exceptions.SharedExceptionValidation;
import ai.shreds.shared.exceptions.SharedShortUrlException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ApplicationServiceShortURL implements ApplicationInputPortShortURLService {

    private static final int MAX_URL_LENGTH = 2048;
    private static final String[] BLOCKED_DOMAINS = {"example.com", "malicious.com"};
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$"
    );

    private final DomainInputPortShortURL domainService;
    private final MeterRegistry meterRegistry;

    public ApplicationServiceShortURL(DomainInputPortShortURL domainService, MeterRegistry meterRegistry) {
        this.domainService = domainService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public SharedShortURLResponseDTO createShortURL(SharedCreateShortURLRequestDTO request) {
        try {
            log.debug("Creating short URL for: {}", request.getOriginalUrl());
            meterRegistry.counter("shorturl.creation.attempts").increment();
            
            validateUrl(request.getOriginalUrl());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("createdAt", LocalDateTime.now());
            metadata.put("validityChecks", true);
            if (request.getExpirationTimeInSeconds() != null) {
                metadata.put("expiresAt", 
                    LocalDateTime.now().plusSeconds(request.getExpirationTimeInSeconds()));
            }

            DomainEntityShortURL domainEntity = domainService.generateShortKey(request.getOriginalUrl());
            domainEntity.setOptionalMetadata(metadata);
            
            SharedShortURLResponseDTO response = mapToResponse(domainEntity);
            log.info("Successfully created short URL with key: {}", response.getShortKey());
            meterRegistry.counter("shorturl.creation.success").increment();
            return response;

        } catch (Exception e) {
            meterRegistry.counter("shorturl.creation.errors").increment();
            return handleExceptions(e);
        }
    }

    @Override
    public SharedShortURLResponseDTO createCustomShortURL(SharedCreateShortURLRequestDTO request) {
        try {
            log.debug("Creating custom short URL for: {} with alias: {}", 
                request.getOriginalUrl(), request.getCustomAlias());
            meterRegistry.counter("shorturl.custom.creation.attempts").increment();

            if (request.getCustomAlias() == null || request.getCustomAlias().isBlank()) {
                throw new SharedExceptionValidation("Custom alias cannot be empty");
            }

            validateUrl(request.getOriginalUrl());
            DomainEntityShortURL domainEntity = domainService.generateShortKey(request.getOriginalUrl());
            domainEntity.setShortKey(request.getCustomAlias());
            
            SharedShortURLResponseDTO response = mapToResponse(domainEntity);
            response.setIsCustomAlias(true);
            
            log.info("Successfully created custom short URL with alias: {}", response.getShortKey());
            meterRegistry.counter("shorturl.custom.creation.success").increment();
            return response;

        } catch (Exception e) {
            meterRegistry.counter("shorturl.custom.creation.errors").increment();
            return handleExceptions(e);
        }
    }

    @Override
    @Cacheable(value = "urlCache", key = "#shortKey")
    public SharedShortURLResponseDTO getOriginalURL(String shortKey) {
        try {
            log.debug("Retrieving original URL for short key: {}", shortKey);
            meterRegistry.counter("shorturl.retrieval.attempts").increment();
            
            if(shortKey == null || shortKey.isBlank()) {
                throw new SharedExceptionValidation("Short key cannot be null or blank");
            }
            
            DomainEntityShortURL domainEntity = domainService.retrieveOriginalURL(shortKey);
            if(domainEntity == null) {
                throw new SharedExceptionNotFound("No URL found for provided short key");
            }
            
            if (!domainEntity.isValid()) {
                throw new SharedShortUrlException("This short URL has expired or is invalid", "INVALID_URL");
            }
            
            domainEntity.incrementUsageCount();
            
            SharedShortURLResponseDTO response = mapToResponse(domainEntity);
            log.info("Successfully retrieved original URL for key: {}", shortKey);
            meterRegistry.counter("shorturl.retrieval.success").increment();
            return response;

        } catch (Exception e) {
            meterRegistry.counter("shorturl.retrieval.errors").increment();
            return handleExceptions(e);
        }
    }

    @Override
    public SharedShortURLResponseDTO getUrlStatistics(String shortKey) {
        try {
            log.debug("Retrieving statistics for short key: {}", shortKey);
            meterRegistry.counter("shorturl.stats.attempts").increment();

            DomainEntityShortURL domainEntity = domainService.retrieveOriginalURL(shortKey);
            if (domainEntity == null) {
                throw new SharedExceptionNotFound("No URL found for provided short key");
            }

            SharedShortURLResponseDTO response = mapToResponse(domainEntity);
            meterRegistry.counter("shorturl.stats.success").increment();
            return response;

        } catch (Exception e) {
            meterRegistry.counter("shorturl.stats.errors").increment();
            return handleExceptions(e);
        }
    }

    @Override
    public boolean validateUrl(String url) {
        if(url == null || url.isBlank()) {
            throw new SharedExceptionValidation("URL cannot be null or blank");
        }
        
        if(url.length() > MAX_URL_LENGTH) {
            throw new SharedExceptionValidation(
                "URL exceeds maximum length of " + MAX_URL_LENGTH + " characters"
            );
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            throw new SharedExceptionValidation("Invalid URL format");
        }

        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost().toLowerCase();
            
            for (String blockedDomain : BLOCKED_DOMAINS) {
                if (host.contains(blockedDomain)) {
                    throw new SharedExceptionValidation("URL domain is not allowed");
                }
            }
            
            if (!parsedUrl.getProtocol().equals("http") && !parsedUrl.getProtocol().equals("https")) {
                throw new SharedExceptionValidation("URL must use HTTP or HTTPS protocol");
            }

            meterRegistry.counter("shorturl.validation.success").increment();
            return true;
            
        } catch (MalformedURLException e) {
            meterRegistry.counter("shorturl.validation.errors").increment();
            throw new SharedExceptionValidation("Invalid URL format: " + e.getMessage());
        }
    }

    @Override
    public boolean isUrlExpired(String shortKey) {
        try {
            DomainEntityShortURL domainEntity = domainService.retrieveOriginalURL(shortKey);
            if (domainEntity == null) {
                throw new SharedExceptionNotFound("No URL found for provided short key");
            }

            Map<String, Object> metadata = domainEntity.getOptionalMetadata();
            if (metadata != null && metadata.containsKey("expiresAt")) {
                LocalDateTime expiresAt = (LocalDateTime) metadata.get("expiresAt");
                return LocalDateTime.now().isAfter(expiresAt);
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking URL expiration", e);
            throw new SharedShortUrlException("Error checking URL expiration", "EXPIRATION_CHECK_ERROR");
        }
    }

    private SharedShortURLResponseDTO mapToResponse(DomainEntityShortURL domainEntity) {
        return SharedShortURLResponseDTO.builder()
                .shortKey(domainEntity.getShortKey())
                .originalUrl(domainEntity.getOriginalUrl())
                .shortUrl(buildShortUrl(domainEntity.getShortKey()))
                .createdAt(getCreatedAt(domainEntity))
                .expiresAt(getExpiresAt(domainEntity))
                .usageCount(domainEntity.getUsageCount())
                .isCustomAlias(isCustomAlias(domainEntity))
                .isActive(domainEntity.isValid())
                .build();
    }

    private String buildShortUrl(String shortKey) {
        return String.format("%s/%s", 
            System.getProperty("app.base.url", "http://localhost:8080"), shortKey);
    }

    private LocalDateTime getCreatedAt(DomainEntityShortURL entity) {
        return entity.getOptionalMetadata() != null && 
               entity.getOptionalMetadata().containsKey("createdAt") ?
               (LocalDateTime) entity.getOptionalMetadata().get("createdAt") :
               LocalDateTime.now();
    }

    private LocalDateTime getExpiresAt(DomainEntityShortURL entity) {
        return entity.getOptionalMetadata() != null && 
               entity.getOptionalMetadata().containsKey("expiresAt") ?
               (LocalDateTime) entity.getOptionalMetadata().get("expiresAt") :
               null;
    }

    private Boolean isCustomAlias(DomainEntityShortURL entity) {
        return entity.getOptionalMetadata() != null && 
               entity.getOptionalMetadata().containsKey("isCustomAlias") ?
               (Boolean) entity.getOptionalMetadata().get("isCustomAlias") :
               false;
    }

    private SharedShortURLResponseDTO handleExceptions(Exception e) {
        if (e instanceof DomainExceptionCollision) {
            log.error("Collision occurred while generating short URL", e);
            throw new SharedShortUrlException(
                "Unable to generate unique short URL. Please try again.", 
                "COLLISION_ERROR"
            );
        } else if (e instanceof DomainExceptionInvalidKey) {
            log.error("Invalid key generated", e);
            throw new SharedShortUrlException(
                "System unable to generate valid short URL", 
                "INVALID_KEY_ERROR"
            );
        } else if (e instanceof SharedExceptionValidation || 
                   e instanceof SharedExceptionNotFound || 
                   e instanceof SharedShortUrlException) {
            log.error("Known error occurred", e);
            throw (RuntimeException) e;
        } else {
            log.error("Internal error occurred", e);
            throw new SharedShortUrlException(
                String.format("An internal error occurred: %s", e.getMessage()),
                "INTERNAL_ERROR"
            );
        }
    }
}