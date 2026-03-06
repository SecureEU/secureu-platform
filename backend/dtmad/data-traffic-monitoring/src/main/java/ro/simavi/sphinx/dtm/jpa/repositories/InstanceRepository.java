package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.InstanceEntity;

import java.util.Optional;

@Repository
public interface InstanceRepository extends CrudRepository<InstanceEntity, Long> {

    Optional<InstanceEntity> findByKey(String key);
}
