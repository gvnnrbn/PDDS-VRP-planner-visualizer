package pucp.pdds.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pucp.pdds.backend.model.Vehiculo;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    java.util.List<Vehiculo> findByTipoOrderByPlacaDesc(Vehiculo.TipoVehiculo tipo);
}
