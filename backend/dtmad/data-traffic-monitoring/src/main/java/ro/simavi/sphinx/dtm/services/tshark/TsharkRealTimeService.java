package ro.simavi.sphinx.dtm.services.tshark;

import ro.simavi.sphinx.dtm.model.ProcessModel;

public interface TsharkRealTimeService {

    void collect(String message);

    void start(ProcessModel processModel);

    void stop();

    ProcessModel getProcess();

    void touch();

    Long lastTouch();
}
