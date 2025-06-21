package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.pdds.backend.model.Pedido;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByFechaRegistroBetween(LocalDateTime startDate, LocalDateTime endDate);
}
