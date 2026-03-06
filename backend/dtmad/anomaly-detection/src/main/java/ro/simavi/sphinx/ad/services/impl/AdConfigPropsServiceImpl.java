package ro.simavi.sphinx.ad.services.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;
import ro.simavi.sphinx.ad.kernel.enums.*;
import ro.simavi.sphinx.ad.kernel.model.AdProperties;
import ro.simavi.sphinx.ad.kernel.model.AdToolModel;
import ro.simavi.sphinx.ad.services.AdConfigPropsService;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdConfigPropsServiceImpl implements AdConfigPropsService {

    private static final Logger logger = LoggerFactory.getLogger(AdConfigPropsServiceImpl.class);

    private final AdConfigProps adConfigProps;

    private final Environment environment;

    public AdConfigPropsServiceImpl(AdConfigProps adConfigProp, Environment environment){
        this.adConfigProps = adConfigProp;
        this.environment = environment;
    }

    public void init(){
        Map<AdTools, AdToolModel> adToolModelMap = adConfigProps.getTools();

        for (AdTools adTools : AdTools.values()) {

            AdToolModel toolModel = new AdToolModel();

            // put disabled property
            Boolean disabled = Boolean.TRUE;
            try {
                disabled = Boolean.parseBoolean(environment.getProperty("ad.adml." + adTools.getName() + ".disabled"));
            }catch (Exception e){

            }
            toolModel.setDisabled(disabled);

            // put properties
            String prefix = "ad.adml." + adTools.getName();
            toolModel.setProperties(getProperties(prefix, true));

            // put algoritms
            AdmlAlgorithmEnum[] algorithm = null;
            if (AdTools.SFLOW==adTools) {
                algorithm = SflowAlgorithm.values();
            } else if (AdTools.KMEANS==adTools){
                algorithm = KmeansAlgorithm.values();
            }

            Map<AdmlAlgorithmEnum, AdProperties> algorithms = new HashMap<>();
            for (AdmlAlgorithmEnum alg : algorithm) {
                String prefixProp = "ad.adml." + adTools.getName()+"."+alg.getName();
                algorithms.put(alg, new AdProperties(getProperties(prefixProp,false)));
            }
            toolModel.setAlgorithms(algorithms);

            // put generals
            Map<AdmlGeneralEnum, AdProperties> generals = new HashMap<>();
            for (AdmlGeneralEnum general : AdmlGeneralEnum.values()) {
                String prefixProp = "ad.adml." + adTools.getName()+"."+general.getName();
                generals.put(general, new AdProperties(getProperties(prefixProp,false)));
            }
            toolModel.setGenerals(generals);

            // put reputations
            Map<AdmlReputationEnum, AdProperties> reputations = new HashMap<>();
            for (AdmlReputationEnum reputation : AdmlReputationEnum.values()) {
                String prefixProp = "ad.adml." + adTools.getName()+"."+reputation.getName();
                reputations.put(reputation, new AdProperties(getProperties(prefixProp,false)));
            }
            toolModel.setReputations(reputations);

            adToolModelMap.put(adTools, toolModel);
        }

    }

    private Map<String, Object> getProperties(String prefix, boolean strict){
        Map<String, Object> properties = new HashMap<>();
        if (environment instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) environment).getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                        boolean ok = true;
                        if (strict){
                            int count = StringUtils.countMatches(key, ".");
                            if (count!=3){
                                ok=false;
                            }
                        }
                        if (ok && key.startsWith(prefix)) {
                            Object value = propertySource.getProperty(key);
                            if (value!=null) {

                                if (value instanceof String) {

                                    String strValue = (String) value;
                                    if ("[]".equals(strValue)) {
                                        value = new String[0];
                                    } else if (strValue.startsWith("[")) {
                                        String tmpValue = strValue.replace("[", "");
                                        tmpValue = tmpValue.replace("]", "");
                                        value = tmpValue.split(",");
                                    } else if (strValue.equals("true") || strValue.equals("false")){
                                        value = Boolean.parseBoolean((String)value);
                                    } else {

                                        try {
                                            Integer intValue = Integer.parseInt((String) value);
                                            value = intValue;
                                        } catch (Exception e) {
                                            try {
                                                String str = (String)value;
                                                if (str.endsWith("L") || str.endsWith("l")) {
                                                    Long longValue = Long.parseLong(str.substring(0, str.length()-1));
                                                    value = longValue;
                                                }
                                            }catch (Exception ee){

                                            }
                                        }
                                    }

                                }

                            }

                            properties.put(key, value);
                        }
                    }
                }
            }
        }
        return properties;
    }

    public void display(){

        for (AdTools adTools : AdTools.values()) {
            String toolName = adTools.getName();

            AdToolModel toolModel = adConfigProps.getTools().get(adTools);

            if (toolModel!=null){
                logger.info("ad.adml."+toolName+".disabled=" + toolModel.getDisabled() );

                Map<String,Object> properties = toolModel.getProperties();
                if (properties!=null) {
                    for (Map.Entry<String,Object> entry : properties.entrySet()) {
                        logger.info("ad.adml." + toolName + "."+entry.getKey()+"=" + entry.getValue());
                    }
                }

                Map<AdmlGeneralEnum,AdProperties> generals = toolModel.getGenerals();
                if (generals!=null){
                    logger.info("==== GENERAL =====");
                    for (Map.Entry<AdmlGeneralEnum,AdProperties> entry : generals.entrySet()) {

                        AdmlGeneralEnum key = entry.getKey();

                        AdProperties adProperties = entry.getValue();

                        Map<String,Object> propertiesMap = adProperties.getProperties();
                        if (propertiesMap!=null) {
                            for (Map.Entry<String,Object> prop : propertiesMap.entrySet()) {
                                logger.info("ad.adml." + toolName + "."+key.getName()+"."+prop.getKey()+"=" + prop.getValue());
                            }
                        }
                    }
                }

                Map<AdmlAlgorithmEnum,AdProperties> algorithms = toolModel.getAlgorithms();
                if (algorithms!=null){
                    logger.info("==== ALGORITHMS =====");
                    for (Map.Entry<AdmlAlgorithmEnum,AdProperties> entry : algorithms.entrySet()) {

                        AdmlAlgorithmEnum algorithm = entry.getKey();

                        AdProperties adAlgoritmProps = entry.getValue();

                        Map<String,Object> algProperties = adAlgoritmProps.getProperties();
                        if (algProperties!=null) {
                            for (Map.Entry<String,Object> prop : algProperties.entrySet()) {
                                logger.info("ad.adml." + toolName + "."+algorithm.getName()+"."+prop.getKey()+"=" + prop.getValue());
                            }
                        }
                    }
                }

                Map<AdmlReputationEnum,AdProperties> reputations = toolModel.getReputations();
                if (reputations!=null){
                    logger.info("==== REPUTATION =====");
                    for (Map.Entry<AdmlReputationEnum,AdProperties> entry : reputations.entrySet()) {

                        AdmlReputationEnum key = entry.getKey();

                        AdProperties adProperties = entry.getValue();

                        Map<String,Object> propertiesMap = adProperties.getProperties();
                        if (propertiesMap!=null) {
                            for (Map.Entry<String,Object> prop : propertiesMap.entrySet()) {
                                logger.info("ad.adml." + toolName + "."+key.getName()+"."+prop.getKey()+"=" + prop.getValue());
                            }
                        }
                    }
                }

            }
        }
    }

}
