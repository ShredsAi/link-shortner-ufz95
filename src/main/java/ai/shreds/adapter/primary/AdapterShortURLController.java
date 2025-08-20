package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationInputPortShortURLService;
import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class AdapterShortURLController {

    private final ApplicationInputPortShortURLService service;

    public AdapterShortURLController(ApplicationInputPortShortURLService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SharedShortURLResponseDTO> createShortURL(
            @RequestBody SharedCreateShortURLRequestDTO request) {
        SharedShortURLResponseDTO response = service.createShortURL(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<SharedShortURLResponseDTO> getOriginalURL(
            @PathVariable String shortKey) {
        SharedShortURLResponseDTO response = service.getOriginalURL(shortKey);
        return ResponseEntity.ok(response);
    }
}
