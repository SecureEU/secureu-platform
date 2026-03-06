package ro.simavi.sphinx.ad.services.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.entities.AdConfigEntity;
import ro.simavi.sphinx.ad.jpa.repositories.AdConfigRepository;
import ro.simavi.sphinx.ad.kernel.enums.*;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.entities.common.enums.ConfigType;
import ro.simavi.sphinx.model.ConfigModel;
import ro.simavi.sphinx.util.ConfigHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AdConfigServiceImpl implements AdConfigService {

    private final AdConfigRepository configRepository;

    private Map<String, ConfigModel> configModelMapCached;

    public AdConfigServiceImpl(AdConfigRepository configRepository ){
        this.configRepository = configRepository;
    }

    @Override
    public void save(ConfigModel configModel) {
        Optional<AdConfigEntity> configEntityOptional = configRepository.findById(configModel.getId());
        if (configEntityOptional.isPresent()){
            AdConfigEntity configEntity = configEntityOptional.get();
            configEntity.setValue(configModel.getValue());
            configRepository.save(configEntity);

            // update cache
            this.configModelMapCached.get(configModel.getCode()).setValue(configModel.getValue());
        }
    }

    @Override
    public Map<String, ConfigModel> getConfigs() {
        if (this.configModelMapCached==null) {
            this.configModelMapCached = new LinkedHashMap<>();
            Iterable<AdConfigEntity> configEntities = configRepository.findAllByOrderByCodeAsc();
            for (AdConfigEntity configEntity : configEntities) {
                this.configModelMapCached.put(configEntity.getCode(), ConfigHelper.toConfigModel(configEntity));
            }
        }
        return this.configModelMapCached;
    }

    @Override
    public Map<String, ConfigModel> getConfigs(String prefix) {
        Map<String, ConfigModel> configModelMapCachedFiltered = new LinkedHashMap<>();
        Map<String, ConfigModel> configModelMapDisabledFiltered = new LinkedHashMap<>();

        for (Map.Entry<String, ConfigModel> set : configModelMapCached.entrySet()) {
            String key = set.getKey();
            ConfigModel configModel = set.getValue();
            if (key!=null && key.contains("disabled") || key.startsWith("ad.kmeans.")) {
                configModelMapDisabledFiltered.put(key, configModel);
            }
            if (key!=null && key.startsWith(prefix) && !key.contains("disabled") ) {
                configModelMapCachedFiltered.put(key, configModel);
            }
            if(key!= null && prefix.startsWith("reputation")){
                for (AdmlReputationEnum reputation : AdmlReputationEnum.values()) {
                    if (key!=null && key.contains(reputation.getName())) {
                        configModelMapCachedFiltered.put(key, configModel);;
                    }
                }
            }
            if(key!= null && prefix.startsWith("general")){
                for (AdmlGeneralEnum general : AdmlGeneralEnum.values()) {
                    if (key!=null && key.contains(general.getName()) && key.startsWith("ad.adml.sflow")) {
                        configModelMapCachedFiltered.put(key, configModel);;
                    }
                }

            }
        }
        if(prefix.equals("disabled")){
            return configModelMapDisabledFiltered;
        }
        return configModelMapCachedFiltered;

    }

    @Override
    public Map<String, ConfigModel> getAlgorithmProperties() {

        Map<String, ConfigModel> configModelMaFiltered = new LinkedHashMap<>();

        for (Map.Entry<String, ConfigModel> set : configModelMapCached.entrySet()) {
            String key = set.getKey();
            ConfigModel configModel = set.getValue();

            if (key!=null){
                if (isKmeansAlgorithm(key)){
                    configModelMaFiltered.put(key,configModel);
                } else if (isSflowAlgorithm(key)){
                    configModelMaFiltered.put(key,configModel);
                }
            }
        }

        return configModelMaFiltered;
    }

    private boolean isKmeansAlgorithm(String key){

        for (AdmlAlgorithmEnum alg : KmeansAlgorithm.values()) {
            if (key.equals("ad.kmeans."+alg.getName()+".algorithm")) {
               return true;
            }
        }
        return false;
    }

    private boolean isSflowAlgorithm(String key){

        for (AdmlAlgorithmEnum alg : SflowAlgorithm.values()) {
            if (!key.endsWith(".disabled") && key.startsWith("ad.adml.sflow."+alg.getName()+".")) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Map<String, ConfigModel> getAlgorithmList() {

        Map<String, ConfigModel> configModelMapDisabledFiltered = new LinkedHashMap<>();

        //"ad.kmeans.dns.algorithm":"DNS Kmeans",
        for (AdmlAlgorithmEnum alg : KmeansAlgorithm.values()) {
            String key = "ad."+AdTools.KMEANS.getName()+"."+alg.getName()+".algorithm";
            String algorithm = AdTools.KMEANS.getName()+"_"+alg.getName();
            configModelMapDisabledFiltered.put(algorithm,configModelMapCached.get(key));
        }

        //"ad.adml.sflow.vPortScan.disabled":"Vertical portscan",
        for (AdmlAlgorithmEnum alg : SflowAlgorithm.values()) {
            String key = "ad.adml."+AdTools.SFLOW.getName()+"."+alg.getName()+".disabled";
            String algorithm = AdTools.SFLOW.getName()+"_"+alg.getName();
            configModelMapDisabledFiltered.put(algorithm,configModelMapCached.get(key));
        }

        return configModelMapDisabledFiltered;
    }

    public ConfigModel getValue(String configCode) {
        Map<String, ConfigModel> configModelMap = getConfigs();
        return configModelMap.get(configCode);
    }

    private Map<String, ?> getMap(){
        Map<String, Object> map = new HashMap<>();
        Map<String, ConfigModel> configModelMap = getConfigs("ad.adml.sflow.");

        if (configModelMap!=null){
            for (Map.Entry<String,ConfigModel> entry : configModelMap.entrySet()) {

                String code = entry.getKey();
                int count = StringUtils.countMatches(code, ".");
                if (count!=4){
                    continue;
                }
                String splitCode[] = code.split("\\.");
                String algorithm = splitCode[3];
                String property = splitCode[4];

                ConfigModel configModel = entry.getValue();
                String value = configModel.getValue();
                ConfigType configType = configModel.getType();
                if (configType==ConfigType.LIST){
                    if (StringUtils.isEmpty(value)){
                        map.put(algorithm + "." + property, "Set()");
                    }else{
                        String setValue = "Set(";
                        for (String item : value.split(",")) {
                            setValue +="\""+item+"\",";
                        }
                        setValue = setValue.substring(0, setValue.length()-1);
                        setValue += ")";
                        map.put(algorithm + "." + property, setValue);

                    }
                } else if (configType==ConfigType.BOOLEAN){
                    Boolean boolValue = Boolean.parseBoolean(value);
                    map.put(algorithm + "." + property, boolValue==true?1:0);
                } else if (configType==ConfigType.INT){
                    map.put(algorithm + "." + property, Integer.parseInt(value));
                } else if (configType==ConfigType.LONG){
                    map.put(algorithm + "." + property, Long.parseLong(value));
                } else {
                    map.put(algorithm + "." + property, value);
                }
            }
        }

        return map;
    }

    @Override
    public Config getConfig() {
        return ConfigFactory.parseMap(getMap());
    }

    @Override
    public Config getMergeConfig( Map<String, Object> map) {

        Map<String, Object> mergeConfig = map;

        Map<String, ?> dbConfig = getMap();

        for (Map.Entry<String,?> entry:dbConfig.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();
            mergeConfig.put(key,  value);
        }

        return ConfigFactory.parseMap(mergeConfig);
    }

    public void clearCache(){
        this.configModelMapCached = null;
    }
}
