package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.model.event.MetricModel;

public interface StatisticsService {
    void collect(MetricModel metricModel);
}
