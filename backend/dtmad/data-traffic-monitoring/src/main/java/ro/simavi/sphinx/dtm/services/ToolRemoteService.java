package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.model.*;

import java.util.HashMap;
import java.util.List;

public interface ToolRemoteService {

    List<NetworkInterfaceModel> getInterfaces(InstanceModel instanceModel, String tool);

    List<ToolProcessStatusModel> getProcesses(InstanceModel instanceModel, String tool);

    ToolProcessStatusModel executeAction(InstanceModel instanceModel, String tool, String action, Long pid);

    Boolean executeAction(InstanceModel instanceModel, String tool, String action);

    void executeAction(Long instanceId, String method);

    Boolean isInstanceUp(InstanceModel instanceModel);

    Boolean clearConfigCache(InstanceModel instanceModel);

    Boolean clearCache(InstanceModel instanceModel, String controler);

    ResponseModel updateProcess(InstanceModel instanceModel, ProcessModel processModel, String tool);

    ResponseModel saveProcess(InstanceModel instanceModel, ProcessModel processModel, String tool);

    HashMap getMetric(InstanceModel tsharkInstanceModel, String metricName, String tool);

    ResponseModel startRealtimeProcess(Long instanceId, ProcessModel processModel, String tool);

    ProcessModel getProcess(Long instanceId, String name);
}
