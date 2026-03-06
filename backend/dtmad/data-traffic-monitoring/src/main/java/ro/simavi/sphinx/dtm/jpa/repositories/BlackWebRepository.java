package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.dtm.entities.BlackWebEntity;

import java.util.List;

@Repository
public interface BlackWebRepository extends CrudRepository<BlackWebEntity, Long> {

    @Query("SELECT b FROM BlackWebEntity b WHERE b.category.id=:categoryId")
    List<BlackWebEntity> findAllByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT b FROM BlackWebEntity b WHERE b.domain=:domain")
    List<BlackWebEntity> findByDomain(@Param("domain") String domain);
}
