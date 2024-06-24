import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegacyApiController {

    @Autowired
    private ApiService apiService;

    @GetMapping("/legacy/api1")
    public String handleLegacyApi1() {
        apiService.callLegacyApi("/legacy/api1");
        return "API 1 called";
    }

    @GetMapping("/legacy/api2")
    public String handleLegacyApi2() {
        apiService.callLegacyApi("/legacy/api2");
        return "API 2 called";
    }
}
