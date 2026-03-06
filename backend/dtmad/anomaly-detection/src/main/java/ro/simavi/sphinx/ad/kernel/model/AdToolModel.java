package ro.simavi.sphinx.ad.kernel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.map.HashedMap;
import ro.simavi.sphinx.ad.kernel.enums.AdmlAlgorithmEnum;
import ro.simavi.sphinx.ad.kernel.enums.AdmlGeneralEnum;
import ro.simavi.sphinx.ad.kernel.enums.AdmlReputationEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class AdToolModel implements Serializable {

    private Boolean disabled;

    private Map<String, Object> properties = new HashMap<>();

    private Map<AdmlAlgorithmEnum, AdProperties> algorithms = new HashMap<>();

    private Map<AdmlGeneralEnum, AdProperties> generals = new HashMap<>();

    private Map<AdmlReputationEnum, AdProperties> reputations = new HashedMap();

}
