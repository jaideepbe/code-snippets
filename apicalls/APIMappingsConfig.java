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

    public static class ApiMapping {
        private String legacy;
        private List<RationalizedApi> rationalized;

        // Getters and Setters

        public static class RationalizedApi {
            private String url;
            private boolean dependent;
            private String dependsOn;
            private String responseModel;
            private int maxThreads;

            // Getters and Setters
        }
    }
}
