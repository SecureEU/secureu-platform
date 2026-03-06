package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.model.enums.ConfigCode;
import ro.simavi.sphinx.dtm.services.AssetCatalogueService;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.dtm.services.EventService;
import ro.simavi.sphinx.dtm.services.alert.BlackWebAlertService;
import ro.simavi.sphinx.dtm.services.alert.MassiveDataProcessingAlertService;
import ro.simavi.sphinx.dtm.services.alert.PortCatalogueAlertService;
import ro.simavi.sphinx.dtm.services.alert.TcpAnalysisFlagsAlertService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkRealTimeService;
import ro.simavi.sphinx.model.ConfigModel;
import ro.simavi.sphinx.model.event.EventModel;

@Service
public class EventServiceImpl implements EventService {

    private final AssetCatalogueService assetCatalogueComponentService;

    private final TcpAnalysisFlagsAlertService tcpAnalysisFlagsAlertService;

    private final MassiveDataProcessingAlertService massiveDataProcessingAlertService;

    private final PortCatalogueAlertService portCatalogueAlertService;

    private final BlackWebAlertService blackWebAlertService;

    private final DtmConfigService configService;

    private final TsharkRealTimeService tsharkRealTimeService;

    public EventServiceImpl(AssetCatalogueService assetCatalogueComponentService,
                            TcpAnalysisFlagsAlertService tcpAnalysisFlagsAlertService,
                            MassiveDataProcessingAlertService massiveDataProcessingAlertService,
                            PortCatalogueAlertService portCatalogueAlertService,
                            BlackWebAlertService blackWebAlertService,
                            DtmConfigService configService,
                            TsharkRealTimeService tsharkRealTimeService){
        this.assetCatalogueComponentService = assetCatalogueComponentService;
        this.tcpAnalysisFlagsAlertService = tcpAnalysisFlagsAlertService;
        this.massiveDataProcessingAlertService = massiveDataProcessingAlertService;
        this.portCatalogueAlertService = portCatalogueAlertService;
        this.blackWebAlertService = blackWebAlertService;
        this.configService = configService;
        this.tsharkRealTimeService = tsharkRealTimeService;
    }

    // suricata
    @Override
    public void collect(EventModel eventModel) {
        //assetCatalogueComponentService.collect(eventModel);

        ConfigModel portConfigModel = configService.getValue(ConfigCode.PORT_DISCOVERY_ALERT_ENABLE);
        if (portConfigModel!=null && portConfigModel.getValue()!=null && portConfigModel.getValue().equals("true")){
            portCatalogueAlertService.detect(eventModel);
        }

    }

    // tshark
    @Override
    public void collect(String[] message) {
        assetCatalogueComponentService.collect(message);
       // tsharkRealTimeService.collect(message);
        /*
        ConfigModel tcpConfigModel = configService.getValue(ConfigCode.TCP_ANALYSIS_FLAG_ALERT_ENABLE);
        if (tcpConfigModel.getValue()!=null && tcpConfigModel.getValue().equals("true")){
            tcpAnalysisFlagsAlertService.detect(message);
        }
        */

        ConfigModel mdpConfigModel = configService.getValue(ConfigCode.MASSIVE_DATA_PROCESSING_ALERT_ENABLE);
        if (mdpConfigModel.getValue()!=null && mdpConfigModel.getValue().equals("true")){
            massiveDataProcessingAlertService.detect(message);
        }
        /*
        ConfigModel portConfigModel = configService.getValue(ConfigCode.PORT_DISCOVERY_ALERT_ENABLE);
        if (portConfigModel.getValue()!=null && portConfigModel.getValue().equals("true")){
            portCatalogueAlertService.detect(message);
        }
        */

        ConfigModel blackwebConfigModel = configService.getValue(ConfigCode.BLACKWEB_ALERT_ENABLE);
        if (blackwebConfigModel.getValue()!=null && blackwebConfigModel.getValue().equals("true")){
            blackWebAlertService.detect(message);
        }

    }

}
