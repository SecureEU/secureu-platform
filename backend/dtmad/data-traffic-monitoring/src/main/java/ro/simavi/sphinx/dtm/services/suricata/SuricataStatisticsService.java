package ro.simavi.sphinx.dtm.services.suricata;

import ro.simavi.sphinx.dtm.model.event.MetricModel;

import java.util.HashMap;

public interface SuricataStatisticsService {

    void collect(MetricModel metricModel);

    HashMap<String,String> getDecoderStatistics();

    HashMap<String, HashMap<String, String>>  getDecoderPerInstanceStatistics();
}
