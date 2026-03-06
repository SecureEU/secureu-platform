package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.BlackWebCategoryEntity;

import java.util.List;

public interface BlackWebCategoryService {
    List<BlackWebCategoryEntity> getList();

    void save(BlackWebCategoryEntity blackWebEntity);

    void delete(Long id);

    BlackWebCategoryEntity findById(Long id);
}
