package ro.simavi.sphinx.dtm.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.InstanceEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.InstanceRepository;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.services.InstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InstanceServiceImpl implements InstanceService {

    private final InstanceRepository instanceRepository;

    public InstanceServiceImpl(InstanceRepository instanceRepository){
        this.instanceRepository = instanceRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(InstanceServiceImpl.class);

    @Override
    public List<InstanceModel> findAll() {

        List<InstanceModel> instanceModels = new ArrayList<>();
        instanceRepository.findAll().forEach(t -> instanceModels.add(toInstanceModel(t)) );
        return instanceModels;

    }

    private InstanceModel toInstanceModel(InstanceEntity instanceEntity) {

        InstanceModel instanceModel = new InstanceModel();
        instanceModel.setId(instanceEntity.getId());
        instanceModel.setDescription(instanceEntity.getDescription());
        instanceModel.setKey(instanceEntity.getKey());
        instanceModel.setName(instanceEntity.getName());
        instanceModel.setEnabled(instanceEntity.getEnabled());
        instanceModel.setUrl(instanceEntity.getUrl());
        instanceModel.setHasTshark(instanceEntity.getHasTshark());
        instanceModel.setHasSuricata(instanceEntity.getHasSuricata());
        instanceModel.setIsMaster(instanceEntity.getMaster());

        return instanceModel;
    }

    @Override
    public void save(InstanceModel instanceModel) {
        InstanceEntity instanceEntity = null;
        if (instanceModel.getId()==null) {
            instanceEntity = new InstanceEntity();
            instanceEntity.setEnabled(Boolean.FALSE);
            String newKey = instanceModel.getKey();
            instanceEntity.setKey(newKey);

        }else{
            Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findById(instanceModel.getId());
            if (optionalInstanceEntity.isPresent()){
                instanceEntity = optionalInstanceEntity.get();
            }
        }
        if (instanceEntity!=null) {
            instanceEntity.setName(instanceModel.getName());
            instanceEntity.setUrl(instanceModel.getUrl());
            instanceEntity.setDescription(instanceModel.getDescription());
            instanceEntity.setHasTshark(instanceModel.getHasTshark());
            instanceEntity.setHasSuricata(instanceModel.getHasSuricata());
            instanceEntity.setMaster(instanceModel.getIsMaster());
            instanceRepository.save(instanceEntity);

        }
    }

    @Override
    public InstanceModel getByKey(String key) {
        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findByKey(key);
        if (optionalInstanceEntity.isPresent()){
            return toInstanceModel(optionalInstanceEntity.get());
        }
        return null;
    }

    @Override
    public boolean isStartable(String key) {
        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findByKey(key);
        if (optionalInstanceEntity.isPresent()){
            boolean startable = optionalInstanceEntity.get().getEnabled()!=null && optionalInstanceEntity.get().getEnabled()==Boolean.TRUE;
            logger.info("isStartable ("+key+") = " + startable);
            return startable;
        }
        logger.info("isStartable ("+key+") = FALSE" );
        return false;
    }

    @Override
    public boolean toggleEnable(Long id) {
        boolean enable = Boolean.TRUE;
        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findById(id);
        if (optionalInstanceEntity.isPresent()){
            InstanceEntity optionalInstanceEntity1 = optionalInstanceEntity.get();
            if (optionalInstanceEntity1.getEnabled()==null){
                optionalInstanceEntity1.setEnabled(Boolean.TRUE);
            }else {
                enable = !optionalInstanceEntity1.getEnabled();
                optionalInstanceEntity1.setEnabled(enable);
            }
            instanceRepository.save(optionalInstanceEntity1);
        }

        return enable;
    }

    @Override
    public void delete(Long id) {

        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findById(id);

        if (optionalInstanceEntity.isPresent()) {
            instanceRepository.deleteById(id);
        }

    }

    @Override
    public InstanceModel getById(Long id) {
        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findById(id);
        if (optionalInstanceEntity.isPresent()){
            return toInstanceModel(optionalInstanceEntity.get());
        }
        return null;
    }

    @Override
    public InstanceEntity findById(Long id) {
        Optional<InstanceEntity> optionalInstanceEntity = instanceRepository.findById(id);
        if (optionalInstanceEntity.isPresent()){
            return optionalInstanceEntity.get();
        }
        return null;
    }

}
