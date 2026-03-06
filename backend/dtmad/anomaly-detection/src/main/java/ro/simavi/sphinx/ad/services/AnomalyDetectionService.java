package ro.simavi.sphinx.ad.services;

import ro.simavi.sphinx.ad.entities.AdComponentEntity;

public interface AnomalyDetectionService {

    void detect(String[] pcapMessage);

    void removeComponentService(AdComponentEntity adc);

    void addComponentService(AdComponentEntity adc);

}
