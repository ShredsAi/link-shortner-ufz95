package ai.shreds.shared.value_objects;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "spring.data.redis")
public class SharedNoSqlProperties {
    private final String host;
    private final Integer port;
    private final Integer database;
    private final String username;
    private final String password;
    private final Integer poolSize;
    
    public SharedNoSqlProperties(
            String host,
            Integer port,
            @DefaultValue("0") Integer database,
            @DefaultValue("") String username,
            @DefaultValue("") String password,
            @DefaultValue("8") Integer poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    }
}