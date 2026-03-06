package ro.simavi.sphinx.dtm.services.impl.tshark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkRealTimeService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.util.NetworkHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TsharkRealTimeServiceImpl implements TsharkRealTimeService {

    private static final Logger logger = LoggerFactory.getLogger(TsharkRealTimeServiceImpl.class);

    private Process realtimeProcess = null;

    private DTMConfigProps dtmConfigProps;

    //public final ApplicationEventPublisher eventPublisher;

    public final MessagingSystemService messagingSystemService;

    private NetworkInterfaceService networkInterfaceService;

    private boolean stop = true;

    private ProcessModel processModel;

    private Long lastTouch = null;

    public TsharkRealTimeServiceImpl(//ApplicationEventPublisher eventPublisher,
                                     MessagingSystemService messagingSystemService,
                                     DTMConfigProps dtmConfigProps,
                                     @Qualifier("tsharkNetworkInterfaceService") NetworkInterfaceService networkInterfaceService){
       // this.eventPublisher = eventPublisher;
        this.messagingSystemService = messagingSystemService;
        this.dtmConfigProps = dtmConfigProps;
        this.networkInterfaceService = networkInterfaceService;
    }

    @Override
    public void collect(String message) {
        if (!this.stop) {
           // this.eventPublisher.publishEvent(pcapMessage);
            messagingSystemService.sendRealtimePackage(message);
        }
    }

    @Override
    public void start(ProcessModel processModel) {
        stop = false;
        touch();
        this.processModel = processModel;
        startProcess(processModel);
    }

    @Override
    public void stop() {
        stop = true;
        stopProcess();
    }

    @Override
    public ProcessModel getProcess() {
        if (stop){
            return null;
        }
        return processModel;
    }

    @Override
    public void touch() {
        lastTouch = new Date().getTime();
    }

    @Override
    public Long lastTouch() {
        return lastTouch;
    }

    private void startProcess(ProcessModel processModel){
        //this.eventPublisher.publishEvent(new RealtimeStatusModel("starting"));
        messagingSystemService.sendRealtimePackageStatus("starting");
        try {
            new Thread(){
                public void run(){
                    startProcess(DtmTools.TSHARK, processModel);
                }
            }.start();

        }catch (Exception e){
            logger.error("Error start tshark realtime" );
        }
    }

    private void startProcess( DtmTools dtmTools, ProcessModel processModel) {
        stopProcess();
        ProcessBuilder pb = new ProcessBuilder(getCommandAndArgs(dtmTools, processModel));
        try {
            realtimeProcess = pb.start();
           // this.eventPublisher.publishEvent(new RealtimeStatusModel("start"));
            messagingSystemService.sendRealtimePackageStatus("start");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(realtimeProcess.getInputStream()), 1)) {
                String line = null;
                while ((line = br.readLine()) != null) {
                    consume(line, processModel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
           // this.eventPublisher.publishEvent(new RealtimeStatusModel("stop"));
            messagingSystemService.sendRealtimePackageStatus("stop");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(realtimeProcess.getErrorStream()), 1)){
                String line = null;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String error = stringBuilder.toString();
                //this.eventPublisher.publishEvent(new RealtimeStatusModel(error));
                messagingSystemService.sendRealtimePackageStatus(error);
                logger.error(error);
            } catch(Exception e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consume(String message, ProcessModel processModel){
        ProcessFilterModel processFilterModel = processModel.getFilterModel();

        String filterCode = processFilterModel!=null?processFilterModel.getCode()!=null?processFilterModel.getCode():"NO_FILTER_CODE":"NO_FILTER_CODE";
        String fullMessage = message+"\t"+NetworkHelper.getUsername()+"\t"+NetworkHelper.getHostName()+"\t"+filterCode;

      //  String[] pcapMessage = fullMessage.split("\t",-1);

        collect(fullMessage);

    }

    protected List<String> getCommandAndArgs(DtmTools dtmTools, ProcessModel processModel){

        String toolName = dtmTools.getName();
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);

        String fields = toolModel.getProperties().get("fields");
        String interfaceName = networkInterfaceService.getNetworkInterfaceModel(processModel.getInterfaceName()).getName();
        ProcessFilterModel processFilterModel = processModel.getFilterModel();

        List<String> commandAndArgs = new ArrayList<>();

        String command = toolModel.getPath() + File.separator + "tshark";

        commandAndArgs.add(command);
        commandAndArgs.add("-l");
        commandAndArgs.add("-i");
        commandAndArgs.add(interfaceName);
        if (processFilterModel!=null){
            //commandAndArgs.add("-Y");
            commandAndArgs.add("-f");
            commandAndArgs.add(processFilterModel.getCommand());
        }
        if (fields!=null){
            commandAndArgs.addAll(Arrays.asList(fields.split(" ")));
        }
        return commandAndArgs;

    }

    private void stopProcess() {
       // this.eventPublisher.publishEvent(new RealtimeStatusModel("stopping"));
        messagingSystemService.sendRealtimePackageStatus("stopping");
        if (realtimeProcess!=null) {
            try {
                realtimeProcess.destroy();
                realtimeProcess.waitFor(2, TimeUnit.SECONDS);
                if(realtimeProcess.isAlive()) {
                    realtimeProcess.destroyForcibly();
                    realtimeProcess.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
      //  this.eventPublisher.publishEvent(new RealtimeStatusModel("stop"));
        messagingSystemService.sendRealtimePackageStatus("stop");
    }
}
