package ro.simavi.sphinx.dtm.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.ToolCollectorService;
import ro.simavi.sphinx.dtm.util.NetworkHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TsharkOneProcessImpl extends ToolProcessAbstract {

    private static final Logger logger = LoggerFactory.getLogger(TsharkOneProcessImpl.class);

    private long count;

    private String error;

    private String info;

    public List<ProcessModel> processModelList;

    private boolean starting = true;

    public TsharkOneProcessImpl(ToolCollectorService tsharkCollectorService, ToolModel toolModel, List<ProcessModel> processModelList){
       super(tsharkCollectorService,toolModel);
       this.processModelList = processModelList;

    }

    public List<ProcessModel> getProcessModelList(){
        return processModelList;
    }

    @Override
    protected void init() {
        count=0;
    }

    @Override
    protected String collectInputStreamProcess() {
        // catch && consume
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getInputStream()), 1)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                consume(line);
                info = "collect trafic: " + count + " packages";
                starting = false;
            }
        info = "stop process";
        logger.warn("<STOP PROCESS [tshark/collectInputStreamProcess]>/[cause:restart/error]");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    protected String collectErrorStreamProcess() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getErrorStream()), 1)){
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line+"<br/>");
            }
            error = stringBuilder.toString();
            if (error!=null && error.startsWith("Capturing on") && error.endsWith("interfaces<br/>")){
                error = null;
            }
            logger.warn("<STOP PROCESS [tshark/collectErrorStreamProcess]>/[cause:restart/error]"+error);
        } catch(Exception e){
            logger.error(e.getMessage());
        }
        starting = true;
        return error;
    }

    public void collectProcess() throws IOException {
        this.starting = true;
        this.info = null;
        this.error = null;

        collectInputStreamProcess();
        collectErrorStreamProcess();

    }

    protected List<String> getCommandAndArgs(){

        String logFiles = getToolModel().getProperties().get("logFiles");
        if (StringUtils.isEmpty(logFiles)){
            logFiles="2";
        }

        String logDuration = getToolModel().getProperties().get("logDuration");
        if (StringUtils.isEmpty(logDuration)){
            logDuration="60";
        }

        String filePcap = getToolModel().getProperties().get("logDir") + File.separator + "one_" + ".pcap";

        String fields = getToolModel().getProperties().get("fields");

        List<String> commandAndArgs = new ArrayList<>();

        String exe = getToolModel().getExe() != null && !getToolModel().getExe().isEmpty() ? getToolModel().getExe() : "tshark";
        String command = getToolModel().getPath() + File.separator + exe;

        commandAndArgs.add(command);
        commandAndArgs.add("-l");
        for(ProcessModel processModel: processModelList) {
            if (processModel.getActive() && processModel.getEnabled()) {
                String interfaceName = processModel.getInterfaceName();
                commandAndArgs.add("-i");
                commandAndArgs.add(interfaceName);
            }
        }

        if (fields!=null){
            commandAndArgs.addAll(Arrays.asList(fields.split(" ")));
        }

        // -b files:2 -b duration:10 -w c:\tools\tshark\41.pcap
        commandAndArgs.add("-b");
        commandAndArgs.add("files:"+logFiles);
        commandAndArgs.add("-b");
        commandAndArgs.add("duration:"+logDuration);
        commandAndArgs.add("-w");
        commandAndArgs.add(filePcap);

        String excludeIP = getToolModel().getProperties().get("excludeIP");
        if(!excludeIP.equals("") && excludeIP != null) {
            commandAndArgs.add(excludeIP);
        }

        return commandAndArgs;

    }

    private void consume(String message){
        ProcessFilterModel processFilterModel = null;

        count++;
        //System.out.println(message);
        String filterCode = processFilterModel!=null?processFilterModel.getCode()!=null?processFilterModel.getCode():"NO_FILTER_CODE":"NO_FILTER_CODE";
        String fullMessage = message+"\t"+NetworkHelper.getUsername()+"\t"+NetworkHelper.getHostName()+"\t"+filterCode+"\t"+"0\tN";
        //logger.info("network="+fullMessage);
        getToolCollectorService().collector(fullMessage, false);
    }

    @Override
    public ToolProcessStatusModel statusProcess() {
        String info = StringUtils.isEmpty(this.error) ?this.info:this.error;
        if (starting && info==null){
            info = "Starting...";
        }
        return new ToolProcessStatusModel(
                getProcess()!=null?getProcess().isAlive():false,
                getProcess()!=null?(getProcess().isAlive()? this.count: 0):0,
                info,
                null, processModelList, this.starting);
    }

}