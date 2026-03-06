package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.PortCatalogueEntity;

@Repository
public interface PortCatalogueRepository extends CrudRepository<PortCatalogueEntity, Long> {

}
