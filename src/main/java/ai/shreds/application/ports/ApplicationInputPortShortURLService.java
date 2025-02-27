package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;

public interface ApplicationInputPortShortURLService {
    SharedShortURLResponseDTO createShortURL(SharedCreateShortURLRequestDTO request);
    SharedShortURLResponseDTO getOriginalURL(String shortKey);
}
