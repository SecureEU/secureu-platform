package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.model.event.EventModel;

public interface EventService {

    void collect(EventModel metricModel);

    void collect(String[] message);
}
