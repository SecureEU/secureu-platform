package ro.simavi.sphinx.dtm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.manager.ToolProcessManager;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.services.impl.ToolRemoteServiceImpl;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/instance")
public class InstanceController {

    private final ToolProcessManager toolProcessManager;

    private final InstanceService instanceService;

    private final ToolRemoteService toolRemoteService;

    private final DtmConfigService configService;

    private static final Logger logger = LoggerFactory.getLogger(ToolRemoteServiceImpl.class);

    @Autowired
    public InstanceController(ToolProcessManager toolProcessManager,
                              InstanceService instanceService,
                              ToolRemoteService toolRemoteService,
                              DtmConfigService configService){
        this.toolProcessManager = toolProcessManager;
        this.instanceService = instanceService;
        this.toolRemoteService = toolRemoteService;
        this.configService = configService;
    }


    @GetMapping(value = "/up")
    public String isUp() {
        return "ok";
    }

    @GetMapping(value = "/clearConfigCache")
    public void clearConfigCache() {
        configService.clearCache();
    }

    @GetMapping(value = "/clearAllConfigCache")
    public void clearAllConfigCache() {
        configService.clearCacheForAllInstances();
    }

    @GetMapping(value = "/all")
    public List<InstanceModel> getInstances() {
        List<InstanceModel> instanceModels = instanceService.findAll();

        for(InstanceModel instanceModel: instanceModels){
           // tsharkInstanceModel.setUp(Boolean.TRUE);
            logger.info("Instance URL:"+instanceModel.getUrl());
            instanceModel.setUp(toolRemoteService.isInstanceUp(instanceModel));
        }

        return instanceModels;
    }

    @ApiIgnore
    @GetMapping("/{id}")
    private InstanceModel getInstance(@PathVariable(name = "id") Long id) {
        return instanceService.getById(id);
    }

    @ApiIgnore
    @GetMapping(value = "/toggle/{id}")
    public ResponseModel getToogleInstance(@PathVariable(name = "id") Long id) {
        boolean enable = instanceService.toggleEnable(id);

        if (enable){
            //=> local tsharkManager.tryToStart();
            toolRemoteService.executeAction(id,"instance/start");
        }else{
            toolRemoteService.executeAction(id,"instance/stop");
        }
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/delete/{id}")
    public ResponseModel getDeleteInstance(@PathVariable(name = "id") Long id) {

        // send to instance to stop tshark all process */
        //=> local tsharkManager.stopAllProcesses();
        // remote:
        toolRemoteService.executeAction(id,"instance/stop");
        instanceService.delete(id);
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/stop")
    public ResponseModel stopInstance() {
        toolProcessManager.stopAllProcesses();
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/start")
    public ResponseModel startInstance() {
        toolProcessManager.tryToStart();
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @PostMapping(value = "/save")
    public ResponseModel getAddInstance(@RequestBody @Valid InstanceModel tsharkInstanceModel, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            getResponseModel(Boolean.FALSE);
        }

        instanceService.save(tsharkInstanceModel);

        return getResponseModel(Boolean.TRUE);
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
