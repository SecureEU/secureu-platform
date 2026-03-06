package ro.simavi.sphinx.ad.handlers;

import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.services.AnomalyDetectionService;
import ro.simavi.sphinx.ad.services.StatisticsService;

@Component
public class NetworkHandler {

    private final StatisticsService statisticsService;

    private final AnomalyDetectionService anomalyDetectionService;

    public NetworkHandler(StatisticsService statisticsService, AnomalyDetectionService anomalyDetectionService){
        this.statisticsService = statisticsService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    public void receiveMessage(String message) {
        String[] pcapMessage = message.split("\t",-1);
        anomalyDetectionService.detect(pcapMessage);
    }
}
