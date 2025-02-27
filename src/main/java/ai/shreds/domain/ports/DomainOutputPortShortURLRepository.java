package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityShortURL;

public interface DomainOutputPortShortURLRepository {
    DomainEntityShortURL saveShortURL(DomainEntityShortURL entity);
    DomainEntityShortURL findByShortKey(String shortKey);
    boolean exists(String shortKey);
}
