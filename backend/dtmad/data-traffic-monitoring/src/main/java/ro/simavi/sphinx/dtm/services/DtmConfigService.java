package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.model.enums.ConfigCode;
import ro.simavi.sphinx.model.ConfigModel;

import java.util.Map;

public interface DtmConfigService {

    void save(ConfigModel configModel);

    Map<String, ConfigModel> getConfigs();

    Map<String, ConfigModel> getConfigs(String prefix);

    void clearCache();

    void clearCacheForAllInstances();

    ConfigModel getValue(ConfigCode configCode);
}
