package ai.shreds.shared.value_objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * Configuration properties for Redis connection.
 * Maps to application.yml redis configuration section.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "spring.redis")
public class SharedNoSqlProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Redis host cannot be empty")
    private String host;

    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must be less than 65536")
    private Integer port;

    @Min(value = 0, message = "Database index must be non-negative")
    @Max(value = 15, message = "Database index must be less than 16")
    private Integer database;

    private String username;
    private String password;

    @Min(value = 1, message = "Pool size must be at least 1")
    @Max(value = 100, message = "Pool size must not exceed 100")
    @Builder.Default
    private Integer poolSize = 8;

    @Builder.Default
    private Integer timeout = 2000;

    @Builder.Default
    private Boolean useSsl = false;

    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Long maxWaitMillis = 1000L;

    @Builder.Default
    private Boolean testOnBorrow = true;

    @Builder.Default
    private Boolean testOnReturn = false;

    @Builder.Default
    private Boolean testWhileIdle = true;
}