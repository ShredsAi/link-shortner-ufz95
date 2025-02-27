package ai.shreds.infrastructure.repositories;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class InfrastructureNoSqlRecord {
    private String shortKey;
    private String originalUrl;
    private Date createTimestamp;
    private int usageCount;
    private boolean validityChecks;
    private Map<String, Object> optionalMetadata;
    private LocalDateTime expirationTime;
    private boolean isCustomAlias;
    private Map<String, Integer> accessByCountry;
    private Map<String, Integer> accessByDevice;
}
