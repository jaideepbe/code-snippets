import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ApiMapping {
    private String legacy;
    private List<RationalizedApi> rationalized;
}
