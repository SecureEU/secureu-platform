package ro.simavi.sphinx.dtm.ws.tshark;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.dtm.manager.ToolProcessManager;
import ro.simavi.sphinx.dtm.manager.ToolProcessRunnable;
import ro.simavi.sphinx.dtm.model.*;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;
import ro.simavi.sphinx.dtm.services.NetworkPersistService;
import ro.simavi.sphinx.dtm.services.ProcessFilterService;
import ro.simavi.sphinx.dtm.services.ProcessService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.ws.ToolController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tshark")
public class TsharkController extends ToolController {

    private final DTMConfigProps dtmConfigProps;

    private final NetworkInterfaceService networkInterfaceService;

    private final ProcessFilterService processFilterService;

    private final NetworkPersistService networkPersistService;

    @Autowired
    public TsharkController(@Qualifier("tsharkNetworkInterfaceService") NetworkInterfaceService networkInterfaceService,
                            ToolProcessManager toolManager,
                            ProcessService processService,
                            ProcessFilterService processFilterService,
                            DTMConfigProps dtmConfigProps,
                            NetworkPersistService networkPersistService){

        super(toolManager, processService);

        this.dtmConfigProps = dtmConfigProps;
        this.networkInterfaceService = networkInterfaceService;
        this.processFilterService = processFilterService;
        this.networkPersistService = networkPersistService;
    }

    @GetMapping(value = "/up")
    public String isUp() {
        return "ok";
    }

    @ApiIgnore
    @GetMapping(value = "/persist")
    public Boolean persist() {
        return networkPersistService.persist();
    }

    @GetMapping(value = "/getFields")
    public String getFields() {

        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        return toolModel.getProperties().get("fields");
    }

    @GetMapping(value = "/getProcesses")
    public List<ToolProcessStatusModel> getProcesses() {
        Map<Long, ToolProcessRunnable> toolProcessRunnableMap = getToolManager().getProcesses(DtmTools.TSHARK);
        return getProcesses(toolProcessRunnableMap);
    }

    @ApiIgnore
    @PostMapping("/process/save")
    private ResponseModel saveProcess(@RequestBody @Valid ProcessModel processModel, BindingResult bindingResult, Model model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        processModel.setProcessType(ProcessType.TSHARK);

        if (StringUtils.isEmpty(processModel.getInterfaceName())){
            NetworkInterfaceModel networkInterfaceModel = networkInterfaceService.getNetworkInterfaceModel(processModel.getInterfaceFullName());
            processModel.setInterfaceName(networkInterfaceModel.getName());
            processModel.setInterfaceDisplayName(networkInterfaceModel.getDisplayName());
            processModel.setInterfaceFullName(processModel.getInterfaceFullName());
        }
        processModel.setInstanceKey(dtmConfigProps.getInstanceKey());
        ProcessEntity processEntity = getProcessService().save(processModel);
        getToolManager().addProcess(processEntity, DtmTools.TSHARK);

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @PostMapping("/process/update")
    private ResponseModel updateProcess(@RequestBody ProcessModel processModel, BindingResult bindingResult, Model model, Principal principal) {

        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        processModel.setProcessType(ProcessType.TSHARK);

        String filterName = processModel.getFilterName();
        if (filterName!=null){
            ToolProcessRunnable toolProcessRunnable = getToolManager().getProcess(processModel.getPid());
            if (toolProcessRunnable!=null) {
                ProcessFilterModel processFilterModel = null;
                if (!StringUtils.isEmpty(filterName)){
                    processFilterModel = processFilterService.findByName(filterName);
                }
                try {
                    toolProcessRunnable.startProcess(processFilterModel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        getProcessService().save(processModel);

        return getResponseModel(Boolean.TRUE);
    }

    @GetMapping(value = "/getInterfaces")
    public List<NetworkInterfaceModel> getInterfaces() {
        try {
            return networkInterfaceService.getNetworkInterfaceModelList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

    @Override
    public String getTool() {
        return DtmTools.TSHARK.getName();
    }
}
