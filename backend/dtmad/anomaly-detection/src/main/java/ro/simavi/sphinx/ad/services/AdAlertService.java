package ro.simavi.sphinx.ad.services;

import ro.simavi.sphinx.ad.entities.AlertEntity;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;

import java.util.Date;
import java.util.List;

public interface AdAlertService {

    void collect(StixModelAlert stixModelAlert);

    List<AlertEntity> getAlerts(Date date, Long limit);
}
