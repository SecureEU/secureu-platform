package ro.simavi.sphinx.dtm.jpa.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ro.simavi.sphinx.dtm.entities.AlertEntity;

import java.time.LocalDateTime;

@Repository
public interface DtmAlertRepository extends JpaRepository<AlertEntity, Long> {

    Page<AlertEntity> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<AlertEntity> findAllBySphinxTool(String sphinxTool, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "delete from \"alert\" where \"timestamp\" < :date", nativeQuery = true)// will delete all records older than a number of days
    void deleteByNumberOfDays(@Param("date") LocalDateTime date);

}
