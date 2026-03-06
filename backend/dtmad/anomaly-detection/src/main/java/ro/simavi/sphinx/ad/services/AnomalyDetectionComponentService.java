package ro.simavi.sphinx.ad.services;

import java.util.List;

public interface AnomalyDetectionComponentService<T> {

    void detect(String[] pcapMessage);

    List<T> getAlerts();

    void clearCache();
}
