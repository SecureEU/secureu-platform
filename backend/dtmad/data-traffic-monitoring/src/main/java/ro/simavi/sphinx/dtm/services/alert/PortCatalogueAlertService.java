package ro.simavi.sphinx.dtm.services.alert;

import ro.simavi.sphinx.dtm.model.PortCatalogueModel;
import ro.simavi.sphinx.dtm.services.DTMAlertComponentService;

import java.util.List;

public interface PortCatalogueAlertService extends DTMAlertComponentService {

    List<PortCatalogueModel> getPortCatalogueList();

    PortCatalogueModel findById(Long id);

    void save(PortCatalogueModel portCatalogueModel);

    void delete(Long id);

    List<PortCatalogueModel> getAlerts();

    void clearCache();
}
