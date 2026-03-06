package ro.simavi.sphinx.ad.kernel.model.algorithm.kmeans;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.KMeansAlgorithmDetails;

@Getter
@Setter
@ToString
public class DnsKMeansAlgorithmDetails extends KMeansAlgorithmDetails {

    public DnsKMeansAlgorithmDetails(long numberOfClusters, Double minDirtyProportion, Double maxAnomalousClusterProportion){
        super("dns_kmeans_clustering", numberOfClusters, minDirtyProportion, maxAnomalousClusterProportion);
    }

}
