package ro.simavi.sphinx.dtm.services.impl;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.BlackWebCategoryEntity;
import ro.simavi.sphinx.dtm.entities.BlackWebEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.BlackWebCategoryRepository;
import ro.simavi.sphinx.dtm.jpa.repositories.BlackWebRepository;
import ro.simavi.sphinx.dtm.services.BlackWebService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BlackWebServiceImpl implements BlackWebService {

    private final BlackWebRepository blackWebRepository;

    private final BlackWebCategoryRepository blackWebCategoryRepository;

    public BlackWebServiceImpl(BlackWebRepository blackWebRepository,BlackWebCategoryRepository blackWebCategoryRepository){
        this.blackWebRepository = blackWebRepository;
        this.blackWebCategoryRepository = blackWebCategoryRepository;
    }

    @Override
    public List<BlackWebEntity> getList(Long categoryId) {
        return blackWebRepository.findAllByCategory(categoryId);
    }

    @Override
    public void save(BlackWebEntity blackWebEntityNew) {
        if (blackWebEntityNew.getId()!=null){
            Optional<BlackWebEntity> blackWebEntityOptional = blackWebRepository.findById(blackWebEntityNew.getId());
            if (blackWebEntityOptional.isPresent()){
                BlackWebEntity blackWebEntity = blackWebEntityOptional.get();
                blackWebEntity.setDomain(blackWebEntityNew.getDomain());
                blackWebEntity.setCategory(blackWebCategoryRepository.findById(blackWebEntityNew.getCategory().getId()).get());
                blackWebRepository.save(blackWebEntity);
            }
        }else {
            blackWebRepository.save(blackWebEntityNew);
        }
    }

    @Override
    public void delete(Long id) {
        blackWebRepository.deleteById(id);
    }

    @Override
    public BlackWebEntity findById(Long id) {
        return blackWebRepository.findById(id).get();
    }

    @Override
    public List<BlackWebEntity> getList() {
        return IteratorUtils.toList(blackWebRepository.findAll().iterator());
    }

    @Override
    public void importFromFile(List<BlackWebEntity> blackWebEntities, Long categoryId) {
        BlackWebCategoryEntity blackWebCategoryEntity = blackWebCategoryRepository.findById(categoryId).get();
        List<BlackWebEntity> blackWebEntityToSaveList = new ArrayList<>();
        for(BlackWebEntity blackWebEntity:blackWebEntities){
            List<BlackWebEntity> blackWebEntityList = blackWebRepository.findByDomain(blackWebEntity.getDomain());
            if (blackWebEntityList==null || blackWebEntityList.size()==0){
                blackWebEntity.setCategory(blackWebCategoryEntity);

                blackWebEntityToSaveList.add(blackWebEntity);
            }
        }

        blackWebRepository.saveAll(blackWebEntityToSaveList);
    }

}
