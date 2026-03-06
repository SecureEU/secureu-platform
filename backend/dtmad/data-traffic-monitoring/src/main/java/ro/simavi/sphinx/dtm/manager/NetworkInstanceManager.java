package ro.simavi.sphinx.dtm.manager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;
import ro.simavi.sphinx.dtm.util.DtmTools;

@Component
public class NetworkInstanceManager {

    private final NetworkInterfaceService tsharkNetworkInterfaceService;

    private final NetworkInterfaceService javaNetworkInterfaceService;

    private final DTMConfigProps dtmConfigProps;

    public NetworkInstanceManager(@Qualifier("tsharkNetworkInterfaceService") NetworkInterfaceService tsharkNetworkInterfaceService,
                                  @Qualifier("javaNetworkInterfaceService") NetworkInterfaceService javaNetworkInterfaceService,
                                  DTMConfigProps dtmConfigProps
                                 ){
        this.tsharkNetworkInterfaceService = tsharkNetworkInterfaceService;
        this.javaNetworkInterfaceService = javaNetworkInterfaceService;
        this.dtmConfigProps = dtmConfigProps;
    }

    public NetworkInterfaceService getNetworkInterfaceService(DtmTools dtmTools){
        if (dtmTools.getProcessType()==ProcessType.TSHARK){
            return tsharkNetworkInterfaceService;
        }
        if (dtmTools.getProcessType()==ProcessType.SURICATA){
            ToolModel toolModel =  dtmConfigProps.getToolModelHashMap().get(dtmTools.getName());
            String networkInterfaceDiscoveyAlgorithm = toolModel.getProperties().get("networkInterfaceDiscoveyAlgorithm");
            if (networkInterfaceDiscoveyAlgorithm.equalsIgnoreCase("tshark")){
                return tsharkNetworkInterfaceService;
            }
            return javaNetworkInterfaceService;
        }
        return null;
    }

}
