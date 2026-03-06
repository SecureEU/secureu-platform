package ro.simavi.sphinx.ad.kernel.model.algorithm;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KMeansAlgorithmDetails extends AlgorithmDetails {

    private long numberOfClusters;

    private Double minDirtyProportion;

    private Double maxAnomalousClusterProportion;

    public KMeansAlgorithmDetails(String type, long numberOfClusters, Double minDirtyProportion, Double maxAnomalousClusterProportion){
        setType(type);
        setNumberOfClusters(numberOfClusters);
        setMinDirtyProportion(minDirtyProportion);
        setMaxAnomalousClusterProportion(maxAnomalousClusterProportion);
    }

}
