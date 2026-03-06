package ro.simavi.sphinx.dtm.ws;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
public abstract class ToolRemoteController {

    private final InstanceService instanceService;

    private final ToolRemoteService toolRemoteService;

    @Autowired
    public ToolRemoteController(InstanceService instanceService, ToolRemoteService toolRemoteService){

        this.instanceService = instanceService;
        this.toolRemoteService = toolRemoteService;
    }

    public abstract String getTool();

    @ApiIgnore
    @GetMapping(value = "/getProcesses/{instanceId}")
    public List<ToolProcessStatusModel> getProcesses(@PathVariable(name = "instanceId") Long instanceId) {
        try {
            return toolRemoteService.getProcesses(instanceService.getById(instanceId), getTool());
        }catch (Exception e){
            return new ArrayList<>();
        }
    }

    @ApiIgnore
    @GetMapping(value = "/status/{pid}/{instanceId}")
    public ToolProcessStatusModel status(@PathVariable(name = "pid") Long pid, @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.executeAction( instanceService.getById(instanceId), getTool(), "status", pid);
    }

    @ApiIgnore
    @GetMapping(value = "/disable/{pid}/{instanceId}")
    public ToolProcessStatusModel disable(@PathVariable(name = "pid") Long pid, @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.executeAction( instanceService.getById(instanceId), getTool(),"disable", pid);
    }

    @ApiIgnore
    @GetMapping(value = "/enable/{pid}/{instanceId}")
    public ToolProcessStatusModel enable(@PathVariable(name = "pid") Long pid, @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.executeAction( instanceService.getById(instanceId), getTool(), "enable", pid);
    }

    @ApiIgnore
    @GetMapping(value = "/start/{pid}/{instanceId}")
    public ToolProcessStatusModel start(@PathVariable(name = "pid") Long pid, @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.executeAction( instanceService.getById(instanceId), getTool(), "start", pid);
    }

    @ApiIgnore
    @GetMapping(value = "/stop/{pid}/{instanceId}")
    public ToolProcessStatusModel stop(@PathVariable(name = "pid") Long pid, @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.executeAction( instanceService.getById(instanceId), getTool(),"stop", pid);
    }

    @ApiIgnore
    @GetMapping(value = "/metric/{name}/{instanceId}")
    public HashMap getMetric(@PathVariable(name = "name") String name,
                             @PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.getMetric(instanceService.getById(instanceId),name, getTool());
    }

}
