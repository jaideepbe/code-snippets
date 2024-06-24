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

    public void callLegacyApi(String legacyApi) {
        ApiMappingsConfig.ApiMapping mapping = apiMappingsConfig.getMappings().stream()
                .filter(m -> m.getLegacy().equals(legacyApi))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No mapping found for legacy API: " + legacyApi));

        List<ApiMappingsConfig.ApiMapping.RationalizedApi> independentApis = mapping.getRationalized().stream()
                .filter(r -> !r.isDependent())
                .collect(Collectors.toList());

        List<ApiMappingsConfig.ApiMapping.RationalizedApi> dependentApis = mapping.getRationalized().stream()
                .filter(ApiMappingsConfig.ApiMapping.RationalizedApi::isDependent)
                .collect(Collectors.toList());

        // Call independent APIs in parallel with respect to maxThreads
        List<CompletableFuture<Void>> futures = independentApis.stream()
                .map(api -> {
                    Executor executor = getExecutor(api.getUrl(), api.getMaxThreads());
                    return CompletableFuture.runAsync(() -> callApiAndHandleResponse(api), executor);
                })
                .collect(Collectors.toList());

        // Call dependent APIs sequentially with respect to maxThreads
        for (ApiMappingsConfig.ApiMapping.RationalizedApi api : dependentApis) {
            ApiMappingsConfig.ApiMapping.RationalizedApi dependsOnApi = dependentApis.stream()
                    .filter(a -> a.getUrl().equals(api.getDependsOn()))
                    .findFirst()
                    .orElse(null);

            if (dependsOnApi != null) {
                Executor dependsOnExecutor = getExecutor(dependsOnApi.getUrl(), dependsOnApi.getMaxThreads());
                CompletableFuture<Void> dependsOnFuture = CompletableFuture.runAsync(() -> callApiAndHandleResponse(dependsOnApi), dependsOnExecutor);

                futures.add(dependsOnFuture.thenRunAsync(() -> {
                    Executor apiExecutor = getExecutor(api.getUrl(), api.getMaxThreads());
                    callApiAndHandleResponse(api);
                }, apiExecutor));
            } else {
                Executor executor = getExecutor(api.getUrl(), api.getMaxThreads());
                futures.add(CompletableFuture.runAsync(() -> callApiAndHandleResponse(api), executor));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private Executor getExecutor(String url, int maxThreads) {
        return executors.computeIfAbsent(url, key -> Executors.newFixedThreadPool(maxThreads));
    }

    private void callApiAndHandleResponse(ApiMappingsConfig.ApiMapping.RationalizedApi api) {
        try {
            Class<?> responseClass = Class.forName(api.getResponseModel());
            Object response = restTemplate.getForObject(api.getUrl(), responseClass);
            // Process the response as needed
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Response model class not found: " + api.getResponseModel(), e);
        }
    }
}
