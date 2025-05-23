package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.VehiculoRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllVehiculos() {
        List<Vehiculo> vehiculos = vehiculoRepository.findAll();
        List<Map<String, Object>> vehiculoMaps = vehiculos.stream()
            .map(Vehiculo::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(vehiculoMaps);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVehiculoById(@PathVariable Long id) {
        return vehiculoRepository.findById(id)
                .map(vehiculo -> ResponseEntity.ok(vehiculo.toMap()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Vehiculo> createVehiculo(@RequestBody Vehiculo vehiculo) {
        Vehiculo savedVehiculo = vehiculoRepository.save(vehiculo);
        return ResponseEntity.ok(savedVehiculo);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> updateVehiculo(@PathVariable Long id, @RequestBody Vehiculo vehiculoDetails) {
        return vehiculoRepository.findById(id)
                .map(vehiculo -> {
                    vehiculo.setTipo(vehiculoDetails.getTipo());
                    vehiculo.setPeso(vehiculoDetails.getPeso());
                    vehiculo.setMaxCombustible(vehiculoDetails.getMaxCombustible());
                    vehiculo.setMaxGlp(vehiculoDetails.getMaxGlp());
                    vehiculo.setDisponible(vehiculoDetails.isDisponible());
                    Vehiculo updatedVehiculo = vehiculoRepository.save(vehiculo);
                    return ResponseEntity.ok(updatedVehiculo);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehiculo(@PathVariable Long id) {
        return vehiculoRepository.findById(id)
                .map(vehiculo -> {
                    vehiculoRepository.delete(vehiculo);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
