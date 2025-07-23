package pucp.pdds.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pucp.pdds.backend.model.Bloqueo;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {
    @Query(value = "SELECT * FROM bloqueo b WHERE b.start_time < :currDate AND b.end_time > :currDate", nativeQuery = true)
    List<Bloqueo> findCurrent(LocalDateTime currDate);

    List<Bloqueo> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
} 