package ro.simavi.sphinx.ad.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ro.simavi.sphinx.ad.kernel.enums.*;
import ro.simavi.sphinx.ad.kernel.model.AdProperties;
import ro.simavi.sphinx.ad.kernel.model.AdToolModel;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix="ad")
public class AdConfigProps {

    private String name;

    private Map<AdTools, AdToolModel> tools = new HashMap<>();

    public AdProperties getSFlowAlgoritmProps(SflowAlgorithm algorithm){
        return getTools().get(AdTools.SFLOW).getAlgorithms().get(algorithm);
    }

    public AdProperties getKmeansAlgoritmProps(KmeansAlgorithm algorithm){
        return getTools().get(AdTools.KMEANS).getAlgorithms().get(algorithm);
    }

    public Map<String, Object> getMap(AdTools tool){
        Map<String, Object> map = new HashMap<>();

        Map<AdmlAlgorithmEnum, AdProperties> algorithms = getTools().get(tool).getAlgorithms();
        if (algorithms!=null){
            for (Map.Entry<AdmlAlgorithmEnum,AdProperties> entry : algorithms.entrySet()) {
                AdmlAlgorithmEnum algorithm = entry.getKey();
                AdProperties adProperties = entry.getValue();

                populate(map, adProperties.getProperties(), algorithm.getName());
            }
        }

        Map<AdmlGeneralEnum, AdProperties> generals = getTools().get(tool).getGenerals();

        if (generals!=null){
            for (Map.Entry<AdmlGeneralEnum,AdProperties> entry : generals.entrySet()) {
                AdmlGeneralEnum general = entry.getKey();
                AdProperties adProperties = entry.getValue();

                populate(map, adProperties.getProperties(), general.getName());
            }
        }

        return map;
    }

    private void populate(Map<String,Object> map, Map<String, Object> properties, String key){
        if (properties!=null) {
            for (Map.Entry<String,Object> prop : properties.entrySet()) {
                Object value = prop.getValue();
                String property =  prop.getKey();
                property = property.substring(property.lastIndexOf(".")+1);
                if (value instanceof Boolean){
                    map.put(key + "." + property, ((Boolean)value)==true?1:0);
                }else if (value instanceof String[]){
                    String[] list = (String[])value;
                    String setValue = "";
                    if (list.length==0){
                        setValue = "Set()";
                    }else {
                        setValue = "Set(";
                        for (String item : list) {
                            setValue +="\""+item+"\",";
                        }
                        setValue = setValue.substring(0, setValue.length()-1);
                        setValue += ")";
                    }
                    map.put(key + "." + property, setValue);
                }else {
                    map.put(key + "." + property, prop.getValue());
                }
            }
        }
    }

    public Config getConfig(Map<String, ?> map){
        return ConfigFactory.parseMap(map);
    }
    public Config getConfig(AdTools tool){
        return ConfigFactory.parseMap(getMap(tool));
    }

}
