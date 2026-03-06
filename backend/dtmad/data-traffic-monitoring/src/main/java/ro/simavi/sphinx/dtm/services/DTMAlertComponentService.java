package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.model.event.EventModel;

public interface DTMAlertComponentService {

    void detect(String[] message);

    void detect(EventModel eventModel);

}
