package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.BlackWebEntity;

import java.util.List;

public interface BlackWebService {
    List<BlackWebEntity> getList(Long categoryId);

    void save(BlackWebEntity blackWebEntity);

    void delete(Long id);

    BlackWebEntity findById(Long id);

    List<BlackWebEntity> getList();

    void importFromFile(List<BlackWebEntity> blackWebEntities, Long categoryId);
}
