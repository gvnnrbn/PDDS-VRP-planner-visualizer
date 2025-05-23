package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.pdds.backend.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
