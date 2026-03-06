package ro.simavi.sphinx.dtm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.manager.NetworkInstanceManager;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.NetworkInterfaceModel;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;
import ro.simavi.sphinx.dtm.services.ProcessService;
import ro.simavi.sphinx.dtm.util.DtmTools;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/helper/ni")
public class NetworkInterfaceController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkInterfaceController.class);

    private final NetworkInstanceManager networkInstanceManager;

    private final NetworkInterfaceService javaNetworkInterfaceService;

    private final ProcessService processService;

    private final InstanceService instanceService;

    public NetworkInterfaceController(NetworkInstanceManager networkInstanceManager,
                                      @Qualifier("javaNetworkInterfaceService") NetworkInterfaceService javaNetworkInterfaceService,
                                      ProcessService processService,
                                      InstanceService instanceService){
        this.networkInstanceManager = networkInstanceManager;
        this.javaNetworkInterfaceService = javaNetworkInterfaceService;
        this.processService = processService;
        this.instanceService = instanceService;
    }

    @GetMapping(value = "/{tool}")
    public List<NetworkInterfaceModel>getNetworkInstance(@PathVariable(name = "tool") String tool) throws IOException {

        if ("java".equals(tool)){
            return javaNetworkInterfaceService.getNetworkInterfaceModelList();
        }

        DtmTools dtmTools = DtmTools.SURICATA;
        if ("tshark".equals(tool)){
            dtmTools = DtmTools.TSHARK;
        }
        NetworkInterfaceService networkInterfaceService = networkInstanceManager.getNetworkInterfaceService(dtmTools);
        return networkInterfaceService.getNetworkInterfaceModelList();
    }

    @GetMapping(value = "/display/{instanceKey}/{tool}")
    public List<ProcessEntity> displayInterfaces(@PathVariable(name = "instanceKey") String instanceKey,
                                                     @PathVariable(name = "tool") String tool) {

        DtmTools dtmTools = DtmTools.SURICATA;
        if ("tshark".equals(tool)){
            dtmTools = DtmTools.TSHARK;
        }

        return processService.findAllInstanceKey(instanceKey, dtmTools.getProcessType());

    }

    @GetMapping(value = "/delete/{instanceKey}/{tool}/{idInterfaceName}")
    public ResponseModel deleteInterfaceName(@PathVariable(name = "instanceKey") String instanceKey,
                                             @PathVariable(name = "tool") String tool,
                                             @PathVariable(name = "idInterfaceName") Long idInterfaceName) {

        DtmTools dtmTools = DtmTools.SURICATA;
        if ("tshark".equals(tool)){
            dtmTools = DtmTools.TSHARK;
        }

        InstanceModel instanceModel = instanceService.getByKey(instanceKey);

        processService.delete(instanceModel.getId(),  dtmTools.getProcessType(), idInterfaceName);

        ResponseModel responseModel = new ResponseModel();
        responseModel.setSuccess(Boolean.TRUE);

        return responseModel;
    }

}
