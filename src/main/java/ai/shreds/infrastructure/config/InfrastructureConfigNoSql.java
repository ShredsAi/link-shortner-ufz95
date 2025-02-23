package ai.shreds.infrastructure.config;

import ai.shreds.shared.value_objects.SharedNoSqlProperties;
import ai.shreds.infrastructure.repositories.InfrastructureRedisTemplate;
import ai.shreds.infrastructure.repositories.InfrastructureNoSqlOperations;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.DefaultClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
@EnableRetry
public class InfrastructureConfigNoSql implements HealthIndicator {

    private final SharedNoSqlProperties properties;
    private final MeterRegistry meterRegistry;
    private RedisConnectionFactory connectionFactory;
    private final DefaultClientResources clientResources;

    public InfrastructureConfigNoSql(SharedNoSqlProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.clientResources = DefaultClientResources.builder()
            .build();
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection factory with host: {}, port: {}", 
            properties.getHost(), properties.getPort());

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getHost());
        redisConfig.setPort(properties.getPort());
        redisConfig.setDatabase(properties.getDatabase());
        
        if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
            redisConfig.setUsername(properties.getUsername());
            redisConfig.setPassword(properties.getPassword());
        }

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(properties.getPoolSize());
        poolConfig.setMaxIdle(properties.getPoolSize() / 4);
        poolConfig.setMinIdle(properties.getPoolSize() / 8);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        ClientOptions clientOptions = ClientOptions.builder()
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .autoReconnect(true)
            .build();

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .clientResources(clientResources)
            .commandTimeout(Duration.ofSeconds(5))
            .build();

        connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
        registerMetrics(poolConfig);
        return connectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMaxInterval(1500);
        backOffPolicy.setMultiplier(1.5);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public InfrastructureNoSqlOperations nosqlTemplate(RedisTemplate<String, Object> redisTemplate) {
        return new InfrastructureRedisTemplate(redisTemplate);
    }

    public void initializeConnection() {
        log.info("Initializing Redis connection");
        if (connectionFactory instanceof LettuceConnectionFactory) {
            ((LettuceConnectionFactory) connectionFactory).afterPropertiesSet();
        }
    }

    public void closeConnection() {
        log.info("Closing Redis connection");
        if (connectionFactory instanceof LettuceConnectionFactory) {
            ((LettuceConnectionFactory) connectionFactory).destroy();
        }
        clientResources.shutdown();
    }

    private void registerMetrics(GenericObjectPoolConfig<?> poolConfig) {
        meterRegistry.gauge("redis.pool.maxTotal", poolConfig, GenericObjectPoolConfig::getMaxTotal);
        meterRegistry.gauge("redis.pool.maxIdle", poolConfig, GenericObjectPoolConfig::getMaxIdle);
        meterRegistry.gauge("redis.pool.minIdle", poolConfig, GenericObjectPoolConfig::getMinIdle);
        meterRegistry.gauge("redis.pool.numActive", poolConfig, GenericObjectPoolConfig::getMaxTotal);
    }

    @Override
    public Health health() {
        try {
            RedisTemplate<String, Object> template = redisTemplate(connectionFactory);
            template.execute(connection -> connection.ping());

            Map<String, Object> details = new HashMap<>();
            details.put("host", properties.getHost());
            details.put("port", properties.getPort());
            details.put("database", properties.getDatabase());

            return Health.up()
                .withDetails(details)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}