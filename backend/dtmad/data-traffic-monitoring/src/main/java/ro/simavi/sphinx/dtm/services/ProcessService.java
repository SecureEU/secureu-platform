package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.dtm.model.InstanceModel;
import ro.simavi.sphinx.dtm.model.ProcessModel;

import java.util.List;
import java.util.Optional;

public interface ProcessService {

    Iterable<ProcessEntity> findByInstanceKeyName(String instanceKey, String name, ProcessType processType);

    Iterable<ProcessEntity> findByInstanceKeyFullName(String instanceKey, String fullname, ProcessType processType);

    ProcessEntity save(ProcessEntity processEntity);

    ProcessEntity save(ProcessModel tsharkProcessModel);

    void inactivateAllInterfaces(InstanceModel instanceModel, ProcessType processType);

    List<ProcessEntity> findAll(InstanceModel instanceModel, ProcessType processType);

    Optional<ProcessEntity> findById(Long pid);

    List<ProcessEntity> findAllInstanceKey(String instanceKey, ProcessType processType);

    void delete(Long instanceid, ProcessType processType, Long processId);
}
