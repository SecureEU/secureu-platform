package ro.simavi.sphinx.dtm.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.dtm.entities.DtmCmdEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.DtmCmdRepository;
import ro.simavi.sphinx.dtm.model.*;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.ToolRemoteService;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.model.event.DtmCmdEventModel;

import java.util.*;

@Service
public class ToolRemoteServiceImpl implements ToolRemoteService {

    private final InstanceService instanceService;

    private final RestTemplate restTemplate;

    private final Environment environment;

    private final DtmCmdRepository dtmCmdRepository;

    private final MessagingSystemService messagingSystemService;

    private static final Logger logger = LoggerFactory.getLogger(ToolRemoteServiceImpl.class);

    public ToolRemoteServiceImpl(InstanceService instanceService, RestTemplate restTemplate, Environment environment, DtmCmdRepository dtmCmdRepository,
                                 MessagingSystemService messagingSystemService){
        this.instanceService = instanceService;
        this.restTemplate = restTemplate;
        this.environment = environment;
        this.dtmCmdRepository = dtmCmdRepository;
        this.messagingSystemService = messagingSystemService;
    }

    private String getUrl(InstanceModel instanceModel, String method){
        String baseUrl = instanceModel.getUrl();
        String contextPath  = environment.getProperty("server.servlet.context-path");
        String url = baseUrl + contextPath + "/" + method;
        return url;
    }

    private DtmCmdEntity getDtmCmdEntity(String url, String method, String instanceKey){
        DtmCmdEntity dtmCmdEntity = new DtmCmdEntity();
        dtmCmdEntity.setUrl(url);
        dtmCmdEntity.setMethod(method);
        dtmCmdEntity.setInstanceKey(instanceKey);

        dtmCmdEntity.setHostname(NetworkHelper.getHostName());
        dtmCmdEntity.setUsername(NetworkHelper.getUsername());

        return dtmCmdEntity;
    }

    private DtmCmdEventModel getDtmCmdEventModel(DtmCmdEntity dtmCmdEntity){

        UUID uuid = UUID.randomUUID();

        DtmCmdEventModel dtmCmdEventModel = new DtmCmdEventModel();
        dtmCmdEventModel.setUuid(uuid.toString());
        dtmCmdEventModel.setUrl(dtmCmdEntity.getUrl());
        dtmCmdEventModel.setMethod(dtmCmdEntity.getMethod());
        dtmCmdEventModel.setInstanceKey(dtmCmdEntity.getInstanceKey());
        dtmCmdEventModel.setCreatedDate(dtmCmdEntity.getCreatedDate());
        dtmCmdEventModel.setHostname(dtmCmdEntity.getHostname());
        dtmCmdEventModel.setUsername(dtmCmdEntity.getUsername());

        return dtmCmdEventModel;

    }

    private void saveDtmCmd(String url, String method, InstanceModel instanceModel){
        DtmCmdEntity dtmCmdEntity = dtmCmdRepository.save(getDtmCmdEntity(url, method, instanceModel.getKey()));
        DtmCmdEventModel dtmCmdEventModel = getDtmCmdEventModel(dtmCmdEntity);
        messagingSystemService.sendDtmCmd(dtmCmdEventModel);
    }

    private HttpHeaders getHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    @Override
    public List<NetworkInterfaceModel> getInterfaces(InstanceModel instanceModel, String tool) {
        String url = getUrl(instanceModel, "/"+tool+"/getInterfaces");

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<List<NetworkInterfaceModel>> parameterizedTypeReference = new ParameterizedTypeReference<List<NetworkInterfaceModel>>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<List<NetworkInterfaceModel>> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<ToolProcessStatusModel> getProcesses(InstanceModel instanceModel, String tool) {
        String url = getUrl(instanceModel, "/"+tool+"/getProcesses");

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<List<ToolProcessStatusModel>> parameterizedTypeReference = new ParameterizedTypeReference<List<ToolProcessStatusModel>>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<List<ToolProcessStatusModel>> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public ToolProcessStatusModel executeAction(InstanceModel instanceModel, String tool, String action, Long pid) {

        String url = getUrl(instanceModel, tool+"/"+action + "/" + pid);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<ToolProcessStatusModel> parameterizedTypeReference = new ParameterizedTypeReference<ToolProcessStatusModel>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<ToolProcessStatusModel> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return null;
        }
    }

    @Override
    public Boolean executeAction(InstanceModel instanceModel, String tool, String action) {

        String url = getUrl(instanceModel, tool+"/"+action);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<Boolean> parameterizedTypeReference = new ParameterizedTypeReference<Boolean>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return null;
        }

    }

    @Override
    public void executeAction(Long instanceId, String method){
        InstanceModel instanceModel = instanceService.getById(instanceId);
        String url = getUrl(instanceModel, method);

        HttpEntity <String> entity = new HttpEntity<>(getHeaders());
        try {
            saveDtmCmd(url, "GET", instanceModel);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    private Boolean execute(String url, InstanceModel instanceModel){
        HttpEntity <String> entity = new HttpEntity<>(getHeaders());

        try {
            saveDtmCmd(url, "GET", instanceModel);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return true;
        }catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }
    }


    @Override
    public Boolean isInstanceUp(InstanceModel instanceModel) {
        String url = getUrl(instanceModel, "/instance/up");
        return execute(url, instanceModel);
    }

    @Override
    public Boolean clearConfigCache(InstanceModel instanceModel) {
        String url = getUrl(instanceModel, "/instance/clearConfigCache");
        return execute(url, instanceModel);
    }

    @Override
    public Boolean clearCache(InstanceModel instanceModel, String controler) {
        String url = getUrl(instanceModel, "/"+controler+"/clearCache");
        return execute(url, instanceModel);
    }

    @Override
    public ResponseModel updateProcess(InstanceModel instanceModel, ProcessModel processModel, String tool) {
        String url = getUrl(instanceModel, "/"+tool+"/process/update");
        return executeAction(instanceModel, processModel, url);
    }

    @Override
    public ResponseModel saveProcess(InstanceModel instanceModel, ProcessModel processModel, String tool) {
        String url = getUrl(instanceModel, "/"+tool+"/process/save");
        return executeAction(instanceModel, processModel, url);
    }

    private ResponseModel executeAction(InstanceModel instanceModel, ProcessModel tsharkProcessModel, String url) {

        HttpEntity<ProcessModel> entity = new HttpEntity<>(tsharkProcessModel,getHeaders());

        ParameterizedTypeReference<ResponseModel> parameterizedTypeReference = new ParameterizedTypeReference<ResponseModel>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<ResponseModel> response = restTemplate.exchange(url, HttpMethod.POST, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return new ResponseModel(e.getMessage(), Boolean.FALSE);
        }
    }

    @Override
    public HashMap getMetric(InstanceModel instanceModel, String metricName, String tool) {
        String url = getUrl(instanceModel, "/"+tool+"/statistics/"+metricName);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<HashMap> parameterizedTypeReference = new ParameterizedTypeReference<HashMap>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<HashMap> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return new HashMap();
        }
    }

    @Override
    public ResponseModel startRealtimeProcess(Long instanceId, ProcessModel processModel, String tool) {
        InstanceModel instanceModel = instanceService.getById(instanceId);
        String url = getUrl(instanceModel, "/tshark/realtime/start");
        return executeAction(instanceModel, processModel, url);
    }

    @Override
    public ProcessModel getProcess(Long instanceId, String name) {
        InstanceModel instanceModel = instanceService.getById(instanceId);
        String url = getUrl(instanceModel, "/tshark/realtime/process");

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());

        ParameterizedTypeReference<ProcessModel> parameterizedTypeReference = new ParameterizedTypeReference<ProcessModel>(){};

        try {
            saveDtmCmd(url, "GET", instanceModel);
            HttpEntity<ProcessModel> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
            return response.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return new ProcessModel();
        }

    }
}
