package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.ProcessFilterEntity;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;

import java.util.List;

public interface ProcessFilterService {

    List<ProcessFilterModel> findAll();

    void save(ProcessFilterModel processFilterModel);

    void deleteById(Long id);

    ProcessFilterModel findById(Long id);

    ProcessFilterModel findByName(String filterName);

    ProcessFilterModel toProcessFilterModel(ProcessFilterEntity processFilterEntity);
}
