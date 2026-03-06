package ro.simavi.sphinx.dtm.services;

import org.springframework.data.domain.Page;
import ro.simavi.sphinx.dtm.entities.AlertEntity;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;

import java.util.Date;
import java.util.List;

public interface AlertService {

    void collect(AlertEventModel metricModel,StixModelAlert stixModelAlert);

    List<AlertEventModel> getAlerts();

    Page<AlertEntity> getAlertFiler(String prefix, int pageNo, int pageSize);

    List<AlertEntity> getAlerts( Date date, Long limit);

    void removeAll();
}
