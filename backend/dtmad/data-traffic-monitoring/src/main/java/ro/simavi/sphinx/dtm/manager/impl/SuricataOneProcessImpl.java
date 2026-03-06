package ro.simavi.sphinx.dtm.manager.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;
import ro.simavi.sphinx.dtm.services.ToolCollectorService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SuricataOneProcessImpl extends ToolProcessAbstract {

    private static final Logger logger = LoggerFactory.getLogger(SuricataOneProcessImpl.class);

    private String error;

    private String info;

    private boolean starting = false;

    boolean tryAgain = false;

    public List<ProcessModel> processModelList;

    public SuricataOneProcessImpl(ToolCollectorService tsharkCollectorService, ToolModel toolModel, List<ProcessModel> processModelList){
        super(tsharkCollectorService, toolModel);
        this.processModelList = processModelList;
    }

    public List<ProcessModel> getProcessModelList(){
        return processModelList;
    }

    @Override
    protected String collectInputStreamProcess() {
        try {
            StringBuilder inputStreamStringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getInputStream()), 1);
            String line = null;
            while (this.starting && (line = br.readLine()) != null) {
                inputStreamStringBuilder.append(line + "<br/>");
                this.info = inputStreamStringBuilder.toString();
                logger.info("[suricata]/[info]:" + line);
                if (this.info.contains("running in SYSTEM mode")) {
                    this.starting = false;
                }
            }
            this.starting = false;
            this.info = inputStreamStringBuilder.toString();
            //logger.info("[suricata]/[command]:" + getCommandAndArgs().toString() + " " + info);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return this.info;
    }

    @Override
    protected String collectErrorStreamProcess() {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(getProcess().getErrorStream()), 1);
            String line = null;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line+"<br/>");
                logger.error("[suricata]/[error]:"+line);
                String ip = getIP(line);
                if (ip!=null){
                    this.tryAgain = true;
                    deactivateIP(ip, processModelList);
                    logger.error("[suricata]/[deactivate]:"+ip + " and try again!");
                }
            }
            this.error = stringBuilder.toString();
            //logger.error("[suricata]/[error]:"+error);
        } catch(Exception e){
            logger.error("[suricata]/[error]:"+e.getMessage());
        }
        return this.error;
    }

    public void collectProcess() throws IOException {
        this.starting = true;
        this.info = null;
        this.error = null;

        this.tryAgain = false;

        collectInputStreamProcess();
        collectErrorStreamProcess();

        if (this.tryAgain){
            this.starting = true;
            // try again
            logger.info("[suricata]/[try again]");
            this.initAndStartProcess();
        }

        this.starting = false;
    }

    private void deactivateIP(String ip, List<ProcessModel> processModelList){
        for(ProcessModel processModel: processModelList){
            if (("/"+ip).equals(processModel.getInterfaceName())){
                processModel.setActive(Boolean.FALSE);
            }
        }
    }

    private String getIP(String line){
        String marker = "failed to find a pcap device for IP";
        if (line!=null && line.contains(marker)){
            String ip = line.substring(line.indexOf(marker)+marker.length()).trim();
            return ip;
        }
        return null;
    }

    /** Normalize path to use OS separators so we don't get mixed C:/path\to\exe. */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return path;
        return Paths.get(path.replace("/", File.separator).replace("\\", File.separator)).normalize().toString();
    }

    protected List<String> getCommandAndArgs(){

        List<String> commandAndArgs = new ArrayList<>();

        String basePath = normalizePath(getToolModel().getPath());
        String exe = getToolModel().getExe() != null && !getToolModel().getExe().isEmpty() ? getToolModel().getExe() : "suricata";
        String command = basePath + File.separator + exe;
        commandAndArgs.add(command);
        commandAndArgs.add("-l");

        commandAndArgs.add(normalizePath(getToolModel().getProperties().get("log")));
        commandAndArgs.add("-c");
        commandAndArgs.add(normalizePath(getToolModel().getProperties().get("yaml")));

        /* multiple -i arguments, and capturing on multiple interfaces at once */
        String excludeIP = getToolModel().getProperties().get("excludeIP");

        for (ProcessModel processModel : processModelList) {
            if (processModel.getActive() && processModel.getEnabled()) {
                String interfaceName = processModel.getInterfaceName();
                if (interfaceName == null || interfaceName.trim().isEmpty()) continue;
                commandAndArgs.add("-i");
                commandAndArgs.add(interfaceName);
                logger.info("Command suricata + " + commandAndArgs);
            }
        }

        /* BPF filter: Suricata expects -F <file>, not -v + filter string (which caused "bpf compilation error: syntax error") */
        if (excludeIP != null && !excludeIP.isEmpty()) {
            String logDir = normalizePath(getToolModel().getProperties().get("log"));
            if (logDir != null && !logDir.isEmpty()) {
                try {
                    Path bpfFile = Paths.get(logDir, "capture-filter.bpf");
                    Files.createDirectories(bpfFile.getParent());
                    Files.write(bpfFile, excludeIP.getBytes(StandardCharsets.UTF_8));
                    commandAndArgs.add("-F");
                    commandAndArgs.add(bpfFile.toAbsolutePath().toString());
                } catch (IOException e) {
                    logger.warn("Could not write BPF filter file, running without filter: {}", e.getMessage());
                }
            }
        }

        return commandAndArgs;

    }

    @Override
    public ToolProcessStatusModel statusProcess() {
        String info = StringUtils.isEmpty(this.error) ?this.info:this.error;
        if (starting){
            info = "Starting...";
        }
        ProcessModel processModel = new ProcessModel();
        processModel.setPid(1L);
        return new ToolProcessStatusModel(
                getProcess()!=null?getProcess().isAlive():false,
                0,
                info,
                null,
                this.processModelList,
                this.starting);
    }
}
