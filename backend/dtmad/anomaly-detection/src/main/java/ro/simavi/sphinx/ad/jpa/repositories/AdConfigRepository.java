package ro.simavi.sphinx.ad.jpa.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.ad.entities.AdConfigEntity;

@Repository
public interface AdConfigRepository extends CrudRepository<AdConfigEntity, Long> {

    Iterable<AdConfigEntity> findAllByOrderByCodeAsc();

}
