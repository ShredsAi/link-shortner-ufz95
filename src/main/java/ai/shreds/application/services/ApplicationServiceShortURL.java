package ai.shreds.application.services;

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
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceShortURL implements ApplicationInputPortShortURLService {

    private final DomainInputPortShortURL domainService;

    public ApplicationServiceShortURL(DomainInputPortShortURL domainService) {
        this.domainService = domainService;
    }

    @Override
    public SharedShortURLResponseDTO createShortURL(SharedCreateShortURLRequestDTO request) {
        try {
            validateUrl(request.getOriginalUrl());
            DomainEntityShortURL entity = domainService.generateShortKey(request.getOriginalUrl());
            return mapToResponse(entity);
        } catch (DomainExceptionInvalidKey e) {
            throw new SharedExceptionValidation(e.getMessage());
        } catch (DomainExceptionCollision e) {
            throw new SharedShortUrlException(e.getMessage(), "COLLISION");
        } catch (SharedExceptionValidation | SharedExceptionNotFound | SharedShortUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new SharedShortUrlException("An unexpected error occurred: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    @Override
    public SharedShortURLResponseDTO getOriginalURL(String shortKey) {
        try {
            DomainEntityShortURL entity = domainService.retrieveOriginalURL(shortKey);
            if (entity == null) {
                throw new SharedExceptionNotFound("Short URL not found: " + shortKey);
            }
            return mapToResponse(entity);
        } catch (DomainExceptionInvalidKey e) {
            throw new SharedExceptionValidation(e.getMessage());
        } catch (SharedExceptionValidation | SharedExceptionNotFound | SharedShortUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new SharedShortUrlException("An unexpected error occurred: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new SharedExceptionValidation("URL cannot be empty");
        }
    }

    private SharedShortURLResponseDTO mapToResponse(DomainEntityShortURL entity) {
        return SharedShortURLResponseDTO.builder()
                .shortKey(entity.getShortKey())
                .originalUrl(entity.getOriginalUrl())
                .build();
    }
}