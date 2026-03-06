package ro.simavi.sphinx.dtm.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.dtm.manager.ToolProcessRunnable;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.ToolCollectorService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class ToolProcessAbstract implements ToolProcessRunnable {

    private static final Logger logger = LoggerFactory.getLogger(ToolProcessAbstract.class);

    private ToolCollectorService toolCollectorService;

    private ToolModel toolModel;

    private ProcessFilterModel processFilterModel;

    private Thread thread;

    private Boolean enabled;

    private Process process;

    public ToolProcessAbstract(ToolCollectorService toolCollectorService, ToolModel toolModel){
        this.toolCollectorService = toolCollectorService;
        this.toolModel = toolModel;
    }

    @Override
    public void startProcess() {
        startProcess(processFilterModel);
    }

    public List<ProcessModel> getProcessModelList(){
        return null;
    }

    @Override
    public void startProcess(ProcessFilterModel processFilterModel) {
        this.processFilterModel = processFilterModel;

        thread = new Thread(this);
        thread.start();

    }

    @Override
    public void stopProcess() {
        if (process!=null) {
            logger.info("<STOP PROCESS>:" + process.toString());
            try {
                process.destroy();
                process.waitFor(2, TimeUnit.SECONDS);
                if(process.isAlive()) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    protected void init(){
    }

    protected abstract String collectInputStreamProcess();

    protected abstract String collectErrorStreamProcess();

    protected abstract List<String> getCommandAndArgs();

    protected void initAndStartProcess() throws IOException {
        init();

        stopProcess();

        List<String> commandAndArgsList = getCommandAndArgs();
        ProcessBuilder pb = new ProcessBuilder(commandAndArgsList);
        Process process = pb.start();

        setProcess(process);

        logger.info("<START PROCESS>/[address]=" + process);
        logger.info("<START PROCESS>/[command]=" + Arrays.toString(commandAndArgsList.toArray()));

        collectProcess();
    }

    protected void collectProcess() throws IOException{
        collectInputStreamProcess();
        collectErrorStreamProcess();
    }

    @Override
    public void run() {
        try {
            initAndStartProcess();
        } catch (IOException e) {
           logger.error(e.getMessage());
        }
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ToolModel getToolModel() {
        return this.toolModel;
    }

    public ToolCollectorService getToolCollectorService() {
        return toolCollectorService;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        logger.info("<START PROCESS>/[old-address]=" + this.process);
        this.process = process;
    }

    public ProcessFilterModel getProcessFilterModel() {
        return processFilterModel;
    }
}
