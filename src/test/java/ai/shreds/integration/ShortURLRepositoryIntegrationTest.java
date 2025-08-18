package ai.shreds.integration;

import ai.shreds.Application;
import ai.shreds.domain.entities.DomainEntityShortURL;
import ai.shreds.domain.ports.DomainOutputPortShortURLRepository;
import ai.shreds.integration.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class, TestRedisConfiguration.class})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class ShortURLRepositoryIntegrationTest {

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired
    private DomainOutputPortShortURLRepository shortURLRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Test
    void When_ShortURL_Entity_Is_Saved_Then_It_Can_Be_Retrieved_By_Short_Key(CapturedOutput output) {
        // Print test logs
        System.out.println("\n=== Repository Integration Test Logs Start ===");
        System.out.println(output.toString());
        System.out.println("=== Repository Integration Test Logs End ===\n");

        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Verify repository is injected
        assertThat(shortURLRepository)
                .as("Repository should be injected")
                .isNotNull();

        System.out.println("Redis Connection Details:");
        System.out.println("Host: " + redisContainer.getHost());
        System.out.println("Port: " + redisContainer.getFirstMappedPort());

        // Create test data
        String shortKey = "test123";
        String originalUrl = "https://www.example.com/very/long/url/for/testing";
        Date createTimestamp = new Date();
        int usageCount = 0;
        boolean validityChecks = true;
        Map<String, Object> optionalMetadata = new HashMap<>();
        optionalMetadata.put("source", "integration-test");
        optionalMetadata.put("testId", "repo-test-001");

        DomainEntityShortURL testEntity = DomainEntityShortURL.builder()
                .shortKey(shortKey)
                .originalUrl(originalUrl)
                .createTimestamp(createTimestamp)
                .usageCount(usageCount)
                .validityChecks(validityChecks)
                .optionalMetadata(optionalMetadata)
                .build();

        System.out.println("\nTest Entity Created:");
        System.out.println("Short Key: " + testEntity.getShortKey());
        System.out.println("Original URL: " + testEntity.getOriginalUrl());
        System.out.println("Create Timestamp: " + testEntity.getCreateTimestamp());
        System.out.println("Usage Count: " + testEntity.getUsageCount());
        System.out.println("Validity Checks: " + testEntity.isValidityChecks());
        System.out.println("Optional Metadata: " + testEntity.getOptionalMetadata());

        // Verify key doesn't exist initially
        boolean initialExists = shortURLRepository.exists(shortKey);
        System.out.println("\nInitial key existence check: " + initialExists);
        assertThat(initialExists)
                .as("Key should not exist initially")
                .isFalse();

        // Save the entity
        System.out.println("\nSaving entity to repository...");
        DomainEntityShortURL savedEntity = shortURLRepository.saveShortURL(testEntity);

        // Verify save operation
        assertThat(savedEntity)
                .as("Saved entity should not be null")
                .isNotNull();

        assertThat(savedEntity.getShortKey())
                .as("Saved entity short key should match")
                .isEqualTo(shortKey);

        System.out.println("Entity saved successfully");

        // Verify key exists after save
        boolean existsAfterSave = shortURLRepository.exists(shortKey);
        System.out.println("Key existence after save: " + existsAfterSave);
        assertThat(existsAfterSave)
                .as("Key should exist after save")
                .isTrue();

        // Verify data is stored in Redis directly
        Object redisData = redisTemplate.opsForValue().get(shortKey);
        System.out.println("Direct Redis data check: " + redisData);
        assertThat(redisData)
                .as("Data should be stored in Redis")
                .isNotNull();

        // Retrieve the entity
        System.out.println("\nRetrieving entity from repository...");
        DomainEntityShortURL retrievedEntity = shortURLRepository.findByShortKey(shortKey);

        // Verify retrieval
        assertThat(retrievedEntity)
                .as("Retrieved entity should not be null")
                .isNotNull();

        System.out.println("\nRetrieved Entity:");
        System.out.println("Short Key: " + retrievedEntity.getShortKey());
        System.out.println("Original URL: " + retrievedEntity.getOriginalUrl());
        System.out.println("Create Timestamp: " + retrievedEntity.getCreateTimestamp());
        System.out.println("Usage Count: " + retrievedEntity.getUsageCount());
        System.out.println("Validity Checks: " + retrievedEntity.isValidityChecks());
        System.out.println("Optional Metadata: " + retrievedEntity.getOptionalMetadata());

        // Verify all fields are intact
        assertThat(retrievedEntity.getShortKey())
                .as("Retrieved short key should match original")
                .isEqualTo(shortKey);

        assertThat(retrievedEntity.getOriginalUrl())
                .as("Retrieved original URL should match original")
                .isEqualTo(originalUrl);

        assertThat(retrievedEntity.getCreateTimestamp())
                .as("Retrieved create timestamp should match original")
                .isEqualTo(createTimestamp);

        assertThat(retrievedEntity.getUsageCount())
                .as("Retrieved usage count should match original")
                .isEqualTo(usageCount);

        assertThat(retrievedEntity.isValidityChecks())
                .as("Retrieved validity checks should match original")
                .isEqualTo(validityChecks);

        assertThat(retrievedEntity.getOptionalMetadata())
                .as("Retrieved optional metadata should match original")
                .isEqualTo(optionalMetadata);

        // Verify entity is valid
        assertThat(retrievedEntity.isValid())
                .as("Retrieved entity should be valid")
                .isTrue();

        // Test usage count increment functionality
        System.out.println("\nTesting usage count increment...");
        int originalUsageCount = retrievedEntity.getUsageCount();
        retrievedEntity.incrementUsageCount();
        assertThat(retrievedEntity.getUsageCount())
                .as("Usage count should be incremented")
                .isEqualTo(originalUsageCount + 1);

        // Save the updated entity
        DomainEntityShortURL updatedEntity = shortURLRepository.saveShortURL(retrievedEntity);
        assertThat(updatedEntity.getUsageCount())
                .as("Updated entity usage count should be incremented")
                .isEqualTo(originalUsageCount + 1);

        // Retrieve again to verify the update was persisted
        DomainEntityShortURL reRetrievedEntity = shortURLRepository.findByShortKey(shortKey);
        assertThat(reRetrievedEntity.getUsageCount())
                .as("Re-retrieved entity usage count should be incremented")
                .isEqualTo(originalUsageCount + 1);

        // Verify no errors in logs
        String logs = output.toString();
        assertThat(logs)
                .as("Application logs should not contain FATAL errors")
                .doesNotContain("FATAL");

        System.out.println("\n=== Repository Integration Test completed successfully ===");

        // Clean up test data
        redisTemplate.delete(shortKey);
        System.out.println("Test data cleaned up from Redis");
    }

    @Test
    void When_Non_Existent_Short_Key_Is_Queried_Then_Repository_Returns_Null(CapturedOutput output) {
        // Print test logs
        System.out.println("\n=== Non-Existent Key Test Logs Start ===");
        System.out.println(output.toString());
        System.out.println("=== Non-Existent Key Test Logs End ===\n");

        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Verify repository is injected
        assertThat(shortURLRepository)
                .as("Repository should be injected")
                .isNotNull();

        String nonExistentKey = "nonexistent-key-12345";
        System.out.println("Testing retrieval of non-existent key: " + nonExistentKey);

        // Verify key doesn't exist
        boolean keyExists = shortURLRepository.exists(nonExistentKey);
        System.out.println("Key existence check: " + keyExists);
        assertThat(keyExists)
                .as("Non-existent key should not exist")
                .isFalse();

        // Attempt to retrieve non-existent key
        System.out.println("Attempting to retrieve non-existent key...");
        DomainEntityShortURL retrievedEntity = shortURLRepository.findByShortKey(nonExistentKey);

        // Verify null is returned
        System.out.println("Retrieved entity: " + retrievedEntity);
        assertThat(retrievedEntity)
                .as("Non-existent key should return null")
                .isNull();

        // Verify direct Redis check also returns null
        Object redisData = redisTemplate.opsForValue().get(nonExistentKey);
        System.out.println("Direct Redis data check: " + redisData);
        assertThat(redisData)
                .as("Direct Redis check should also return null")
                .isNull();

        // Verify no critical errors in logs (some INFO/DEBUG logs about missing keys are expected)
        String logs = output.toString();
        assertThat(logs)
                .as("Application logs should not contain FATAL errors")
                .doesNotContain("FATAL");

        System.out.println("\n=== Non-Existent Key Test completed successfully ===");
    }
}