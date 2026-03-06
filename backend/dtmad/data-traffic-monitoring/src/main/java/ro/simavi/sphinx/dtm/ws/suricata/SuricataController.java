package ro.simavi.sphinx.dtm.ws.suricata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.manager.ToolProcessManager;
import ro.simavi.sphinx.dtm.manager.ToolProcessRunnable;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.ProcessService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.ws.ToolController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suricata")
public class SuricataController extends ToolController {

    @Autowired
    public SuricataController(ToolProcessManager toolManager,
                              ProcessService processService){

        super(toolManager,processService);
    }

    @GetMapping(value = "/getProcesses")
    public List<ToolProcessStatusModel> getProcesses() {
        Map<Long, ToolProcessRunnable> toolProcessRunnableMap = getToolManager().getProcesses(DtmTools.SURICATA);
        return getProcesses(toolProcessRunnableMap);
    }


    @Override
    public String getTool() {
        return DtmTools.SURICATA.getName();
    }
}
