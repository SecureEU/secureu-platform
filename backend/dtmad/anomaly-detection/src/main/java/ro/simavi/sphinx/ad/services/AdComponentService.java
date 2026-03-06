package ro.simavi.sphinx.ad.services;

import ro.simavi.sphinx.ad.entities.AdComponentEntity;

import java.util.List;

public interface AdComponentService {

    List<AdComponentEntity> getAdComponentList();

    AdComponentEntity enable(AdComponentEntity adComponentEntity, Boolean enable);

}
