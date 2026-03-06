package ro.simavi.sphinx.dtm.ws.suricata;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.ws.ToolRemoteController;

@RestController
@RequestMapping("/suricata")
public class SuricataRemoteController extends ToolRemoteController {

    @Autowired
    public SuricataRemoteController(InstanceService instanceService, ToolRemoteService toolRemoteService){
        super(instanceService, toolRemoteService);
    }

    @Override
    public String getTool() {
        return DtmTools.SURICATA.getName();
    }

}
