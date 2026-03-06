package ro.simavi.sphinx.ad.kernel.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

// din java 14 => record in loc de class
@Getter
@AllArgsConstructor
public class AdmlFlow implements Serializable {

    private Map<String,String> map;
    private String lowerIp;
    private String upperIp;

    public String get(String key) {
        return map.get(key);
    }

}
