package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.ProcessFilterEntity;

import java.util.Optional;

@Repository
public interface ProcessFilterRepository extends CrudRepository<ProcessFilterEntity, Long> {

    Iterable<ProcessFilterEntity> findAllByOrderByNameAsc();

    Optional<ProcessFilterEntity> findByName(String filterName);
}
