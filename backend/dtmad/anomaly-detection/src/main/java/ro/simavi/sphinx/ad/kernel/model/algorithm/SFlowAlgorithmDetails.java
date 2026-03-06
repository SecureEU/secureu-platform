package ro.simavi.sphinx.ad.kernel.model.algorithm;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SFlowAlgorithmDetails extends AlgorithmDetails {

    public SFlowAlgorithmDetails(String type){
        setType(type);
    }
}
