package ro.simavi.sphinx.ad.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.ad.entities.AdComponentEntity;

@Repository
public interface AdComponentRepository extends CrudRepository<AdComponentEntity, Long> {

}
