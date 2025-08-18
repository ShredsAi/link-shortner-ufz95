package ai.shreds.integration;

import ai.shreds.Application;
import ai.shreds.integration.config.TestRedisConfiguration;
import ai.shreds.shared.dtos.SharedCreateShortURLRequestDTO;
import ai.shreds.shared.dtos.SharedShortURLResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class, TestRedisConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class ShortURLControllerIntegrationTest {

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Test
    void When_Valid_URL_Is_Submitted_Then_Short_URL_Is_Created_And_Stored_In_Database(CapturedOutput output) {
        // Print test logs
        System.out.println("\n=== ShortURL Controller Integration Test Logs Start ===");
        System.out.println(output.toString());
        System.out.println("=== ShortURL Controller Integration Test Logs End ===\n");

        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Prepare test data
        String originalUrl = "https://www.example.com/very/long/url/that/needs/to/be/shortened";
        SharedCreateShortURLRequestDTO request = new SharedCreateShortURLRequestDTO();
        request.setOriginalUrl(originalUrl);

        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCreateShortURLRequestDTO> entity = new HttpEntity<>(request, headers);

        // Test URL creation endpoint
        String createUrl = "http://localhost:" + port + "/api/v1/urls";
        System.out.println("Testing POST to: " + createUrl);
        System.out.println("Request payload: " + request.getOriginalUrl());

        ResponseEntity<SharedShortURLResponseDTO> createResponse = restTemplate.exchange(
                createUrl,
                HttpMethod.POST,
                entity,
                SharedShortURLResponseDTO.class
        );

        // Verify creation response
        System.out.println("Create response status: " + createResponse.getStatusCode());
        System.out.println("Create response body: " + createResponse.getBody());

        assertThat(createResponse.getStatusCode())
                .as("Should return 200 OK for valid URL creation")
                .isEqualTo(HttpStatus.OK);

        SharedShortURLResponseDTO createResponseBody = createResponse.getBody();
        assertThat(createResponseBody)
                .as("Response body should not be null")
                .isNotNull();

        assertThat(createResponseBody.getShortKey())
                .as("Short key should be generated")
                .isNotNull()
                .isNotEmpty();

        assertThat(createResponseBody.getOriginalUrl())
                .as("Original URL should match the request")
                .isEqualTo(originalUrl);

        String shortKey = createResponseBody.getShortKey();
        System.out.println("Generated short key: " + shortKey);

        // Verify data is stored in Redis
        System.out.println("\nVerifying Redis storage...");
        System.out.println("Redis Host: " + redisContainer.getHost());
        System.out.println("Redis Port: " + redisContainer.getFirstMappedPort());

        // Check if the short key exists in Redis
        Boolean keyExists = redisTemplate.hasKey(shortKey);
        System.out.println("Key exists in Redis: " + keyExists);

        assertThat(keyExists)
                .as("Short key should be stored in Redis")
                .isTrue();

        // Retrieve the stored data from Redis to verify it's correct
        Object storedData = redisTemplate.opsForValue().get(shortKey);
        System.out.println("Stored data in Redis: " + storedData);

        assertThat(storedData)
                .as("Stored data should not be null")
                .isNotNull();

        // Test retrieval endpoint
        String retrieveUrl = "http://localhost:" + port + "/api/v1/urls/" + shortKey;
        System.out.println("\nTesting GET from: " + retrieveUrl);

        ResponseEntity<SharedShortURLResponseDTO> retrieveResponse = restTemplate.getForEntity(
                retrieveUrl,
                SharedShortURLResponseDTO.class
        );

        // Verify retrieval response
        System.out.println("Retrieve response status: " + retrieveResponse.getStatusCode());
        System.out.println("Retrieve response body: " + retrieveResponse.getBody());

        assertThat(retrieveResponse.getStatusCode())
                .as("Should return 200 OK for valid short key retrieval")
                .isEqualTo(HttpStatus.OK);

        SharedShortURLResponseDTO retrieveResponseBody = retrieveResponse.getBody();
        assertThat(retrieveResponseBody)
                .as("Retrieve response body should not be null")
                .isNotNull();

        assertThat(retrieveResponseBody.getShortKey())
                .as("Retrieved short key should match")
                .isEqualTo(shortKey);

        assertThat(retrieveResponseBody.getOriginalUrl())
                .as("Retrieved original URL should match")
                .isEqualTo(originalUrl);

        // Verify no errors in logs
        String logs = output.toString();
        assertThat(logs)
                .as("Application logs should not contain errors")
                .doesNotContain("ERROR")
                .doesNotContain("FATAL");

        System.out.println("\n=== Test completed successfully ===");

        // Clean up test data
        redisTemplate.delete(shortKey);
        System.out.println("Test data cleaned up from Redis");
    }
}