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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TsharkProcessImpl extends ToolProcessAbstract {

    private static final Logger logger = LoggerFactory.getLogger(TsharkProcessImpl.class);

    private long count;

    private String error;

    private ProcessModel processModel;

    public TsharkProcessImpl(ToolCollectorService tsharkCollectorService, ToolModel toolModel, ProcessModel processModel){
       super(tsharkCollectorService,toolModel);
       this.processModel = processModel;
    }

    @Override
    protected void init() {
        count=0;
    }

    protected String collectInputStreamProcess(){
        // input stream
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getInputStream()), 1)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                consume(line);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    protected String collectErrorStreamProcess(){
        // error stream
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getErrorStream()), 1)){
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            error = stringBuilder.toString();
            logger.error(error);
        } catch(Exception e){
            logger.error(e.getMessage());
        }
        return error;
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

        String filePcap = getToolModel().getProperties().get("logDir") + File.separator + processModel.getPid() + ".pcap";

        String fields = getToolModel().getProperties().get("fields");
        String interfaceName = processModel.getInterfaceName();
        ProcessFilterModel processFilterModel = processModel.getFilterModel();

        List<String> commandAndArgs = new ArrayList<>();

        String exe = getToolModel().getExe() != null && !getToolModel().getExe().isEmpty() ? getToolModel().getExe() : "tshark";
        String command = getToolModel().getPath() + File.separator + exe;

        commandAndArgs.add(command);
        commandAndArgs.add("-l");
        commandAndArgs.add("-i");
        /* multiple -i arguments, and capturing on multiple interfaces at once*/
        commandAndArgs.add(interfaceName);
        if (processFilterModel!=null){
            commandAndArgs.add("-Y");
            commandAndArgs.add(processFilterModel.getCommand());
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

        return commandAndArgs;

    }

    private void consume(String message){
        ProcessFilterModel processFilterModel = processModel.getFilterModel();

        count++;
        //System.out.println(message);
        String filterCode = processFilterModel!=null?processFilterModel.getCode()!=null?processFilterModel.getCode():"NO_FILTER_CODE":"NO_FILTER_CODE";
        String fullMessage = message+"\t"+NetworkHelper.getUsername()+"\t"+NetworkHelper.getHostName()+"\t"+filterCode+"\t"+"0\tN";
        //logger.info("network="+fullMessage);
        getToolCollectorService().collector(fullMessage, false);
    }

    @Override
    public ToolProcessStatusModel statusProcess() {
        return new ToolProcessStatusModel(
                getProcess()!=null?getProcess().isAlive():false,
                getProcess()!=null?(getProcess().isAlive()? this.count: 0):0,
                getProcess()!=null?(getProcess().isAlive()?"":this.error):"the process was not initialized!",
                processModel, null, false);
    }

}