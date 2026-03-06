package ro.simavi.sphinx.ad.jpa.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.simavi.sphinx.ad.entities.AlertEntity;

import java.time.LocalDateTime;

@Repository
public interface AdAlertRepository extends JpaRepository<AlertEntity, Long> {

    Page<AlertEntity> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

}
