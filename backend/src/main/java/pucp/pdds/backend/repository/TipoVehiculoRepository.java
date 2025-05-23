package pucp.pdds.backend.repository;

import org.springframework.stereotype.Repository;
import pucp.pdds.backend.model.TipoVehiculo;

import java.util.Arrays;
import java.util.Optional;

@Repository
public class TipoVehiculoRepository {
    
    public TipoVehiculo[] getAll() {
        return TipoVehiculo.values();
    }
    
    public Optional<TipoVehiculo> findById(String id) {
        return Arrays.stream(TipoVehiculo.values())
            .filter(tipo -> tipo.name().equals(id))
            .findFirst();
    }
}
