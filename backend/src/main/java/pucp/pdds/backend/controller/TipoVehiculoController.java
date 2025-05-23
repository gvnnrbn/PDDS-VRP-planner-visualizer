package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pucp.pdds.backend.model.TipoVehiculo;
import pucp.pdds.backend.repository.TipoVehiculoRepository;

@RestController
@RequestMapping("/api/tipos-vehiculos")
public class TipoVehiculoController {
    
    @Autowired
    private TipoVehiculoRepository tipoVehiculoRepository;
    
    @GetMapping
    public ResponseEntity<TipoVehiculo[]> getAllTipos() {
        return ResponseEntity.ok(tipoVehiculoRepository.getAll());
    }
    
    @GetMapping("/{tipoId}")
    public ResponseEntity<TipoVehiculo> getTipoById(@PathVariable String tipoId) {
        return tipoVehiculoRepository.findById(tipoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
