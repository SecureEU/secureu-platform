package ro.simavi.sphinx.dtm.ws.tshark;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.handlers.KafkaListener;
import ro.simavi.sphinx.dtm.model.*;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkRealTimeService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.util.PackageFields;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/tshark/realtime")
@Profile("!agent")
public class TsharkRealtimeController {

    private static final Logger logger = LoggerFactory.getLogger(TsharkRealtimeController.class);

    private final DTMConfigProps dtmConfigProps;

    private final TsharkRealTimeService tsharkRealTimeService;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<SseEmitter> statusEmitters = new CopyOnWriteArrayList<>();

    private final String[] filter;

    private final ToolRemoteService toolRemoteService;

    private final InstanceService instanceService;

    private final KafkaListener kafkaListener;

    @Autowired
    public TsharkRealtimeController(DTMConfigProps dtmConfigProps,
                                    TsharkRealTimeService tsharkRealTimeService,
                                    ToolRemoteService toolRemoteService,
                                    InstanceService instanceService,
                                    @Qualifier("filterKafkaListenerContainerFactory") ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory
                                    ){

        this.tsharkRealTimeService = tsharkRealTimeService;
        this.dtmConfigProps = dtmConfigProps;
        this.toolRemoteService = toolRemoteService;
        this.instanceService = instanceService;
        this.kafkaListener = new KafkaListener(kafkaListenerContainerFactory);
        this.filter = new String[]{"frame.number","frame.time","frame.interface_description","frame.len","ip.src","ip.dst","udp.srcport","udp.dstport"};

    }

    @ApiIgnore
    @GetMapping(value = "/getFields")
    public List<String> getFields() {
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        String fields = toolModel.getProperties().get("fields");
        String[] fieldsArray = fields.split(" ");

        List<String> fieldsList = new ArrayList<>();
        for(int i = 0; i<fieldsArray.length-1; i++){
            String item = fieldsArray[i];
            if (item.equals("-e")){
                String field = fieldsArray[i+1];
                if (in(filter, field)) {
                    fieldsList.add(fieldsArray[i + 1]);
                }
            }
        }

        return fieldsList;
    }

    private boolean in(String[] filterList, String field){
        for(String filter:filterList){
            if (field.equals(filter)){
                return true;
            }
        }
        return false;
    }

    @ApiIgnore
    @GetMapping(path = "/sse/getData")
    public SseEmitter getData() {

        SseEmitter emitter = new SseEmitter();
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
        });

        return emitter;

    }

    @ApiIgnore
    @GetMapping(path = "/sse/getDataStatus")
    public SseEmitter getDataStatus() {

        SseEmitter emitter = new SseEmitter();
        this.statusEmitters.add(emitter);

        emitter.onCompletion(() -> this.statusEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.statusEmitters.remove(emitter);
        });

        return emitter;

    }

    //@EventListener
    public void onNotification(String[] line) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        this.emitters.forEach(emitter -> {
            try {
                String[] filteredLine = getFilteredLine(line);
                emitter.send(filteredLine);
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        this.emitters.remove(deadEmitters);
    }

    //@EventListener
    public void onNotification(RealtimeStatusModel status) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        this.statusEmitters.forEach(emitter -> {
            try {
                emitter.send(status);
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        this.statusEmitters.remove(deadEmitters);
    }

    private String[] getFilteredLine(String[] line) {
        String[] filteredLine = new String[filter.length];
        // "frame.number","frame.time","frame.interface_description","frame.len","ip.src","ip.dst","udp.srcport","udp.dstport"
        filteredLine[0] = line[PackageFields.FRAME_NUMBER];
        filteredLine[1] = line[PackageFields.FRAME_TIME];
        filteredLine[2] = line[PackageFields.FRAME_INTERFACE_DESCRIPTION];
        filteredLine[3] = line[PackageFields.FRAME_LEN];
        filteredLine[4] = line[PackageFields.IP_SRC];
        filteredLine[5] = line[PackageFields.IP_DST];
        filteredLine[6] = line[PackageFields.UDP_SCR_PORT];
        filteredLine[7] = line[PackageFields.UDP_DST_PORT];

        return filteredLine;
    }

    @ApiIgnore
    @GetMapping(value = "/stop/{instanceId}")
    public ResponseModel stopInstance(@PathVariable(name = "instanceId") Long instanceId) {
        toolRemoteService.executeAction(instanceId,"tshark/realtime/stop");

        InstanceModel instanceModel = instanceService.getById(instanceId);
        String rtTopic = "dtm-rt-" + instanceModel.getKey();
        String rtsTopic = "dtm-rts-" + instanceModel.getKey();

        Set<String> topics = new HashSet<>();
        topics.add(rtsTopic);
        topics.add(rtTopic);

        kafkaListener.deRegister(()->topics);

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @PostMapping("/start/{instanceId}")
    private ResponseModel startInstance(@PathVariable(name = "instanceId") Long instanceId, @RequestBody @Valid ProcessModel processModel,
                                BindingResult bindingResult, Model model, Principal principal) {


        InstanceModel instanceModel = instanceService.getById(instanceId);
        String rtTopic = "dtm-rt-" + instanceModel.getKey();
        String rtsTopic = "dtm-rts-" + instanceModel.getKey();

        Set<String> topics = new HashSet<>();
        topics.add(rtsTopic);
        topics.add(rtTopic);

      //  kafkaListener.deRegister(()->topics);

        kafkaListener.register(() -> topics, () -> (r) -> {

            ConsumerRecord record = (ConsumerRecord)r;

            if (record.topic().startsWith("dtm-rt-")){
                String message = (String)record.value();
                if (message!=null) {
                    String[] pcapMessage = message.split("\t", -1);
                    this.onNotification(pcapMessage);
                }
            }else if (record.topic().startsWith("dtm-rts-")){
                String status = (String)record.value();
                this.onNotification(new RealtimeStatusModel(status));
            }

        });

        return toolRemoteService.startRealtimeProcess(instanceId,processModel,DtmTools.TSHARK.getName());
    }

    @ApiIgnore
    @PostMapping(value = "/start")
    public ResponseModel start(@RequestBody @Valid ProcessModel processModel,
                               BindingResult bindingResult, Model model, Principal principal) {
        tsharkRealTimeService.start(processModel);
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/stop")
    public ResponseModel stop() {
        tsharkRealTimeService.stop();
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/touch")
    public ResponseModel touch() {
        tsharkRealTimeService.touch();
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/touch/{instanceId}")
    public ResponseModel touchInstance(@PathVariable(name = "instanceId") Long instanceId) {
        toolRemoteService.executeAction(instanceId,"tshark/realtime/touch");
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/process")
    public ProcessModel getProcess() {
        ProcessModel processModel = tsharkRealTimeService.getProcess();
        if (processModel==null){
            processModel = new ProcessModel();
            processModel.setActive(false);
        }else{
            processModel.setActive(true);
        }
        return processModel;
    }

    @ApiIgnore
    @GetMapping(value = "/process/{instanceId}")
    public ProcessModel getProcessInstance(@PathVariable(name = "instanceId") Long instanceId) {
        return toolRemoteService.getProcess(instanceId, DtmTools.TSHARK.getName());
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
