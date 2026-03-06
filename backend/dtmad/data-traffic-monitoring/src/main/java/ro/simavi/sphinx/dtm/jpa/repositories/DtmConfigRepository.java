package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.DtmConfigEntity;

@Repository
public interface DtmConfigRepository extends CrudRepository<DtmConfigEntity, Long> {

    Iterable<DtmConfigEntity> findAllByOrderByCodeAsc();

}
