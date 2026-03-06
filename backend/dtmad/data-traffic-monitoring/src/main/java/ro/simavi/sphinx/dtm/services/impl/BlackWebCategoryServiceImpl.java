package ro.simavi.sphinx.dtm.services.impl;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.BlackWebCategoryEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.BlackWebCategoryRepository;
import ro.simavi.sphinx.dtm.services.BlackWebCategoryService;

import java.util.List;
import java.util.Optional;

@Service
public class BlackWebCategoryServiceImpl implements BlackWebCategoryService {

    private BlackWebCategoryRepository blackWebCategoryRepository;

    public BlackWebCategoryServiceImpl(BlackWebCategoryRepository blackWebCategoryRepository){
        this.blackWebCategoryRepository = blackWebCategoryRepository;
    }

    @Override
    public List<BlackWebCategoryEntity> getList() {
        return IteratorUtils.toList(blackWebCategoryRepository.findAll().iterator());
    }

    @Override
    public void save(BlackWebCategoryEntity blackWebCategoryEntityNew) {
        if (blackWebCategoryEntityNew.getId()!=null){
            Optional<BlackWebCategoryEntity> blackWebCategoryEntityOptional = blackWebCategoryRepository.findById(blackWebCategoryEntityNew.getId());
            if (blackWebCategoryEntityOptional.isPresent()){
                BlackWebCategoryEntity blackWebCategoryEntity = blackWebCategoryEntityOptional.get();
                blackWebCategoryEntity.setName(blackWebCategoryEntityNew.getName());
                blackWebCategoryEntity.setDescription(blackWebCategoryEntityNew.getDescription());
                blackWebCategoryRepository.save(blackWebCategoryEntity);
            }
        }else {
            blackWebCategoryRepository.save(blackWebCategoryEntityNew);
        }
    }

    @Override
    public void delete(Long id) {
        blackWebCategoryRepository.deleteById(id);
    }

    @Override
    public BlackWebCategoryEntity findById(Long id) {
        Optional<BlackWebCategoryEntity> blackWebCategoryEntityOptional = blackWebCategoryRepository.findById(id);
        if (blackWebCategoryEntityOptional.isPresent()){
            return blackWebCategoryEntityOptional.get();
        }
        return null;
    }
}
