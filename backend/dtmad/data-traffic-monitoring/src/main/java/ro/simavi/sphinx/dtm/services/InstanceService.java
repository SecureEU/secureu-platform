package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.InstanceEntity;
import ro.simavi.sphinx.dtm.model.InstanceModel;

import java.util.List;

public interface InstanceService {

    List<InstanceModel> findAll();

    void save(InstanceModel filterModel);

    InstanceModel getByKey(String key);

    boolean isStartable(String key);

    boolean toggleEnable(Long id);

    void delete(Long id);

    InstanceModel getById(Long id);

    InstanceEntity findById(Long id);

}
