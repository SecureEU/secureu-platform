package ro.simavi.sphinx.dtm.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.entities.ProcessFilterEntity;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.dtm.jpa.repositories.InstanceRepository;
import ro.simavi.sphinx.dtm.jpa.repositories.ProcessFilterRepository;
import ro.simavi.sphinx.dtm.jpa.repositories.ProcessRepository;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.ProcessService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessServiceImpl implements ProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessServiceImpl.class);

    private final ProcessRepository processRepository;

    private final ProcessFilterRepository filterRepository;

    private final InstanceRepository instanceRepository;

    private final DTMConfigProps dtmConfigProps;

    public ProcessServiceImpl(ProcessRepository processRepository, ProcessFilterRepository filterRepository, InstanceRepository instanceRepository,
                              DTMConfigProps dtmConfigProps){
        this.processRepository = processRepository;
        this.filterRepository = filterRepository;
        this.instanceRepository = instanceRepository;
        this.dtmConfigProps = dtmConfigProps;
    }

    @Override
    public Iterable<ProcessEntity> findByInstanceKeyName(String instanceKey, String name, ProcessType processType) {
        return processRepository.findByInstanceKeyName(instanceKey, name, processType);
    }
    @Override
    public Iterable<ProcessEntity> findByInstanceKeyFullName(String instanceKey, String fullname, ProcessType processType) {
        return processRepository.findByInstanceKeyFullName(instanceKey, fullname, processType);
    }

    @Override
    public ProcessEntity save(ProcessEntity processEntity) {
        return processRepository.save(processEntity);
    }

    @Override
    @Transactional
    public ProcessEntity save(ProcessModel processModel) {

        ProcessEntity processEntity = null;

        if (processModel.getPid()!=null){
            Optional<ProcessEntity> processEntityOptional = processRepository.findById(processModel.getPid());
            if (processEntityOptional.isPresent()){
                processEntity = processEntityOptional.get();
            }
        }else{
            processEntity = new ProcessEntity();
            processEntity.setInterfaceName(processModel.getInterfaceName());
            processEntity.setInterfaceDisplayName(processModel.getInterfaceDisplayName());
            processEntity.setInterfaceFullName(processModel.getInterfaceFullName());
            processEntity.setInstance(instanceRepository.findByKey(processModel.getInstanceKey()).get());
            processEntity.setDescription(processModel.getInterfaceFullName());
            processEntity.setEnabled(Boolean.FALSE);
            processEntity.setActive(Boolean.TRUE);
            processEntity.setProcessType(processModel.getProcessType());
        }

        // update filter
        String filterName= processModel.getFilterName();
        if (!StringUtils.isEmpty(filterName)){
            Optional<ProcessFilterEntity> filterEntityOptional = filterRepository.findByName(filterName);
            if (filterEntityOptional.isPresent()) {
                processEntity.setFilter(filterEntityOptional.get());
            }
        }else{
            processEntity.setFilter(null);
        }
        return save(processEntity);

    }

    @Override
    @Transactional
    public void inactivateAllInterfaces(InstanceModel instanceModel, ProcessType processType) {
        processRepository.inactivateAllInterfaces(instanceModel.getId(), processType);
    }

    @Override
    public List<ProcessEntity> findAll(InstanceModel instanceModel, ProcessType processType) {
        Iterable<ProcessEntity> processEntities =  processRepository.findAllByOrderByEnabledDesc(instanceModel.getId(), processType);
        return filterBySource(processEntities,processType);
    }

    @Override
    public Optional<ProcessEntity> findById(Long pid) {
        return processRepository.findById(pid);
    }

    @Override
    public List<ProcessEntity> findAllInstanceKey(String instanceKey, ProcessType processType) {
        Iterable<ProcessEntity> processEntities = processRepository.findAllInstanceKey(instanceKey, processType);
        return filterBySource(processEntities,processType);
    }

    @Override
    @Transactional
    public void delete(Long instanceId, ProcessType processType, Long id) {
        processRepository.delete(instanceId, processType, id);
    }

    private List<ProcessEntity> filterBySource(Iterable<ProcessEntity> processEntities, ProcessType processType){
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(processType.name().toLowerCase());
        String source = toolModel.getProperties().get("source");

        String[] sourceList = source.split(",");
        boolean allSources = true;
        if (sourceList!=null && sourceList.length>0){
            allSources = sourceList[0].equalsIgnoreCase("all");
        }

        logger.info("===== [NetworkInterfaceController] display/{instanceKey}/{tool} SOURCE = "+source+" =====");

        List<ProcessEntity> result = new ArrayList<ProcessEntity>();
        for (ProcessEntity processEntity : processEntities) {
            if (accept(allSources, sourceList, processEntity.getInterfaceName())) {
                result.add(processEntity);
            }
        }

        return result;
    }

    private boolean accept(boolean allSources, String[] sourceList, String interfaceName){
        if (allSources){
            return true;
        }
        for(String source: sourceList){
            if (source.equals(interfaceName)){
                return true;
            }
        }
        return false;
    }

}
