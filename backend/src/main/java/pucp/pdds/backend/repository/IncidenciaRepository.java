package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.pdds.backend.model.Incidencia;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
}
