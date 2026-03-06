package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.dtm.services.StatisticsService;
import ro.simavi.sphinx.dtm.services.suricata.SuricataStatisticsService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkStatisticsService;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final TsharkStatisticsService tsharkStatisticsService;

    private final SuricataStatisticsService suricataStatisticsService;

    public StatisticsServiceImpl(
                        TsharkStatisticsService tsharkStatisticsService,
                        SuricataStatisticsService suricataStatisticsService){
        this.tsharkStatisticsService = tsharkStatisticsService;
        this.suricataStatisticsService = suricataStatisticsService;
    }


    @Override
    public void collect(MetricModel metricModel) {
        tsharkStatisticsService.collect(metricModel);
        suricataStatisticsService.collect(metricModel);
    }
}
