package ai.shreds.infrastructure.repositories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureNoSqlRecord implements Serializable {
    private String shortKey;
    private Date createTimestamp;
    private int usageCount;
    private String originalUrl;
    private boolean validityChecks;
    
    @Builder.Default
    private Map<String, Object> optionalMetadata = new HashMap<>();
    
    private LocalDateTime expirationTime;
    private boolean isCustomAlias;
    
    @Builder.Default
    private Map<String, Integer> accessByCountry = new HashMap<>();
    
    @Builder.Default
    private Map<String, Integer> accessByDevice = new HashMap<>();
}