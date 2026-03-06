package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.DtmConfigEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.DtmConfigRepository;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.enums.ConfigCode;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.model.ConfigModel;
import ro.simavi.sphinx.util.ConfigHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DtmConfigServiceImpl implements DtmConfigService {

    private final DtmConfigRepository configRepository;

    private Map<String, ConfigModel> configModelMapCached;

    private final InstanceService instanceService;

    private final ToolRemoteServiceImpl toolRemoteService;

    public DtmConfigServiceImpl(DtmConfigRepository configRepository, InstanceService instanceService, ToolRemoteServiceImpl toolRemoteService){
        this.configRepository = configRepository;
        this.instanceService = instanceService;
        this.toolRemoteService = toolRemoteService;
    }

    @Override
    public void save(ConfigModel configModel) {
        Optional<DtmConfigEntity> configEntityOptional = configRepository.findById(configModel.getId());
        if (configEntityOptional.isPresent()){
            DtmConfigEntity configEntity = configEntityOptional.get();
            configEntity.setValue(configModel.getValue());
            configRepository.save(configEntity);

            // update cache
            this.configModelMapCached.get(configModel.getCode()).setValue(configModel.getValue());

            // reset cache for all instances;
            clearCacheForAllInstances();
        }
    }

    public void clearCacheForAllInstances() {
        List<InstanceModel> instanceModels = instanceService.findAll();
        for(InstanceModel instanceModel: instanceModels){
            toolRemoteService.clearConfigCache(instanceModel);
        }
    }

    @Override
    public Map<String, ConfigModel> getConfigs() {
        if (this.configModelMapCached==null) {
            this.configModelMapCached = new LinkedHashMap<>();
            Iterable<DtmConfigEntity> configEntities = configRepository.findAllByOrderByCodeAsc();
            for (DtmConfigEntity configEntity : configEntities) {
                this.configModelMapCached.put(configEntity.getCode(), ConfigHelper.toConfigModel(configEntity));
            }
        }
        return this.configModelMapCached;
    }

    @Override
    public Map<String, ConfigModel> getConfigs(String prefix) {

        if (this.configModelMapCached==null){
            getConfigs();
        }

        Map<String, ConfigModel> configModelMapCachedFiltered = new LinkedHashMap<>();

        for (Map.Entry<String, ConfigModel> set : configModelMapCached.entrySet()) {
            String key = set.getKey();
            ConfigModel configModel = set.getValue();

            if (key!=null && key.startsWith(prefix) && !key.contains("disabled") ) {
                configModelMapCachedFiltered.put(key, configModel);
            }

        }

        return configModelMapCachedFiltered;
    }


    public ConfigModel getValue(ConfigCode configCode) {
        Map<String, ConfigModel> configModelMap = getConfigs();
        return configModelMap.get(configCode.getCode());
    }

    public void clearCache(){
        this.configModelMapCached = null;
    }
}
