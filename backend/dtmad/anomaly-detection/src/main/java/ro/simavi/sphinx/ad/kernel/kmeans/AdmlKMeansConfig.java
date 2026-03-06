package ro.simavi.sphinx.ad.kernel.kmeans;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AdmlKMeansConfig implements Serializable {

    private int numberOfClusters;

    private Double maxAnomalousClusterProportion;

    private Double minDirtyProportion;

    private List<String> features;

    private List<String> notNullFeatures;

}
