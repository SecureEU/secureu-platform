package ro.simavi.sphinx.ad.kernel.kmeans;

import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.model.ConfigModel;

import java.io.Serializable;
import java.util.*;

public class HttpKMeansConfigBuilder implements Serializable {

    private AdConfigService addConfigService;

    public HttpKMeansConfigBuilder(AdConfigService addConfigService){
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
        AdmlKMeansConfig admlKMeansConfig = new AdmlKMeansConfig();

        String numberOfCluster = getParameters().get("ad.parameter.algorithm.kmeans.http.param.numberOfClusters").getValue();
        admlKMeansConfig.setNumberOfClusters(Integer.parseInt(numberOfCluster));

        String maxAnomalousClusterProportion = getParameters().get("ad.parameter.algorithm.kmeans.http.param.maxAnomalousClusterProportion").getValue();
        admlKMeansConfig.setMaxAnomalousClusterProportion(Double.parseDouble(maxAnomalousClusterProportion));

        String minDirtyProportion = getParameters().get("ad.parameter.algorithm.kmeans.http.param.minDirtyProportion").getValue();
        admlKMeansConfig.setMinDirtyProportion(Double.parseDouble(minDirtyProportion));

        admlKMeansConfig.setFeatures(getFeature());
        admlKMeansConfig.setNotNullFeatures(getNotNullFeature());

        return admlKMeansConfig;
    }

    public Map<String, ConfigModel> getParameters() {
        return addConfigService.getConfigs();
    }

    public Map<String, ConfigModel> getFilterParameter() {
        return addConfigService.getConfigs("ad.algorithm.kmeans.http.");
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

        configModelMapFiltered.put("ad.algorithm.kmeans.http.feature.packet_size-1", getFilterParameter().get("ad.algorithm.kmeans.http.feature.packet_size-1").getValue());
        configModelMapFiltered.put("ad.algorithm.kmeans.http.feature.inter_time-1", getFilterParameter().get("ad.algorithm.kmeans.http.feature.inter_time-1").getValue());
        configModelMapFiltered.put("ad.algorithm.kmeans.http.feature.packet_size-2", getFilterParameter().get("ad.algorithm.kmeans.http.feature.packet_size-2").getValue());
        configModelMapFiltered.put("ad.algorithm.kmeans.http.feature.inter_time-2", getFilterParameter().get("ad.algorithm.kmeans.http.feature.inter_time-2").getValue());
        configModelMapFiltered.put("ad.algorithm.kmeans.http.feature.packet_size-3", getFilterParameter().get("ad.algorithm.kmeans.http.feature.packet_size-3").getValue());
        configModelMapFiltered.put( "ad.algorithm.kmeans.http.feature.inter_time-3", getFilterParameter().get("ad.algorithm.kmeans.http.feature.inter_time-3").getValue());
        configModelMapFiltered.put( "ad.algorithm.kmeans.http.feature.packet_size-4", getFilterParameter().get("ad.algorithm.kmeans.http.feature.packet_size-4").getValue());
        configModelMapFiltered.put( "ad.algorithm.kmeans.http.feature.inter_time-4", getFilterParameter().get("ad.algorithm.kmeans.http.feature.inter_time-4").getValue());
        configModelMapFiltered.put( "ad.algorithm.kmeans.http.feature.http_method", getFilterParameter().get("ad.algorithm.kmeans.http.feature.http_method").getValue());

        for (Map.Entry<String, String> set : configModelMapFiltered.entrySet()) {
            String key = set.getKey();
            String configModel = set.getValue();

            if (key!=null && (configModel.equals("true"))) {
                configNotNullFeature.add(key);
            }
        }
        return configNotNullFeature;

    }

    private AdmlKMeansConfig getDefaults(){
        AdmlKMeansConfig admlKMeansConfig = new AdmlKMeansConfig();
        admlKMeansConfig.setNumberOfClusters(32);
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
                "flow:inter_time-1",
                "flow:packet_size-2",
                "flow:inter_time-2",
                "flow:packet_size-3",
                "flow:inter_time-3",
                "flow:packet_size-4",
                "flow:inter_time-4",
                "flow:http_method"));
    }

    private List<String> getNotNullFeatures(){
        return new ArrayList<>(Arrays.asList(
                "flow:packet_size-1",
                "flow:inter_time-1",
                "flow:packet_size-2",
                "flow:inter_time-2",
                "flow:packet_size-3",
                "flow:inter_time-3",
                "flow:packet_size-4",
                "flow:inter_time-4",
                "flow:http_method"
        ));
    }

}
