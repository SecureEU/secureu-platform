package ro.simavi.sphinx.dtm.services.alert;

import ro.simavi.sphinx.dtm.services.DTMAlertComponentService;

public interface BlackWebAlertService extends DTMAlertComponentService {

    void clearCache();

}
