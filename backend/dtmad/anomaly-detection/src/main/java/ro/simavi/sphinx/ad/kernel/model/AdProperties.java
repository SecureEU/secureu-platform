package ro.simavi.sphinx.ad.kernel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class AdProperties implements Serializable {

    private Map<String, Object> properties = new HashMap<>();

    public AdProperties(Map<String, Object> properties){
        setProperties(properties);
    }
}
