package pucp.pdds.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.VehiculoRepository;

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
        Vehiculo.TipoVehiculo tipo = vehiculo.getTipo();
        if (tipo != null) {
            java.util.List<Vehiculo> tipoVehiculos = vehiculoRepository.findByTipoOrderByPlacaDesc(tipo);
            int nextNumber = 1;
            if (!tipoVehiculos.isEmpty()) {
                String lastPlaca = tipoVehiculos.get(0).getPlaca(); // Ejemplo: TD03, TA01, etc.
                // Extraer el número al final de la placa
                String numberPart = lastPlaca.replaceAll("^" + tipo.name() + "(0*)", "");
                try {
                    int lastNumber = Integer.parseInt(numberPart);
                    nextNumber = lastNumber + 1;
                } catch (NumberFormatException e) {
                    // Si la placa no tiene número, usar 1
                    nextNumber = 1;
                }
            }
            String newPlaca = String.format("%s%02d", tipo.name(), nextNumber);
            vehiculo.setPlaca(newPlaca);
        }
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
                    vehiculo.setCurrCombustible(vehiculoDetails.getCurrCombustible());
                    vehiculo.setCurrGlp(vehiculoDetails.getCurrGlp());
                    vehiculo.setDisponible(vehiculoDetails.isDisponible());
                    vehiculo.setPosicionX(vehiculoDetails.getPosicionX());
                    vehiculo.setPosicionY(vehiculoDetails.getPosicionY());
                    vehiculo.setPlaca(vehiculoDetails.getPlaca());

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