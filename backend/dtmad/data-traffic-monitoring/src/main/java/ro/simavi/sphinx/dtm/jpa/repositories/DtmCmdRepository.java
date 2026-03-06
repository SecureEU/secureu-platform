package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.DtmCmdEntity;

import java.util.List;

@Repository
public interface DtmCmdRepository extends CrudRepository<DtmCmdEntity, Long> {

    @Query("SELECT c from DtmCmdEntity c")
    public List<DtmCmdEntity> findWithPageable(Pageable pageable);

}
