package ai.shreds.domain.entities;

import lombok.Data;
import lombok.Builder;

import java.util.Date;
import java.util.Map;

@Data
@Builder
public class DomainEntityShortURL {
    private String shortKey;
    private String originalUrl;
    private Date createTimestamp;
    private int usageCount;
    private boolean validityChecks;
    private Map<String, Object> optionalMetadata;

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public boolean isValid() {
        return validityChecks && originalUrl != null && !originalUrl.isEmpty();
    }
}
