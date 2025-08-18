package ai.shreds.integration;

import ai.shreds.Application;
import ai.shreds.integration.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class, TestRedisConfiguration.class})
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class ApplicationStartupIntegrationTest {

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Test
    void shouldStartApplicationSuccessfully(CapturedOutput output) {
        // Print startup logs
        System.out.println("\n=== Application Startup Test Logs Start ===\n");
        System.out.println(output.toString());
        System.out.println("\n=== Application Startup Test Logs End ===\n");

        // Verify Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Verify Spring context is loaded
        assertThat(applicationContext)
                .as("Application context should be loaded")
                .isNotNull();

        // Verify Redis template is configured
        assertThat(redisTemplate)
                .as("Redis template should be configured")
                .isNotNull();

        // Test Redis connectivity
        String testKey = "test:startup";
        String testValue = "application-started";
        
        try {
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = redisTemplate.opsForValue().get(testKey);

            assertThat(retrievedValue)
                    .as("Should be able to write and read from Redis")
                    .isEqualTo(testValue);
        } finally {
            redisTemplate.delete(testKey);
        }

        // Verify application startup logs
        String logs = output.toString();
        assertThat(logs)
                .as("Application startup logs should indicate successful initialization")
                .contains("Started ApplicationStartupIntegrationTest")
                .contains("url-shortener-test")
                .doesNotContain("ERROR")
                .doesNotContain("FATAL");

        System.out.println("\nRedis Connection Details:");
        System.out.println("Host: " + redisContainer.getHost());
        System.out.println("Port: " + redisContainer.getFirstMappedPort());
    }
}