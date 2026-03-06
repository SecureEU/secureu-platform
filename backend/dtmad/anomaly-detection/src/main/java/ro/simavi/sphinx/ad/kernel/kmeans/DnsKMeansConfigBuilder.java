package ro.simavi.sphinx.ad.kernel.kmeans;

import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.model.ConfigModel;

import java.io.Serializable;
import java.util.*;

@Component
public class DnsKMeansConfigBuilder implements Serializable {

    private final AdConfigService addConfigService;
    AdmlKMeansConfig admlKMeansConfig;

    public DnsKMeansConfigBuilder(AdConfigService addConfigService){
        this.addConfigService = addConfigService;
    }

    public AdmlKMeansConfig getAdmlKMeansConfig(){
        AdmlKMeansConfig admlKMeansConfig = getDefaults();
        admlKMeansConfig = loadFromDb();

        return admlKMeansConfig;
    }

    private AdmlKMeansConfig loadFromDb(){
        if (addConfigService==null){
            return null;
        }
        // todo: populate from database
        admlKMeansConfig = new AdmlKMeansConfig();

        String numberOfCluster = getParameters().get("ad.parameter.algorithm.kmeans.dns.param.numberOfClusters").getValue();
        admlKMeansConfig.setNumberOfClusters(Integer.parseInt(numberOfCluster));

        String maxAnomalousClusterProportion = getParameters().get("ad.parameter.algorithm.kmeans.dns.param.maxAnomalousClusterProportion").getValue();
        admlKMeansConfig.setMaxAnomalousClusterProportion(Double.parseDouble(maxAnomalousClusterProportion));

        String minDirtyProportion = getParameters().get("ad.parameter.algorithm.kmeans.dns.param.minDirtyProportion").getValue();
        admlKMeansConfig.setMinDirtyProportion(Double.parseDouble(minDirtyProportion));

        admlKMeansConfig.setFeatures(getFeature());

        admlKMeansConfig.setNotNullFeatures(getNotNullFeature());

        return admlKMeansConfig;
    }
    public Map<String, ConfigModel> getParameters() {
        return addConfigService.getConfigs();
    }

    public Map<String, ConfigModel> getFilterParameter() {
        return addConfigService.getConfigs("ad.algorithm.kmeans.dns.");
    }

    public List<String> getFeature(){
        List<String> configFeature = new ArrayList<>();

        for (Map.Entry<String, ConfigModel> set : getFilterParameter().entrySet()) {
            String key = set.getKey();
            ConfigModel configModel = set.getValue();

            if (key!=null && (configModel.getValue().equals("true"))) {
                configFeature.add(configModel.getName());
            }
        }

        return configFeature;
    }

    public List<String> getNotNullFeature(){
        List<String> configNotNullFeature = new ArrayList<>();
        Map<String, String> configModelMapFiltered = new LinkedHashMap<>();

            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.dns_num_queries", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.dns_num_queries").getValue());
            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.dns_num_answers", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.dns_num_answers").getValue());
            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.dns_ret_code", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.dns_ret_code").getValue());
            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.dns_query_type", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.dns_query_type").getValue());
            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.dns_rsp_type", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.dns_rsp_type").getValue());
            configModelMapFiltered.put("ad.algorithm.kmeans.dns.feature.packet_size-1", getFilterParameter().get("ad.algorithm.kmeans.dns.feature.packet_size-1").getValue());

        for (Map.Entry<String, String> set : configModelMapFiltered.entrySet()) {
            String key = set.getKey();
            String configModel = set.getValue();

            if (key!=null && (configModel.equals("true"))) {
                configNotNullFeature.add(key);
            }
        }
        return configNotNullFeature;

    }

    public AdmlKMeansConfig getDefaults(){
        AdmlKMeansConfig admlKMeansConfig = new AdmlKMeansConfig();
        admlKMeansConfig.setNumberOfClusters(8);
        admlKMeansConfig.setMaxAnomalousClusterProportion(0.05);
        admlKMeansConfig.setMinDirtyProportion(0.001);

        admlKMeansConfig.setFeatures(getFeatures());
        admlKMeansConfig.setNotNullFeatures(getNotNullFeatures());
        return admlKMeansConfig;
    }

    private List<String> getFeatures() {

        return new ArrayList<>(Arrays.asList(
                "flow:avg_packet_size",
                "flow:packets_without_payload",
                "flow:avg_inter_time",
                "flow:flow_duration",
                "flow:max_packet_size",
                "flow:bytes",
                "flow:packets",
                "flow:min_packet_size",
                "flow:packet_size-0",
                "flow:inter_time-0",
                "flow:packet_size-1",
                "flow:dns_num_queries",
                "flow:dns_num_answers",
                "flow:dns_ret_code",
                // "flow:dns_bad_packet", // deprecated
                "flow:dns_query_type",
                "flow:dns_rsp_type"));
    }

    private List<String> getNotNullFeatures(){
        return new ArrayList<>(Arrays.asList(
                "flow:dns_num_queries",
                "flow:dns_num_answers",
                "flow:dns_ret_code",
                //   "flow:dns_bad_packet", deprecated
                "flow:dns_query_type",
                "flow:dns_rsp_type",
                "flow:packet_size-1"
        ));
    }

}
