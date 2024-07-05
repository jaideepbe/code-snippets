import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ApiService {

    @Autowired
    private ApiMappingsConfig apiMappingsConfig;

    @Autowired
    private RestTemplate restTemplate;

    private ConcurrentHashMap<String, Executor> executors = new ConcurrentHashMap<>();

    public LegacyResponse callLegacyApi(String legacyApi) {
        ApiMapping mapping = apiMappingsConfig.getMappings().stream()
                .filter(m -> m.getLegacy().equals(legacyApi))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No mapping found for legacy API: " + legacyApi));

        List<RationalizedApi> independentApis = mapping.getRationalized().stream()
                .filter(r -> !r.isDependent())
                .collect(Collectors.toList());

        List<RationalizedApi> dependentApis = mapping.getRationalized().stream()
                .filter(RationalizedApi::isDependent)
                .collect(Collectors.toList());

        // Call independent APIs in parallel
        List<Object> responses1 = callIndependentApisParallel(independentApis);

        // Assuming we need to extract some field from the first response to pass to the second
        String dependentFieldValue = extractDependencyFieldValue(responses1, independentApis);

        // Call dependent APIs sequentially
        List<Object> responses2 = callDependentApisSequential(dependentApis, dependentFieldValue);

        return mapToLegacyResponse(responses1, responses2);
    }

    private List<Object> callIndependentApisParallel(List<RationalizedApi> apis) {
        List<CompletableFuture<Object>> futures = apis.stream()
                .map(api -> {
                    Executor executor = getExecutor(api.getUrl(), api.getMaxThreads());
                    return CompletableFuture.supplyAsync(() -> callApi(api), executor);
                })
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private List<Object> callDependentApisSequential(List<RationalizedApi> apis, String dependencyFieldValue) {
        return apis.stream()
                .map(api -> {
                    Executor executor = getExecutor(api.getUrl(), api.getMaxThreads());
                    return CompletableFuture.supplyAsync(() -> callDependentApi(api, dependencyFieldValue), executor).join();
                })
                .collect(Collectors.toList());
    }

    private Executor getExecutor(String url, int maxThreads) {
        return executors.computeIfAbsent(url, key -> Executors.newFixedThreadPool(maxThreads));
    }

    private Object callApi(RationalizedApi api) {
        try {
            Class<?> responseClass = Class.forName(api.getResponseModel());
            return restTemplate.getForObject(api.getUrl(), responseClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Response model class not found: " + api.getResponseModel(), e);
        }
    }

    private Object callDependentApi(RationalizedApi api, String dependencyFieldValue) {
        // Assuming the dependent API needs the field value as a query parameter
        String urlWithDependency = api.getUrl() + "?dependency=" + dependencyFieldValue;
        try {
            Class<?> responseClass = Class.forName(api.getResponseModel());
            return restTemplate.getForObject(urlWithDependency, responseClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Response model class not found: " + api.getResponseModel(), e);
        }
    }

    private String extractDependencyFieldValue(List<Object> responses, List<RationalizedApi> apis) {
        // Logic to extract the dependency field value from the responses
        // This should be based on the specific response structure and the dependencyField defined in the RationalizedApi
        // For demonstration purposes, we'll assume the dependency field is extracted from the first response's first item

        for (int i = 0; i < responses.size(); i++) {
            Object response = responses.get(i);
            RationalizedApi api = apis.get(i);
            if (api.getDependencyField() != null) {
                // Extract the dependency field value using reflection
                try {
                    Class<?> responseClass = Class.forName(api.getResponseModel());
                    Object item = responseClass.getMethod("getItems").invoke(response);
                    if (item instanceof List) {
                        Object firstItem = ((List<?>) item).get(0);
                        return (String) firstItem.getClass().getMethod("get" + capitalize(api.getDependencyField())).invoke(firstItem);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error extracting dependency field value", e);
                }
            }
        }
        return null;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private LegacyResponse mapToLegacyResponse(List<Object> responses1, List<Object> responses2) {
        LegacyResponse legacyResponse = new LegacyResponse();

        // Assuming the legacyResponse property1 is combined from the responses based on the commonId
        for (Object response1 : responses1) {
            try {
                List<?> items1 = (List<?>) response1.getClass().getMethod("getItems").invoke(response1);
                for (Object item1 : items1) {
                    String commonId1 = (String) item1.getClass().getMethod("getCommonId").invoke(item1);
                    String propertyA = (String) item1.getClass().getMethod("getPropertyA").invoke(item1);

                    for (Object response2 : responses2) {
                        List<?> items2 = (List<?>) response2.getClass().getMethod("getItems").invoke(response2);
                        for (Object item2 : items2) {
                            String commonId2 = (String) item2.getClass().getMethod("getCommonId").invoke(item2);
                            String propertyB = (String) item2.getClass().getMethod("getPropertyB").invoke(item2);

                            if (commonId1.equals(commonId2)) {
                                legacyResponse.setProperty1(propertyA + " " + propertyB);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error mapping to legacy response", e);
            }
        }

        return legacyResponse;
    }
}
