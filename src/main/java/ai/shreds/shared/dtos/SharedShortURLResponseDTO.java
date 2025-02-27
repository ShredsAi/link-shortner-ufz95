package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SharedShortURLResponseDTO {
    private String shortKey;
    private String originalUrl;
}
