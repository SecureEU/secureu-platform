package ro.simavi.sphinx.ad.model.simulation;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AlgorithmSimulationModel {

    private String code;

    private String name;

    public AlgorithmSimulationModel(String code, String name){
        this.code = code;
        this.name = name;
    }

}
