package ro.simavi.sphinx.ad.services.impl;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.entities.AdComponentEntity;
import ro.simavi.sphinx.ad.jpa.repositories.AdComponentRepository;
import ro.simavi.sphinx.ad.services.AdComponentService;

import java.util.List;

@Service
public class AdComponentServiceImpl implements AdComponentService {

    private final AdComponentRepository adComponentRepository;

    public AdComponentServiceImpl(AdComponentRepository adComponentRepository){
        this.adComponentRepository = adComponentRepository;
    }

    @Override
    public List<AdComponentEntity> getAdComponentList() {
        return IteratorUtils.toList(adComponentRepository.findAll().iterator());
    }

    @Override
    public AdComponentEntity enable(AdComponentEntity adComponentEntity, Boolean enable) {
        adComponentEntity.setEnabled(enable);
        adComponentRepository.save(adComponentEntity);
        return adComponentEntity;
    }

}
