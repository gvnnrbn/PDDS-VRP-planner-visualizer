package pucp.pdds.backend.service;

import org.springframework.stereotype.Service;
import pucp.pdds.backend.dto.TipoVehiculoDto;
import pucp.pdds.backend.model.TipoVehiculo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TipoVehiculoService {
    
    public List<TipoVehiculoDto> getAllTipos() {
        return Arrays.stream(TipoVehiculo.values())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public TipoVehiculoDto getTipoById(String tipoId) {
        TipoVehiculo tipo = Arrays.stream(TipoVehiculo.values())
                .filter(t -> t.name().equals(tipoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tipo de vehiculo no encontrado: " + tipoId));
        return convertToDto(tipo);
    }
    
    private TipoVehiculoDto convertToDto(TipoVehiculo tipo) {
        return new TipoVehiculoDto(
            tipo.name(),
            tipo.getPeso(),
            tipo.getMaxCombustible(),
            tipo.getMaxGlp()
        );
    }
}
