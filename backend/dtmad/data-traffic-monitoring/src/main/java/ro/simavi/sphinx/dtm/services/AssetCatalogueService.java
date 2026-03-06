package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.AssetCatalogueEntity;
import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.model.event.EventModel;

import java.util.List;

public interface AssetCatalogueService extends CacheableService{

    List<AssetCatalogueEntity> getAssetCatalogueList();

    AssetCatalogueEntity findById(Long id);

    void save(AssetModel assetCatalogueEntity);

    void delete(Long id);

    /* todo: tshark */
    void collect(String[] pcapMessage);

    /* todo: suricata */
    void collect(EventModel metricModel);

    List<AssetModel> getAlerts();

    List<AssetCatalogueEntity> getAssetAlerts();

    boolean untouch(String physicalAddress);

}
