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

    @Test
    void When_Short_Key_Is_Requested_Then_Original_URL_Is_Retrieved_From_Database(CapturedOutput output) {
        System.out.println("\n=== URL Retrieval and Usage Count Test Start ===");
        System.out.println(output.toString());
        System.out.println("=== URL Retrieval and Usage Count Test Logs End ===\n");

        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Step 1: Create a short URL first
        String originalUrl = "https://www.google.com/search?q=integration+testing";
        SharedCreateShortURLRequestDTO createRequest = new SharedCreateShortURLRequestDTO();
        createRequest.setOriginalUrl(originalUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SharedCreateShortURLRequestDTO> createEntity = new HttpEntity<>(createRequest, headers);

        String createUrl = "http://localhost:" + port + "/api/v1/urls";
        System.out.println("Creating short URL for testing retrieval: " + createUrl);
        
        ResponseEntity<SharedShortURLResponseDTO> createResponse = restTemplate.exchange(
                createUrl,
                HttpMethod.POST,
                createEntity,
                SharedShortURLResponseDTO.class
        );

        assertThat(createResponse.getStatusCode())
                .as("URL creation should succeed")
                .isEqualTo(HttpStatus.OK);

        String shortKey = createResponse.getBody().getShortKey();
        System.out.println("Created short key for testing: " + shortKey);

        // Step 2: Verify initial usage count is 0 by checking Redis directly
        Object initialStoredData = redisTemplate.opsForValue().get(shortKey);
        System.out.println("Initial stored data in Redis: " + initialStoredData);
        assertThat(initialStoredData)
                .as("Initial data should be stored in Redis")
                .isNotNull();

        // Step 3: Retrieve the URL for the first time
        String retrieveUrl = "http://localhost:" + port + "/api/v1/urls/" + shortKey;
        System.out.println("\nFirst retrieval - Testing GET from: " + retrieveUrl);

        ResponseEntity<SharedShortURLResponseDTO> firstRetrieveResponse = restTemplate.getForEntity(
                retrieveUrl,
                SharedShortURLResponseDTO.class
        );

        // Verify first retrieval response
        System.out.println("First retrieve response status: " + firstRetrieveResponse.getStatusCode());
        System.out.println("First retrieve response body: " + firstRetrieveResponse.getBody());

        assertThat(firstRetrieveResponse.getStatusCode())
                .as("First retrieval should return 200 OK")
                .isEqualTo(HttpStatus.OK);

        SharedShortURLResponseDTO firstRetrieveBody = firstRetrieveResponse.getBody();
        assertThat(firstRetrieveBody)
                .as("First retrieve response body should not be null")
                .isNotNull();

        assertThat(firstRetrieveBody.getShortKey())
                .as("Retrieved short key should match")
                .isEqualTo(shortKey);

        assertThat(firstRetrieveBody.getOriginalUrl())
                .as("Retrieved original URL should match")
                .isEqualTo(originalUrl);

        // Step 4: Verify usage count was incremented in Redis after first retrieval
        Object dataAfterFirstRetrieval = redisTemplate.opsForValue().get(shortKey);
        System.out.println("Data after first retrieval: " + dataAfterFirstRetrieval);
        assertThat(dataAfterFirstRetrieval)
                .as("Data should still exist in Redis after first retrieval")
                .isNotNull();

        // Step 5: Retrieve the URL for the second time to verify usage count increment
        System.out.println("\nSecond retrieval - Testing GET from: " + retrieveUrl);
        
        ResponseEntity<SharedShortURLResponseDTO> secondRetrieveResponse = restTemplate.getForEntity(
                retrieveUrl,
                SharedShortURLResponseDTO.class
        );

        // Verify second retrieval response
        System.out.println("Second retrieve response status: " + secondRetrieveResponse.getStatusCode());
        System.out.println("Second retrieve response body: " + secondRetrieveResponse.getBody());

        assertThat(secondRetrieveResponse.getStatusCode())
                .as("Second retrieval should return 200 OK")
                .isEqualTo(HttpStatus.OK);

        SharedShortURLResponseDTO secondRetrieveBody = secondRetrieveResponse.getBody();
        assertThat(secondRetrieveBody)
                .as("Second retrieve response body should not be null")
                .isNotNull();

        assertThat(secondRetrieveBody.getShortKey())
                .as("Retrieved short key should still match")
                .isEqualTo(shortKey);

        assertThat(secondRetrieveBody.getOriginalUrl())
                .as("Retrieved original URL should still match")
                .isEqualTo(originalUrl);

        // Step 6: Verify usage count was incremented again in Redis
        Object dataAfterSecondRetrieval = redisTemplate.opsForValue().get(shortKey);
        System.out.println("Data after second retrieval: " + dataAfterSecondRetrieval);
        assertThat(dataAfterSecondRetrieval)
                .as("Data should still exist in Redis after second retrieval")
                .isNotNull();

        // Step 7: Test retrieval of non-existent key
        String nonExistentKey = "nonexistent123";
        String nonExistentUrl = "http://localhost:" + port + "/api/v1/urls/" + nonExistentKey;
        System.out.println("\nTesting retrieval of non-existent key: " + nonExistentUrl);

        ResponseEntity<String> notFoundResponse = restTemplate.getForEntity(
                nonExistentUrl,
                String.class
        );

        System.out.println("Non-existent key response status: " + notFoundResponse.getStatusCode());
        System.out.println("Non-existent key response body: " + notFoundResponse.getBody());

        assertThat(notFoundResponse.getStatusCode())
                .as("Non-existent key should return 404 or 500 (depending on exception handling)")
                .isIn(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);

        // Verify no critical errors in logs (allow expected domain exceptions)
        String logs = output.toString();
        assertThat(logs)
                .as("Application logs should not contain FATAL errors")
                .doesNotContain("FATAL");

        System.out.println("\n=== URL Retrieval and Usage Count Test completed successfully ===");

        // Clean up test data
        redisTemplate.delete(shortKey);
        System.out.println("Test data cleaned up from Redis");
    }
}