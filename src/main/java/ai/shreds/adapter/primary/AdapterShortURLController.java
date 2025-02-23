package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationInputPortShortURLService;
import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/shorturls")
@Tag(name = "Short URL API", description = "Operations for URL shortening service")
@Validated
@SecurityRequirement(name = "bearer-key")
public class AdapterShortURLController {

    private final ApplicationInputPortShortURLService shortURLService;
    private final MeterRegistry meterRegistry;
    private final Timer createUrlTimer;
    private final Timer retrieveUrlTimer;

    public AdapterShortURLController(ApplicationInputPortShortURLService shortURLService,
                                    MeterRegistry meterRegistry) {
        this.shortURLService = shortURLService;
        this.meterRegistry = meterRegistry;
        this.createUrlTimer = Timer.builder("shorturl.operation.timer")
                .tag("operation", "create")
                .description("Timer for URL creation operation")
                .register(meterRegistry);
        this.retrieveUrlTimer = Timer.builder("shorturl.operation.timer")
                .tag("operation", "retrieve")
                .description("Timer for URL retrieval operation")
                .register(meterRegistry);
    }

    @PostMapping
    @Operation(summary = "Create a short URL", description = "Creates a shortened version of the provided URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Short URL created successfully",
                    content = @Content(schema = @Schema(implementation = SharedShortURLResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid URL format"),
        @ApiResponse(responseCode = "409", description = "URL collision detected"),
        @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @Timed(value = "shorturl.creation", description = "Time taken to create short URL")
    @PreAuthorize("hasRole('URL_CREATOR')")
    public ResponseEntity<SharedShortURLResponseDTO> createShortURL(
            @Valid @RequestBody SharedCreateShortURLRequestDTO request) {
        String requestId = generateRequestId();
        log.info("[{}] Creating short URL for: {}", requestId, request.getOriginalUrl());
        
        return createUrlTimer.record(() -> {
            meterRegistry.counter("shorturl.creation.requests", 
                "url_domain", extractDomain(request.getOriginalUrl())).increment();

            SharedShortURLResponseDTO response = shortURLService.createShortURL(request);
            
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{shortKey}")
                    .buildAndExpand(response.getShortKey())
                    .toUri();

            log.info("[{}] Created short URL: {} -> {}", requestId, response.getShortKey(), request.getOriginalUrl());
            return ResponseEntity
                    .created(location)
                    .body(response);
        });
    }

    @GetMapping("/{shortKey}")
    @Operation(summary = "Get original URL", description = "Retrieves the original URL for a given short key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Original URL found",
                    content = @Content(schema = @Schema(implementation = SharedShortURLResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Short URL not found"),
        @ApiResponse(responseCode = "410", description = "Short URL has expired"),
        @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @Timed(value = "shorturl.retrieval", description = "Time taken to retrieve original URL")
    public ResponseEntity<SharedShortURLResponseDTO> getOriginalURL(
            @Parameter(description = "Short URL key", required = true)
            @PathVariable String shortKey) {
        String requestId = generateRequestId();
        log.debug("[{}] Retrieving original URL for key: {}", requestId, shortKey);

        return retrieveUrlTimer.record(() -> {
            meterRegistry.counter("shorturl.retrieval.requests", "key", shortKey).increment();

            SharedShortURLResponseDTO response = shortURLService.getOriginalURL(shortKey);
            
            return ResponseEntity
                    .ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)
                            .mustRevalidate()
                            .proxyRevalidate())
                    .header(HttpHeaders.VARY, "Accept-Encoding")
                    .body(response);
        });
    }

    @GetMapping("/{shortKey}/redirect")
    @Operation(summary = "Redirect to original URL", description = "Redirects directly to the original URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "404", description = "Short URL not found"),
        @ApiResponse(responseCode = "410", description = "Short URL has expired")
    })
    @Timed(value = "shorturl.redirect", description = "Time taken to redirect to original URL")
    public void redirectToOriginalUrl(
            @Parameter(description = "Short URL key", required = true)
            @PathVariable String shortKey,
            HttpServletResponse response) throws IOException {
        String requestId = generateRequestId();
        log.debug("[{}] Redirecting for key: {}", requestId, shortKey);

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            meterRegistry.counter("shorturl.redirect.requests", "key", shortKey).increment();

            SharedShortURLResponseDTO urlInfo = shortURLService.getOriginalURL(shortKey);
            response.setHeader("X-Short-URL-Key", shortKey);
            response.setHeader("X-Request-ID", requestId);
            response.sendRedirect(urlInfo.getOriginalUrl());
        } finally {
            sample.stop(meterRegistry.timer("shorturl.redirect.timer", "key", shortKey));
        }
    }

    @GetMapping("/{shortKey}/stats")
    @Operation(summary = "Get URL statistics", description = "Retrieves usage statistics for a short URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    @Timed(value = "shorturl.stats", description = "Time taken to retrieve URL statistics")
    @PreAuthorize("hasRole('URL_ADMIN')")
    public ResponseEntity<SharedShortURLResponseDTO> getUrlStats(
            @Parameter(description = "Short URL key", required = true)
            @PathVariable String shortKey) {
        String requestId = generateRequestId();
        log.debug("[{}] Retrieving stats for key: {}", requestId, shortKey);

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            meterRegistry.counter("shorturl.stats.requests", "key", shortKey).increment();

            SharedShortURLResponseDTO response = shortURLService.getOriginalURL(shortKey);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                    .body(response);
        } finally {
            sample.stop(meterRegistry.timer("shorturl.stats.timer", "key", shortKey));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Checks the health of the URL shortening service")
    public ResponseEntity<String> healthCheck() {
        String requestId = generateRequestId();
        log.debug("[{}] Health check requested", requestId);
        meterRegistry.counter("shorturl.health.checks").increment();
        return ResponseEntity.ok("Service is healthy");
    }

    private String generateRequestId() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getHeader("X-Request-ID");
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null ? domain.startsWith("www.") ? domain.substring(4) : domain : "unknown";
        } catch (Exception e) {
            return "invalid";
        }
    }
}
