package ro.simavi.sphinx.dtm.ws;

import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.impl.ToolRemoteServiceImpl;

import java.util.List;

public class CacheableController {

    private final InstanceService instanceService;

    private final ToolRemoteServiceImpl toolRemoteService;

    public CacheableController(InstanceService instanceService, ToolRemoteServiceImpl toolRemoteService){
        this.instanceService = instanceService;
        this.toolRemoteService = toolRemoteService;
    }

    public void clearCacheForAllInstances(String controler) {
        List<InstanceModel> instanceModels = instanceService.findAll();
        for(InstanceModel instanceModel: instanceModels){
            toolRemoteService.clearCache(instanceModel, controler);
        }
    }
}
