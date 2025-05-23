package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pucp.pdds.backend.model.Incidencia;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.IncidenciaRepository;
import pucp.pdds.backend.repository.VehiculoRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {
    
    @Autowired
    private IncidenciaRepository incidenciaRepository;
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllIncidencias() {
        List<Incidencia> incidencias = incidenciaRepository.findAll();
        List<Map<String, Object>> incidenciaMaps = incidencias.stream()
            .map(Incidencia::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(incidenciaMaps);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getIncidenciaById(@PathVariable Long id) {
        return incidenciaRepository.findById(id)
                .map(incidencia -> ResponseEntity.ok(incidencia.toMap()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Incidencia> createIncidencia(@RequestBody Incidencia incidencia) {
        // Ensure ID is null for new entities
        incidencia.setId(null);
        
        // Ensure vehiculo exists
        if (incidencia.getVehiculo() != null) {
            Vehiculo vehiculo = vehiculoRepository.findById(incidencia.getVehiculo().getId())
                    .orElseThrow(() -> new RuntimeException("Vehiculo not found"));
            incidencia.setVehiculo(vehiculo);
        }
        
        Incidencia savedIncidencia = incidenciaRepository.save(incidencia);
        return ResponseEntity.ok(savedIncidencia);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Incidencia> updateIncidencia(@PathVariable Long id, @RequestBody Incidencia incidenciaDetails) {
        return incidenciaRepository.findById(id)
                .map(incidencia -> {
                    incidencia.setFecha(incidenciaDetails.getFecha());
                    incidencia.setTurno(incidenciaDetails.getTurno());
                    
                    // Update vehiculo if provided
                    if (incidenciaDetails.getVehiculo() != null) {
                        Vehiculo vehiculo = vehiculoRepository.findById(incidenciaDetails.getVehiculo().getId())
                                .orElseThrow(() -> new RuntimeException("Vehiculo not found"));
                        incidencia.setVehiculo(vehiculo);
                    }
                    
                    incidencia.setOcurrido(incidenciaDetails.isOcurrido());
                    return ResponseEntity.ok(incidenciaRepository.save(incidencia));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncidencia(@PathVariable Long id) {
        return incidenciaRepository.findById(id)
                .map(incidencia -> {
                    incidenciaRepository.delete(incidencia);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
