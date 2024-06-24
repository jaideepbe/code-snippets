api:
  legacy:
    base-url: "http://legacy-api-url"
  rationalized:
    base-url: "http://rationalized-api-url"


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private EndpointConfig legacy;
    private EndpointConfig rationalized;

    public EndpointConfig getLegacy() {
        return legacy;
    }

    public void setLegacy(EndpointConfig legacy) {
        this.legacy = legacy;
    }

    public EndpointConfig getRationalized() {
        return rationalized;
    }

    public void setRationalized(EndpointConfig rationalized) {
        this.rationalized = rationalized;
    }

    public static class EndpointConfig {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ApiComparisonService {

    @Autowired
    private ApiConfig apiConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> compareApis(String apiEndpoint, Class<?> responseClass) {
        String legacyUrl = apiConfig.getLegacy().getBaseUrl() + apiEndpoint;
        String rationalizedUrl = apiConfig.getRationalized().getBaseUrl() + apiEndpoint;

        Object legacyResponse = restTemplate.getForObject(legacyUrl, responseClass);
        Object rationalizedResponse = restTemplate.getForObject(rationalizedUrl, responseClass);

        Map<String, Object> legacyResponseMap = objectMapper.convertValue(legacyResponse, Map.class);
        Map<String, Object> rationalizedResponseMap = objectMapper.convertValue(rationalizedResponse, Map.class);

        return findDifferences(legacyResponseMap, rationalizedResponseMap);
    }

    private Map<String, Object> findDifferences(Map<String, Object> map1, Map<String, Object> map2) {
        MapDifference<String, Object> difference = Maps.difference(map1, map2);
        return difference.entriesDiffering();
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiComparisonController {

    @Autowired
    private ApiComparisonService apiComparisonService;

    @GetMapping("/compare-api")
    public Map<String, Object> compareApi(@RequestParam String endpoint, @RequestParam String responseType) throws ClassNotFoundException {
        Class<?> responseClass = Class.forName(responseType);
        return apiComparisonService.compareApis(endpoint, responseClass);
    }
}


<dependencies>
    <!-- Other dependencies -->

    <!-- Jackson for JSON conversion -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Guava for MapDifference -->
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>31.0.1-jre</version>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
