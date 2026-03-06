package ro.simavi.sphinx.ad.kernel.model.algorithm.kmeans;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.KMeansAlgorithmDetails;

@Getter
@Setter
@ToString
public class HttpKmeansAlgorithmDetails extends KMeansAlgorithmDetails {

    public HttpKmeansAlgorithmDetails(long numberOfClusters, Double minDirtyProportion, Double maxAnomalousClusterProportion){
        super("http_kmeans_clustering", numberOfClusters, minDirtyProportion, maxAnomalousClusterProportion);
    }

}
