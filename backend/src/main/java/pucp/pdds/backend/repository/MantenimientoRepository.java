package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.pdds.backend.model.Mantenimiento;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {
} 