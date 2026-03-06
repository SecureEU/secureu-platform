package ro.simavi.sphinx.dtm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.manager.ToolProcessManager;
import ro.simavi.sphinx.dtm.manager.ToolProcessRunnable;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.ProcessService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public abstract class ToolController {

    private static final Logger logger = LoggerFactory.getLogger(ToolController.class);

    private final ToolProcessManager toolManager;

    private final ProcessService processService;

    public ToolController(ToolProcessManager toolManager,
                   ProcessService processService){

        this.toolManager = toolManager;
        this.processService = processService;
    }

    public abstract String getTool();

    @ApiIgnore
    @GetMapping(value = "/stop/{pid}")
    public ToolProcessStatusModel stop(@PathVariable(name = "pid") Long pid) {
        ToolProcessRunnable toolProcessRunnable = getToolManager().getProcess(pid);
        if (toolProcessRunnable!=null) {
            toolProcessRunnable.stopProcess();

            DtmTools dtmTools = DtmTools.valueOf(getTool().toUpperCase());
            ToolProcessStatusModel statusModel = toolProcessRunnable.statusProcess();

            if (!dtmTools.isOneProcess()) {
                statusModel.getProcessModel().setPid(pid);
            }
            return statusModel;

        }else{
            return invalidStatus();
        }
    }

    @ApiIgnore
    @GetMapping(value = "/status/{pid}")
    public ToolProcessStatusModel status(@PathVariable(name = "pid") Long pid) {

        ToolProcessRunnable toolProcessRunnable = getToolManager().getProcess(pid);
        if (toolProcessRunnable!=null) {
            ToolProcessStatusModel statusModel = toolProcessRunnable.statusProcess();
            statusModel.getProcessModel().setPid(pid);
            if (toolProcessRunnable.getProcessFilterModel()!=null) {
                statusModel.getProcessModel().setFilterName(toolProcessRunnable.getProcessFilterModel().getName());
            }
            return statusModel;
        }else{
            return invalidStatus();
        }
    }

    @ApiIgnore
    @GetMapping(value = "/start/{pid}")
    public ToolProcessStatusModel start(@PathVariable(name = "pid") Long pid) {

        ToolProcessRunnable toolProcessRunnable = getToolManager().getProcess(pid);
        if (toolProcessRunnable != null) {
            try {
                DtmTools dtmTools = DtmTools.valueOf(getTool().toUpperCase());

                toolProcessRunnable.startProcess();

                ToolProcessStatusModel statusModel = toolProcessRunnable.statusProcess();
                statusModel.setNoPcap(0);
                statusModel.setInfo("starting");
                if (!dtmTools.isOneProcess()) {
                    statusModel.getProcessModel().setPid(pid);
                }

                statusModel.setAlive(true);
                return statusModel;

            } catch (IOException e) {
                logger.error(e.getMessage());
                return invalidStatus();
            }
        } else {
            return invalidStatus();
        }

    }

    private ToolProcessStatusModel invalidStatus(){
        return new ToolProcessStatusModel(false,0,"invalid pid",null, null, false);
    }

    private ToolProcessStatusModel avoidStatus(){
        return new ToolProcessStatusModel(false,0,"",null, null, false);
    }

    protected ToolProcessStatusModel setEnabled(Boolean enabled, Long pid){
        ToolProcessRunnable toolProcessRunnable = toolManager.getProcess(pid);
        if (toolProcessRunnable!=null) {
            toolProcessRunnable.setEnabled(enabled);
            Optional<ProcessEntity> optionalProcessEntity = processService.findById(pid);
            if (optionalProcessEntity.isPresent()){
                ProcessEntity processEntity = optionalProcessEntity.get();
                processEntity.setEnabled(enabled);
                processService.save(processEntity);
            }
            ToolProcessStatusModel statusModel = toolProcessRunnable.statusProcess();
            statusModel.getProcessModel().setPid(pid);
            statusModel.getProcessModel().setEnabled(enabled);
            return statusModel;
        }else{
            return invalidStatus();
        }
    }

    protected ToolProcessStatusModel setEnabledAndRestart(Boolean enabled, Long pid, Long networkIntefaceId){
        ToolProcessRunnable toolProcessRunnable = toolManager.getProcess(pid);
        if (toolProcessRunnable!=null) {

            Optional<ProcessEntity> optionalProcessEntity = processService.findById(networkIntefaceId);
            if (optionalProcessEntity.isPresent()) {
                ProcessEntity processEntity = optionalProcessEntity.get();
                processEntity.setEnabled(enabled);
                processService.save(processEntity);

                String interfaceName = processEntity.getInterfaceName();

                List<ProcessModel> processModels = toolProcessRunnable.getProcessModelList();
                if (processModels != null) {
                    for (ProcessModel processModel : processModels) {
                        if (interfaceName.equals(processModel.getInterfaceName())) {
                            processModel.setEnabled(enabled);
                            processModel.setActive(enabled);
                        }
                    }
                }

                try {
                    toolProcessRunnable.startProcess();
                    return toolProcessRunnable.statusProcess();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return invalidStatus();

    }

    protected List<ToolProcessStatusModel> getProcesses(Map<Long, ToolProcessRunnable> toolProcessRunnableMap) {

        List<ToolProcessStatusModel> toolProcessStatusModels = new ArrayList<>();

        toolProcessRunnableMap.forEach(
                (pid,toolProcessRunnable) -> {
                    ToolProcessStatusModel statusModel = toolProcessRunnable.statusProcess();
                    if (pid>=0) {
                        statusModel.getProcessModel().setPid(pid);
                    }
                    toolProcessStatusModels.add(statusModel);
                });

        return toolProcessStatusModels;
    }

    @ApiIgnore
    @GetMapping(value = "/disable/{pid}")
    public ToolProcessStatusModel disable(@PathVariable(name = "pid") Long pid) {

        DtmTools dtmTools = DtmTools.valueOf(getTool().toUpperCase());
        if (dtmTools.isOneProcess()){
            // restart process
            return setEnabledAndRestart(Boolean.FALSE, dtmTools.getOneProcessPid(), pid);
        }else {
            stop(pid);
            return setEnabled(Boolean.FALSE, pid);
        }
    }

    @ApiIgnore
    @GetMapping(value = "/enable/{pid}")
    public ToolProcessStatusModel enable(@PathVariable(name = "pid") Long pid) {
        DtmTools dtmTools = DtmTools.valueOf(getTool().toUpperCase());
        if (dtmTools.isOneProcess()) {
            // restart process
            return setEnabledAndRestart(Boolean.TRUE, dtmTools.getOneProcessPid(), pid);
        }else{
            return setEnabled(Boolean.TRUE, pid);
        }
    }

    public ToolProcessManager getToolManager() {
        return toolManager;
    }

    public ProcessService getProcessService() {
        return processService;
    }

}
