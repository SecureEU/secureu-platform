package ro.simavi.sphinx.dtm.ws.tshark;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.model.*;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.ws.ToolRemoteController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/tshark")
public class TsharkRemoteController extends ToolRemoteController {

    private final InstanceService instanceService;

    private final ToolRemoteService toolRemoteService;

    @Autowired
    public TsharkRemoteController(InstanceService instanceService,
                                  ToolRemoteService toolRemoteService){
        super(instanceService,toolRemoteService);
        this.instanceService = instanceService;
        this.toolRemoteService = toolRemoteService;
    }

    @Override
    public String getTool() {
        return DtmTools.TSHARK.getName();
    }

    @ApiIgnore
    @PostMapping("/process/save/{instanceId}")
    private ResponseModel saveProcess(@PathVariable(name = "instanceId") Long instanceId, @RequestBody @Valid ProcessModel processModel,
                                      BindingResult bindingResult, Model model, Principal principal) {
        return toolRemoteService.saveProcess(instanceService.getById(instanceId),processModel, DtmTools.TSHARK.getName());
    }

    @ApiIgnore
    @PostMapping("/process/update/{instanceId}")
    private ResponseModel updateProcess(@PathVariable(name = "instanceId") Long instanceId, @RequestBody ProcessModel processModel,
                                        BindingResult bindingResult, Model model, Principal principal) {
        return toolRemoteService.updateProcess(instanceService.getById(instanceId),processModel, DtmTools.TSHARK.getName());
    }

    @ApiIgnore
    @GetMapping(value = "/getInterfaces/{instanceId}")
    public List<NetworkInterfaceModel> getInterfaces(@PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.getInterfaces(instanceService.getById(instanceId), DtmTools.TSHARK.getName());
    }

    @GetMapping(value = "/persist/{instanceKey}")
    public Boolean persistInstance(@PathVariable(name = "instanceKey") String instanceKey) {
        return toolRemoteService.executeAction( instanceService.getByKey(instanceKey), getTool(),"persist");
    }


    @ApiOperation(value = "Copy PCAP files generate by Tshark to a configurable location to be analyze")
    @GetMapping(value = "/persistAll")
    public Boolean persist() {
        List<InstanceModel> instanceModels = instanceService.findAll();
        for(InstanceModel instanceModel: instanceModels){
            toolRemoteService.executeAction( instanceModel, getTool(),"persist");
        }
        return true;
    }

}
