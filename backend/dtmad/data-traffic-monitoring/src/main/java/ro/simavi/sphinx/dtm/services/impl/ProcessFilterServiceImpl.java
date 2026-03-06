package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.ProcessFilterEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.ProcessFilterRepository;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.services.ProcessFilterService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessFilterServiceImpl implements ProcessFilterService {

    private final ProcessFilterRepository processFilterRepository;

    public ProcessFilterServiceImpl(ProcessFilterRepository processFilterRepository){
        this.processFilterRepository = processFilterRepository;
    }

    public void save(ProcessFilterEntity processFilterEntity){
        processFilterRepository.save(processFilterEntity);
    }

    public List<ProcessFilterModel> findAll(){
        List<ProcessFilterModel> processFilterModels = new ArrayList<>();
        processFilterRepository.findAllByOrderByNameAsc().forEach( t -> processFilterModels.add(toProcessFilterModel(t)) );
        return processFilterModels;
    }

    @Override
    public void save(ProcessFilterModel processFilterModel) {
        this.save(toProcessFilterEntity(processFilterModel));
    }

    @Override
    public void deleteById(Long id) {
        processFilterRepository.deleteById(id);
    }

    @Override
    public ProcessFilterModel findById(Long id) {
        Optional<ProcessFilterEntity> optionalProcessFilterEntity = processFilterRepository.findById(id);
        if (optionalProcessFilterEntity.isPresent()){
            ProcessFilterEntity processFilterEntity = optionalProcessFilterEntity.get();
            return toProcessFilterModel(processFilterEntity);
        }
        return null;
    }

    @Override
    public ProcessFilterModel findByName(String filterName) {
        Optional<ProcessFilterEntity> processFilterEntityOptional = processFilterRepository.findByName(filterName);
        if (processFilterEntityOptional.isPresent()){
            ProcessFilterEntity processFilterEntity = processFilterEntityOptional.get();
            return toProcessFilterModel(processFilterEntity);
        }
        return null;
    }

    public ProcessFilterModel toProcessFilterModel(ProcessFilterEntity processFilterEntity) {
        if (processFilterEntity!=null) {
            ProcessFilterModel processFilterModel = new ProcessFilterModel();
            processFilterModel.setId(processFilterEntity.getId());
            processFilterModel.setName(processFilterEntity.getName());
            processFilterModel.setDescription(processFilterEntity.getDescription());
            processFilterModel.setCommand(processFilterEntity.getCommand());
            processFilterModel.setCanDelete(processFilterEntity.getCanDelete());
            processFilterModel.setCode(processFilterEntity.getCode());
            return processFilterModel;
        }

        return null;
    }

    private ProcessFilterEntity toProcessFilterEntity(ProcessFilterModel processFilterModel){
        ProcessFilterEntity processFilterEntity = new ProcessFilterEntity();
        if (processFilterModel.getId()!=null){
            Optional<ProcessFilterEntity> filterEntityOptional = processFilterRepository.findById(processFilterModel.getId());
            if (filterEntityOptional.isPresent()){
                processFilterEntity = filterEntityOptional.get();
            }
        }

        processFilterEntity.setId(processFilterModel.getId());
        processFilterEntity.setName(processFilterModel.getName());
        processFilterEntity.setCommand(processFilterModel.getCommand());
        processFilterEntity.setDescription(processFilterModel.getDescription());
        return processFilterEntity;
    }

}
