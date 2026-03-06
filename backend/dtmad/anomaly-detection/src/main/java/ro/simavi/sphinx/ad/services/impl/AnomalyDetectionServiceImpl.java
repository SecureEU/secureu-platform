package ro.simavi.sphinx.ad.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.entities.AdComponentEntity;
import ro.simavi.sphinx.ad.services.AdComponentService;
import ro.simavi.sphinx.ad.services.AnomalyDetectionComponentService;
import ro.simavi.sphinx.ad.services.AnomalyDetectionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AdComponentService adComponentService;

    private Set<AnomalyDetectionComponentService> anomalyDetectionComponentServices;

    public AnomalyDetectionServiceImpl(AdComponentService adComponentService){

        this.adComponentService = adComponentService;
        this.anomalyDetectionComponentServices =  new HashSet<>();

        List<AdComponentEntity> adComponentEntities = adComponentService.getAdComponentList();
        adComponentEntities.forEach(adc->{
           this.addComponentService(adc);
        });
    }

    public void removeComponentService(AdComponentEntity adc){
        anomalyDetectionComponentServices.remove(getService(adc));

    }

    public void addComponentService(AdComponentEntity adc){
        if (adc.getEnabled()==Boolean.TRUE) {
            anomalyDetectionComponentServices.add(getService(adc));
        }
    }

    private AnomalyDetectionComponentService getService(AdComponentEntity adc){
        return null;
    }

    @Override
    public void detect(String[] pcapMessage) {
        for(AnomalyDetectionComponentService anomalyDetectionComponentService: anomalyDetectionComponentServices){
            if (anomalyDetectionComponentService!=null) {
                anomalyDetectionComponentService.detect(pcapMessage);
            }
        }
    }

}
