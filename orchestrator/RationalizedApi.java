import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RationalizedApi {
    private String url;
    private boolean dependent;
    private String dependsOn;
    private String dependencyField; // Field name to pass as input
    private String responseModel;
    private int maxThreads;
}
