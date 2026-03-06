package ro.simavi.sphinx.dtm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.StatisticsDetailsModel;
import ro.simavi.sphinx.dtm.services.InstanceService;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final DTMConfigProps dtmConfigProps;

    private final InstanceService instanceService;

    public StatisticsController(DTMConfigProps dtmConfigProps, InstanceService instanceService){
        this.dtmConfigProps = dtmConfigProps;
        this.instanceService = instanceService;
    }


    @GetMapping(value = "/instance/statistics")
    public List<StatisticsDetailsModel> getAvailableInstanceStatistics(){
        List<StatisticsDetailsModel> statisticsDetailsModels = new ArrayList<>();

        if (this.isValidInstanceKey()) {
            String instanceKey = dtmConfigProps.getInstanceKey();
            InstanceModel instanceModel = instanceService.getByKey(instanceKey);
            if (instanceModel.getHasSuricata()){
                statisticsDetailsModels.add(new StatisticsDetailsModel("stats.suricata.dtmi","data traffic information", "suricata"));
            }
            if (instanceModel.getHasTshark()){
                statisticsDetailsModels.add(new StatisticsDetailsModel("stats.tshark.dtmpi","data traffic per protocols and interfaces", "tshark"));
            }
        }

        return statisticsDetailsModels;
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

    @GetMapping(value = "/statistics")
    public List<StatisticsDetailsModel> getInstanceStatistics(){
        List<StatisticsDetailsModel> statisticsDetailsModels = new ArrayList<>();

        if (this.isValidInstanceKey()) {
            String instanceKey = dtmConfigProps.getInstanceKey();
            InstanceModel instanceModel = instanceService.getByKey(instanceKey);
            if (instanceModel.getHasSuricata()){
                statisticsDetailsModels.add(new StatisticsDetailsModel("stats.suricata.dtmi","data traffic information", "suricata"));
            }
            if (instanceModel.getHasTshark()){
                statisticsDetailsModels.add(new StatisticsDetailsModel("stats.tshark.dtmip","data traffic per instances and protocols", "tshark"));
            }
        }

        return statisticsDetailsModels;
    }


}
