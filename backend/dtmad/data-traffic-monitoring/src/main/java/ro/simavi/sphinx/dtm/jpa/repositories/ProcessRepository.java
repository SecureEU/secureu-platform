package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.ProcessEntity;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;

@Repository
public interface ProcessRepository extends CrudRepository<ProcessEntity, Long> {

    @Query("SELECT ti FROM ProcessEntity ti WHERE ti.interfaceName=:interfaceName AND ti.instance.key=:instanceKey and ti.processType=:processType")
    Iterable<ProcessEntity> findByInstanceKeyName(@Param("instanceKey") String instanceKey, @Param("interfaceName") String interfaceName, @Param("processType") ProcessType processType);

    @Query("SELECT ti FROM ProcessEntity ti WHERE ti.interfaceFullName=:interfaceFullName AND ti.instance.key=:instanceKey and ti.processType=:processType")
    Iterable<ProcessEntity> findByInstanceKeyFullName(@Param("instanceKey") String instanceKey, @Param("interfaceFullName") String interfaceFullName, @Param("processType") ProcessType processType);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProcessEntity ti SET ti.active=false WHERE ti.instance.id=:instanceId and ti.processType=:processType")
    void inactivateAllInterfaces(@Param("instanceId") Long instanceId, @Param("processType") ProcessType processType);

    @Query("SELECT ti FROM ProcessEntity ti WHERE ti.instance.id=:instanceId and ti.processType=:processType ORDER BY ti.enabled DESC")
    Iterable<ProcessEntity> findAllByOrderByEnabledDesc(@Param("instanceId") Long instanceId, @Param("processType") ProcessType processType);

    @Query("SELECT ti FROM ProcessEntity ti WHERE ti.instance.key=:instanceKey and ti.processType=:processType ORDER BY ti.enabled DESC")
    Iterable<ProcessEntity> findAllInstanceKey(@Param("instanceKey") String instanceKey, @Param("processType") ProcessType processType);

    @Modifying(clearAutomatically = true)
    @Query("DELETE from ProcessEntity ti WHERE ti.instance.id=:instanceId and ti.id=:id and ti.processType=:processType")
    void delete(@Param("instanceId") Long instanceId, @Param("processType") ProcessType processType, @Param("id") Long id);
}
