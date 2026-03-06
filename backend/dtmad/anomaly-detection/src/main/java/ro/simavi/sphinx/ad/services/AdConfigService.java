package ro.simavi.sphinx.ad.services;


import com.typesafe.config.Config;
import ro.simavi.sphinx.model.ConfigModel;

import java.io.Serializable;
import java.util.Map;

public interface AdConfigService extends Serializable {

    void save(ConfigModel configModel);

    Map<String, ConfigModel> getConfigs();

    Map<String, ConfigModel> getConfigs(String prefix);

    Map<String, ConfigModel> getAlgorithmList();

    Map<String, ConfigModel> getAlgorithmProperties();

    ConfigModel getValue(String configCode);

    Config getConfig();

    Config getMergeConfig( Map<String,  Object> map);

    void clearCache();
}
