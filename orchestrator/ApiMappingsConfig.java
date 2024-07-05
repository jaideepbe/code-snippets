import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "legacy.api")
public class ApiMappingsConfig {
    private List<ApiMapping> mappings;

    public List<ApiMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<ApiMapping> mappings) {
        this.mappings = mappings;
    }
}
