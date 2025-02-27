package ai.shreds.shared.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.URL;

@Data
public class SharedCreateShortURLRequestDTO {
    @NotEmpty(message = "Original URL cannot be empty")
    @URL(message = "Invalid URL format")
    private String originalUrl;
}
