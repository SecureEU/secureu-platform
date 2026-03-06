package ro.simavi.sphinx.dtm.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.ToolCollectorService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SuricataProcessImpl extends ToolProcessAbstract {

    private static final Logger logger = LoggerFactory.getLogger(SuricataProcessImpl.class);

    private String error;

    private String info;

    public ProcessModel processModel;

    public SuricataProcessImpl(ToolCollectorService tsharkCollectorService, ToolModel toolModel, ProcessModel processModel){
        super(tsharkCollectorService, toolModel);
        this.processModel = processModel;
    }

    protected String collectInputStreamProcess(){
        try {
            info = null;
            StringBuilder inputStreamStringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getInputStream()), 1);
            String line = null;
            while ((line = br.readLine()) != null) {
                inputStreamStringBuilder.append(line + "<br/>");
            }
            info = inputStreamStringBuilder.toString();
            logger.info("[suricata]:" + info);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return info;
    }

    protected String collectErrorStreamProcess(){

        try {
            error = null;
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getErrorStream()), 1);
            String line = null;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            error = stringBuilder.toString();
            logger.error("[suricata]:"+error);
        } catch(Exception e){
            logger.error("error:"+e);
        }

        return error;
    }

    protected List<String> getCommandAndArgs(){
        String interfaceName = processModel.getInterfaceName();

        List<String> commandAndArgs = new ArrayList<>();

        String command = getToolModel().getPath() + File.separator + "suricata";
        commandAndArgs.add(command);
        commandAndArgs.add("-l");
        commandAndArgs.add(getToolModel().getPath() + File.separator+"log");
        commandAndArgs.add("-c");
        commandAndArgs.add(getToolModel().getPath() + File.separator + "suricata.yaml");

        /* multiple -i arguments, and capturing on multiple interfaces at once*/
        commandAndArgs.add("-i");
        commandAndArgs.add(interfaceName);
        // commandAndArgs.add("\\"+interfaceName); // daca se foloseste "tshark -D", trebuie pus "\" in fata

        return commandAndArgs;

    }

    @Override
    public ToolProcessStatusModel statusProcess() {
        if (this.error==null || this.error.equals("")){
            this.error = this.info;
        }
        return new ToolProcessStatusModel(
                getProcess()!=null?getProcess().isAlive():false,
                0,
                getProcess()!=null?(getProcess().isAlive()?this.info:(this.error)):"the process was not initialized!",
                processModel,null,false);
    }
}
