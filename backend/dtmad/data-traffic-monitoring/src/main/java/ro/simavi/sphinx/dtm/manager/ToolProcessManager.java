package ro.simavi.sphinx.dtm.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.dtm.manager.impl.SuricataOneProcessImpl;
import ro.simavi.sphinx.dtm.manager.impl.SuricataProcessImpl;
import ro.simavi.sphinx.dtm.manager.impl.TsharkOneProcessImpl;
import ro.simavi.sphinx.dtm.manager.impl.TsharkProcessImpl;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.NetworkInterfaceModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.*;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.dtm.util.PythonHelper;
import ro.simavi.sphinx.model.sm.SmModel;
import ro.simavi.sphinx.util.OperatingSystem;
import ro.simavi.sphinx.util.OperatingSystemHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ToolProcessManager implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ToolProcessManager.class);

    private final DTMConfigProps dtmConfigProps;

    private final NetworkInstanceManager networkInstanceManger;

    private final ProcessService progressService;

    private final InstanceService instanceService;

    private final ProcessFilterService processFilterService;

    private final ToolCollectorService toolCollectorService;

    private Map<Long, ToolProcessRunnable> toolProcessRunnableMap = new HashMap<>();

    /** One Logstash process per pipeline (Suricata and Nfstream); key = source tool name (suricata, nfstream). */
    private final Map<String, Process> logstashProcessBySource = new HashMap<>();

    /** Throttle repeated Logstash stdout lines per source to avoid log flood (e.g. "error:... null" loop). */
    private final Map<String, LogstashLineState> logstashLineState = new ConcurrentHashMap<>();

    private Process tsharkWithPcapProcess = null;

    private ToolModel toolModel;

    private SmModel smModel;

    public ToolProcessManager(DTMConfigProps dtmConfigProps,
                              NetworkInstanceManager networkInstanceManger,
                              ProcessService progressService,
                              InstanceService instanceService,
                              ProcessFilterService processFilterService,
                              ToolCollectorService toolCollectorService,
                              SmModel smModel){
        this.dtmConfigProps = dtmConfigProps;
        this.networkInstanceManger = networkInstanceManger;
        this.progressService = progressService;
        this.instanceService = instanceService;
        this.processFilterService = processFilterService;
        this.toolCollectorService = toolCollectorService;
        this.smModel = smModel;
    }

    public void tryToStart(){
        if (this.isValidInstanceKey()) {
            String instanceKey = dtmConfigProps.getInstanceKey();
            InstanceModel instanceModel = instanceService.getByKey(instanceKey);
            if (instanceModel.getHasSuricata()){
                // start logstash for suricata
                startLogstash(DtmTools.SURICATA);

                start(instanceModel, DtmTools.SURICATA);
            }

            if (instanceModel.getHasTshark()){
                start(instanceModel, DtmTools.TSHARK);
            }

            startNfstream();
        }

        toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        String pcapFile = toolModel.getProperties().get("pcap");
        if (pcapFile!=null && new File(pcapFile).exists()){
            logger.info("======START TSKARK WITH PCAP = " + pcapFile);
            startTsharkWithPcap(pcapFile);

        }
    }

    private void startNfstream(){
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.NFSTREAM.getName());

        PythonHelper pythonHelper = new PythonHelper();
        try {
            if (!StringUtils.isEmpty(toolModel.getProperties().get("source"))) {
                pythonHelper.start(toolModel);
            }else{
                String source = "eth0";
                pythonHelper.start(toolModel, source);
            }

            // start logstash for nfstream
            startLogstash(DtmTools.NFSTREAM);

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void startTsharkWithPcap(String pcapFile){
        try {
            new Thread(){
                public void run(){
                    startTsharkWithPcapProcess(tsharkWithPcapProcess, DtmTools.TSHARK, pcapFile);
                }
            }.start();

        }catch (Exception e){
            logger.error("Error start Tshark" + e);
        }
    }

    public void tryToStart(DtmTools dtmTools){
        if (this.isValidInstanceKey()) {
            String instanceKey = dtmConfigProps.getInstanceKey();
            InstanceModel instanceModel = instanceService.getByKey(instanceKey);
            if (instanceModel.getHasSuricata() && dtmTools==DtmTools.SURICATA){
                // start logstash
                startLogstash(DtmTools.SURICATA);
                start(instanceModel, DtmTools.SURICATA);
            }
            if (instanceModel.getHasTshark() && dtmTools==DtmTools.TSHARK){
                start(instanceModel, DtmTools.TSHARK);
            }

        }
    }

    private void startLogstash(DtmTools sourceDtmTool){
        ToolModel logstashModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.LOGSTASH.getName());
        if (logstashModel != null && logstashModel.getProperties() != null && "true".equalsIgnoreCase(logstashModel.getProperties().get("skipLocal"))) {
            logger.info("[Logstash-{}] skipped (dtm.tool.logstash.skipLocal=true; Logstash is run elsewhere, e.g. Docker)", sourceDtmTool.getName());
            return;
        }
        try {
            new Thread(){
                public void run(){
                    startProcess(null, DtmTools.LOGSTASH, sourceDtmTool);
                }
            }.start();

        }catch (Exception e){
            logger.error("Error start Logstash for " + sourceDtmTool.getName() );
        }
    }

    private boolean isValidInstanceKey(){
        String instanceKey = dtmConfigProps.getInstanceKey();
        if (instanceKey!=null && instanceService.isStartable(instanceKey)) {
            return true;
        }else{
            logger.error("INVALID KEY:" + instanceKey +" or INSTANCE is not enabled!" );
            return false;
        }
    }

    private List<NetworkInterfaceModel> getNetworkInterfaceModel(DtmTools dtmTools){
        List<NetworkInterfaceModel> interfaceList = new ArrayList<>();
        try {
            NetworkInterfaceService networkInterfaceService = networkInstanceManger.getNetworkInterfaceService(dtmTools);
            if (networkInterfaceService==null){
                logger.error("INVALID NetworkInterfaceService CONFIGURATION for " + dtmTools.getName());
                return interfaceList;
            }
            interfaceList = networkInterfaceService.getNetworkInterfaceModelList();
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return interfaceList;
    }
    private void start(InstanceModel instanceModel, DtmTools dtmTools){

        logger.info("====>>> start process: "+dtmTools.getName());
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(dtmTools.getName());
        if (toolModel==null){
            logger.error("INVALID CONFIGURATION for " + dtmTools.getName());
            return;
        }

        List<NetworkInterfaceModel> interfaceList = getNetworkInterfaceModel(dtmTools);

        progressService.inactivateAllInterfaces(instanceModel, dtmTools.getProcessType());

        String instanceKey = dtmConfigProps.getInstanceKey();

        String source = toolModel.getProperties().get("source");
        String[] sourceList = source.split(",");
        boolean allSources = true;
        if (sourceList!=null && sourceList.length>0){
            allSources = sourceList[0].equalsIgnoreCase("all");
        }

        for(NetworkInterfaceModel interfaceName:interfaceList) {
            String name = interfaceName.getName();
            if (!accept(allSources, sourceList, name)){
                logger.info("==== IGNORE to start process for interface name:["+dtmTools.getName()+"]"+name);
                continue;
            }
            logger.info("==== try to start process for interface name:["+dtmTools.getName()+"]"+name);

            Iterable<ProcessEntity> processEntities = null;

            if (dtmTools.getProcessType()==ProcessType.SURICATA){

               String networkInterfaceDiscoveyAlgorithm = toolModel.getProperties().get("networkInterfaceDiscoveyAlgorithm");
               if (networkInterfaceDiscoveyAlgorithm.equalsIgnoreCase("tshark")){
                   processEntities = progressService.findByInstanceKeyName(instanceKey, name, dtmTools.getProcessType());
               }else { // java
                   processEntities = progressService.findByInstanceKeyFullName(instanceKey, interfaceName.getFullName(), dtmTools.getProcessType());
               }

            }else {
               processEntities = progressService.findByInstanceKeyName(instanceKey, name, dtmTools.getProcessType());
            }

            int count = 0;
            for(ProcessEntity processEntity: processEntities){
                count++;
                processEntity.setActive(Boolean.TRUE);
                processEntity.setInterfaceName(name);
                progressService.save(processEntity);
            }

            if (count==0){
                ProcessEntity processEntity = new ProcessEntity();
                processEntity.setInterfaceName(name);
                processEntity.setInterfaceDisplayName(interfaceName.getDisplayName());
                processEntity.setInterfaceFullName(interfaceName.getFullName());
                processEntity.setDescription(interfaceName.getFullName());
                processEntity.setActive(Boolean.TRUE);
                processEntity.setEnabled(Boolean.TRUE);
                processEntity.setProcessType(dtmTools.getProcessType());
                processEntity.setInstance(instanceService.findById(instanceModel.getId()));
                progressService.save(processEntity);
            }

        }

        // create process
        List<ProcessEntity> processEntities = progressService.findAll(instanceModel, dtmTools.getProcessType());
        createProcess(processEntities,dtmTools);
    }

    private boolean accept(boolean allSources, String[] sourceList, String interfaceName){
        if (allSources){
            return true;
        }
        for(String source: sourceList){
            if (source.equals(interfaceName)){
                return true;
            }
        }
        return false;
    }

    private void createProcess(List<ProcessEntity> processEntities,DtmTools dtmTools){
        if (dtmTools.isOneProcess()){
            addProcess(processEntities, dtmTools);
        }else {
            for (ProcessEntity processEntity : processEntities) {
                addProcess(processEntity, dtmTools);
            }
        }
    }

    public void addProcess(ProcessEntity processEntity, DtmTools dtmTools){
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(dtmTools.getName());

        addProcess(toolModel, toProcessModel(processEntity), dtmTools);
    }

    public void addProcess(List<ProcessEntity> processEntities, DtmTools dtmTools){
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(dtmTools.getName());

        addProcess(toolModel, toProcessModelList(processEntities), dtmTools);
    }

    private List<ProcessModel> toProcessModelList(List<ProcessEntity> processEntities) {
        List<ProcessModel> processModels = new ArrayList<>();
        for(ProcessEntity processEntity: processEntities){
            processModels.add(toProcessModel(processEntity));
        }
        return processModels;
    }

    private ProcessModel toProcessModel(ProcessEntity processEntity){
        ProcessModel processModel = new ProcessModel();
        processModel.setPid(processEntity.getId());
        processModel.setActive(processEntity.getActive());
        processModel.setEnabled(processEntity.getEnabled());
        processModel.setInterfaceName(processEntity.getInterfaceName());
        processModel.setInterfaceDisplayName(processEntity.getInterfaceDisplayName());
        processModel.setInterfaceFullName(processEntity.getInterfaceFullName());
        processModel.setFilterModel(processFilterService.toProcessFilterModel( processEntity.getFilter()));
        processModel.setFilterName(processModel.getFilterModel()!=null?processModel.getFilterModel().getName():null);
        return processModel;
    }

    private void addProcess(ToolModel toolModel, ProcessModel processModel, DtmTools dtmTools) {
        ToolProcessRunnable toolProcessRunnable = getToolProcessRunnable(toolModel, processModel, dtmTools);
        if (toolProcessRunnable==null){
            logger.error("INVALID ToolProcessRunnable CONFIGURATION for " + dtmTools.getName());
            return;
        }
        try {
            if (processModel.getActive() && processModel.getEnabled()) {
                toolProcessRunnable.startProcess(processModel.getFilterModel());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        toolProcessRunnableMap.put(processModel.getPid(), toolProcessRunnable);
    }

    private boolean hasProcessesEnabled(List<ProcessModel> processModelList){
        for(ProcessModel processModel: processModelList){
            if ( processModel.getActive() && processModel.getEnabled()){
                return true;
            }
        }
        return false;
    }

    private void addProcess(ToolModel toolModel, List<ProcessModel> processModelList, DtmTools dtmTools) {
        ToolProcessRunnable toolProcessRunnable = getToolProcessRunnable(toolModel, processModelList, dtmTools);
        if (toolProcessRunnable==null){
            logger.error("INVALID ToolProcessRunnable CONFIGURATION for " + dtmTools.getName());
            return;
        }
        try {
            if (hasProcessesEnabled(processModelList)) {
                toolProcessRunnable.startProcess();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        toolProcessRunnableMap.put(dtmTools.getOneProcessPid(), toolProcessRunnable);
    }

    private ToolProcessRunnable getToolProcessRunnable(ToolModel toolModel, List<ProcessModel> processModel, DtmTools dtmTools){
        if (dtmTools.getProcessType()==ProcessType.TSHARK) {
            return new TsharkOneProcessImpl(this.toolCollectorService, toolModel, processModel);
        }
        if(dtmTools.getProcessType()==ProcessType.SURICATA){
            return new SuricataOneProcessImpl(this.toolCollectorService, toolModel, processModel);
        }
        return null;
    }

    private ToolProcessRunnable getToolProcessRunnable(ToolModel toolModel, ProcessModel processModel, DtmTools dtmTools){
        if (dtmTools.getProcessType()==ProcessType.TSHARK) {
            return new TsharkProcessImpl(this.toolCollectorService, toolModel, processModel);
        }
        if(dtmTools.getProcessType()==ProcessType.SURICATA){
            return new SuricataProcessImpl(this.toolCollectorService, toolModel, processModel);
        }
        return null;
    }

    public ToolProcessRunnable getProcess(Long pid){
        return toolProcessRunnableMap.get(pid);
    }

    public Map<Long, ToolProcessRunnable> getProcesses(DtmTools dtmTools){
        Map<Long, ToolProcessRunnable> newProcessList = new HashMap<>();
        toolProcessRunnableMap.forEach(
                (pid,toolProcessRunnable) -> {
                    if (toolProcessRunnable.getToolModel()!=null && toolProcessRunnable.getToolModel().getName().equals(dtmTools.getName())) {
                       newProcessList.put(pid, toolProcessRunnable);
                    }
                }
        );
        return newProcessList;
    }

    @Override
    public void destroy() throws Exception {

        // stop all Logstash pipelines
        synchronized (logstashProcessBySource) {
            logstashProcessBySource.values().forEach(this::stopProcess);
            logstashProcessBySource.clear();
        }

        /*
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        String path = toolModel.getPath();

        if (path.startsWith("docker")) {
            Set<Long> pids = getProcesses().keySet();

            for (Long pid : pids) {
                List<String> commandAndArgs = new ArrayList<>();
                String command = "docker rm -f sphinx-tshark_" + pid;
                commandAndArgs.addAll(Arrays.asList(command.split(" ")));

                ProcessBuilder pb = new ProcessBuilder(commandAndArgs);
                pb.start();
            }
        }
        */
    }

    public void stopAllProcesses(){
        this.toolProcessRunnableMap.forEach(
                (pid,toolProcessRunnable) -> {
                    toolProcessRunnable.stopProcess();
                }
        );
    }

    private void startProcess(Process logstashProcess, DtmTools dtmTools, DtmTools sourceDtmTools) {
        String sourceKey = sourceDtmTools.getName();
        synchronized (logstashProcessBySource) {
            Process existing = logstashProcessBySource.get(sourceKey);
            if (existing != null) {
                stopProcess(existing);
                logstashProcessBySource.remove(sourceKey);
            }
        }

        String instanceKey = dtmConfigProps.getInstanceKey();
        String username = NetworkHelper.getUsername();
        String hostname = NetworkHelper.getHostName();

        List<String> commandAndArgs = getCommandAndArgs(dtmTools, sourceDtmTools);

        logger.info("[Logstash-{}] startProcess / commandAndArgs={}", sourceKey, Arrays.toString(commandAndArgs.toArray()));

        ProcessBuilder pb = new ProcessBuilder(commandAndArgs);
        // Use PIPE and drain streams in background so "error:... null" never reaches the console.
        // (Redirect to NUL does not work when logstash.bat spawns the JVM on Windows.)
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        Map<String, String> env = pb.environment();
        env.put("SPHINX_DTM_INSTANCE_KEY", instanceKey);
        env.put("SPHINX_DTM_USERNAME", username);
        env.put("SPHINX_DTM_HOSTNAME", hostname);
        String oauthClient = smModel.getOauthClientSecret();
        if (oauthClient != null) {
            env.put("OAUTH_CLIENT_SECRET", oauthClient);
            env.put("oauth.client.secret", oauthClient);
        }

        try {
            Process process = pb.start();
            drainStreamInBackground(process.getInputStream());
            drainStreamInBackground(process.getErrorStream());
            synchronized (logstashProcessBySource) {
                logstashProcessBySource.put(sourceKey, process);
            }
            logger.info("[Logstash-{}] process started (output discarded; check --path.logs for Logstash logs)", sourceKey);
        } catch (IOException e) {
            logger.error("[Logstash-{}] failed to start: {}", sourceKey, e.getMessage());
        }
    }

    /** Drain an input stream in a daemon thread and discard all bytes (no logging). Prevents process output from flooding the console. */
    private static void drainStreamInBackground(final InputStream stream) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buf = new byte[4096];
                try {
                    while (stream.read(buf) >= 0) {
                        // discard
                    }
                } catch (IOException ignored) {
                } finally {
                    try {
                        stream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }, "logstash-drain");
        t.setDaemon(true);
        t.start();
    }

    /** Logstash "error:<timestamp> null" lines – match and suppress; never log the line content to avoid flood. */
    private static final java.util.regex.Pattern LOGSTASH_ERROR_NULL_PATTERN =
            java.util.regex.Pattern.compile("^error:.+\\s+null\\s*$");

    /** Log a Logstash stdout line, throttling repeated identical or same-pattern lines to avoid log flood. */
    private void logLogstashLineThrottled(String sourceKey, String line) {
        if (LOGSTASH_ERROR_NULL_PATTERN.matcher(line).matches()) {
            LogstashLineState state = logstashLineState.computeIfAbsent(sourceKey, k -> new LogstashLineState());
            synchronized (state) {
                if ("error:...null".equals(state.lastLine)) {
                    state.repeatCount++;
                    if (state.repeatCount % 500 == 0) {
                        logger.debug("[logstash]: 'error:... null' repeated {} times from {} (suppressed)", state.repeatCount, sourceKey);
                    }
                    return;
                }
                state.lastLine = "error:...null";
                state.repeatCount = 1;
                logger.warn("[logstash]: {} is emitting 'error:... null' lines (content suppressed). Check Logstash config.", sourceKey);
            }
            return;
        }
        String keyForRepeat = line;
        LogstashLineState state = logstashLineState.computeIfAbsent(sourceKey, k -> new LogstashLineState());
        synchronized (state) {
            if (keyForRepeat.equals(state.lastLine)) {
                state.repeatCount++;
                if (state.repeatCount <= 2) {
                    logger.info("[logstash]:{}", line);
                } else if (state.repeatCount == 3) {
                    logger.info("[logstash]: (repeating, further duplicates throttled)");
                } else if (state.repeatCount % 100 == 0) {
                    logger.info("[logstash]: (same line repeated {} times)", state.repeatCount);
                }
                return;
            }
            if (state.repeatCount > 2 && state.lastLine != null && !"error:...null".equals(state.lastLine)) {
                logger.info("[logstash]: (previous line repeated {} times)", state.repeatCount);
            }
            state.lastLine = keyForRepeat;
            state.repeatCount = 1;
            logger.info("[logstash]:{}", line);
        }
    }

    private  List<String>  getCommandAndArgs(DtmTools dtmTools, DtmTools sourceDtmTools) {
        OperatingSystem operatingSystem  = OperatingSystemHelper.getOperatingSystem();


        // cmd /c  C:\tools\logstash\bin\logstash -f C:\tools\logstash\config\logstash.conf
        String toolName = dtmTools.getName();
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);

        List<String> commandAndArgs = new ArrayList<>();

        String command = toolModel.getPath() + File.separator + "bin" + File.separator + toolModel.getExe();
        // On Windows prefer logstash.exe over logstash.bat so the child inherits our PIPE (avoids "error:... null" on console).
        if (DtmTools.LOGSTASH.getName().equals(toolName)) {
            String exeAlt = toolModel.getPath() + File.separator + "bin" + File.separator + "logstash.exe";
            if (new File(exeAlt).exists()) command = exeAlt;
            else if (!new File(command).exists() && new File(exeAlt).exists()) command = exeAlt;
        }

        commandAndArgs.add(command);
        commandAndArgs.add("-f");

        String fileConf = toolModel.getProperties().get(sourceDtmTools.getName().concat("Conf"));

        commandAndArgs.add(toolModel.getPath() + File.separator + "config" + File.separator + fileConf);

        commandAndArgs.add("--path.data");
        commandAndArgs.add(toolModel.getPath() + File.separator + "data-" + sourceDtmTools.getName());
        commandAndArgs.add("--path.logs");
        commandAndArgs.add(toolModel.getPath() + File.separator + "logs-" + sourceDtmTools.getName());

        return commandAndArgs;
    }

//    private List<String> getCommandAndArgs(DtmTools dtmTools, DtmTools sourceDtmTools) {
//        boolean useDocker = true; // sau ia din config (application.properties)
//
//        List<String> commandAndArgs = new ArrayList<>();
//
//        if (useDocker) {
//            // doar logurile din container
//            commandAndArgs.add("docker");
//            commandAndArgs.add("logs");
//            commandAndArgs.add("-f");
//            commandAndArgs.add("logstash"); // numele containerului
//        } else {
//            // varianta locală (logstash.exe)
//            String toolName = dtmTools.getName();
//            ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);
//
//            String command = toolModel.getPath() + File.separator + "bin" + File.separator + toolModel.getExe();
//
//            commandAndArgs.add(command);
//            commandAndArgs.add("-f");
//
//            String fileConf = toolModel.getProperties().get(sourceDtmTools.getName().concat("Conf"));
//
//            commandAndArgs.add(toolModel.getPath() + File.separator + "config" + File.separator + fileConf);
//
//            commandAndArgs.add("--path.data");
//            commandAndArgs.add(toolModel.getPath() + File.separator + "data-" + sourceDtmTools.getName());
//        }
//
//        return commandAndArgs;
//    }


    public void stopProcess(Process process) {
        if (process!=null) {
            try {
                process.destroy();
                process.waitFor(2, TimeUnit.SECONDS);
                if(process.isAlive()) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startTsharkWithPcapProcess(Process tsharkWithPcapProcess, DtmTools dtmTools, String pcapFile) {
        stopProcess(tsharkWithPcapProcess);

        ProcessBuilder pb = new ProcessBuilder(getCommandAndArgsForTsharkWithPcap(pcapFile));
        try {
            tsharkWithPcapProcess = pb.start();

            StringBuilder inputStreamStringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(tsharkWithPcapProcess.getInputStream()), 1);
            String line = null;
            int i = 0;
            while ((line = br.readLine()) != null) {
                consume(line, pcapFile);
                i++;
            }

//            StringBuilder errorStreamStringBuilder = new StringBuilder();
//            BufferedReader brError = new BufferedReader(new InputStreamReader(tsharkWithPcapProcess.getErrorStream()), 1);
//            String lineError = null;
//            while ((lineError = brError.readLine()) != null) {
//                errorStreamStringBuilder.append(lineError);
//                System.out.println("LInieeeeeeeeeeee"+ lineError.toString());
//            }

            String info = inputStreamStringBuilder.toString();
            if (info!=null && info.contains("[FATAL]")){
                stopProcess(tsharkWithPcapProcess);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    private  List<String>  getCommandAndArgsForTsharkWithPcap(String pcapFile) {
        OperatingSystem operatingSystem  = OperatingSystemHelper.getOperatingSystem();
        String fields = toolModel.getProperties().get("fields");
        String command = toolModel.getPath() + File.separator + toolModel.getExe();

        List<String> commandAndArgs = new ArrayList<>();

        if (operatingSystem.equals(OperatingSystem.WINDOWS)) {
            commandAndArgs.add("cmd");
            commandAndArgs.add("/c");
        }else{
            commandAndArgs.add("bash"); // "/bin/sh" /*todo*/ "bash", "-c" / or /bin/bash
            commandAndArgs.add("-c");
        }

        commandAndArgs.add(command);
        commandAndArgs.add("-r");
        commandAndArgs.add(pcapFile);
        commandAndArgs.addAll(Arrays.asList(fields.split(" ")));

        logger.info("getCommandAndArgsForTsharkWithPcap="+Arrays.toString(commandAndArgs.toArray()));
        return commandAndArgs;
    }

    private void consume(String message, String pcapFile){

        //System.out.println(message);
        String filterCode = "NO_FILTER_CODE";
        String fullMessage = message+"\t"+NetworkHelper.getUsername()+"\t"+NetworkHelper.getHostName()+"\t"+filterCode+"\t"+"1\t"+pcapFile; // from pcap
        //logger.info("pcap="+fullMessage);
        this.toolCollectorService.collector(fullMessage,true);

    }

    private static class LogstashLineState {
        String lastLine;
        int repeatCount;
    }

}
