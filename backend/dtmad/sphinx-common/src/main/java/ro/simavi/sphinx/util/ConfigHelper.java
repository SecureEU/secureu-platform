package ro.simavi.sphinx.util;

import ro.simavi.sphinx.entities.common.ConfigEntity;
import ro.simavi.sphinx.model.ConfigModel;

public class ConfigHelper {

    public static ConfigModel toConfigModel(ConfigEntity configEntity) {
        ConfigModel configModel = new ConfigModel();
        configModel.setId(configEntity.getId());
        configModel.setCode(configEntity.getCode());
        configModel.setValue(configEntity.getValue());
        configModel.setDescription(configEntity.getDescription());
        configModel.setName(configEntity.getName());
        configModel.setType(configEntity.getType());
        configModel.setDefaultValue(configEntity.getDefaultValue());
        return configModel;
    }

}
