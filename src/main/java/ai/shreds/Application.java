package ai.shreds;

import ai.shreds.shared.value_objects.SharedNoSqlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Main Application class that starts the URL Shortener service.
 */
@SpringBootApplication
@EnableConfigurationProperties(SharedNoSqlProperties.class)
@Import(RedisAutoConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
