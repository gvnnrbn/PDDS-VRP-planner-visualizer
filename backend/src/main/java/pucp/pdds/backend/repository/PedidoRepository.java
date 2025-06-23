package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pucp.pdds.backend.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByFechaRegistroBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT * FROM pedido p WHERE p.fecha_registro < :currDate AND (p.fecha_registro + (p.tiempo_tolerancia || ' minutes')::interval) > :currDate", nativeQuery = true)
    List<Pedido> findCurrent(LocalDateTime currDate);
}
