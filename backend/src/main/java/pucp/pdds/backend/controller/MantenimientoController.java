package pucp.pdds.backend.controller;

import java.util.List;

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

import pucp.pdds.backend.model.Mantenimiento;
import pucp.pdds.backend.model.Vehiculo;
import pucp.pdds.backend.repository.MantenimientoRepository;
import pucp.pdds.backend.repository.VehiculoRepository;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    @Autowired
    private MantenimientoRepository mantenimientoRepository;
    
    @Autowired
    private VehiculoRepository vehiculoRepository;

    @GetMapping
    public ResponseEntity<List<Mantenimiento>> getAllMantenimientos() {
        List<Mantenimiento> mantenimientos = mantenimientoRepository.findAll();
        return ResponseEntity.ok(mantenimientos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mantenimiento> getMantenimientoById(@PathVariable Long id) {
        return mantenimientoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mantenimiento> createMantenimiento(@RequestBody Mantenimiento mantenimiento) {
        // Ensure ID is null for new entities
        mantenimiento.setId(null);
        
        // Ensure vehiculo exists
        if (mantenimiento.getVehiculo() != null) {
            Vehiculo vehiculo = vehiculoRepository.findById(mantenimiento.getVehiculo().getId())
                    .orElseThrow(() -> new RuntimeException("Vehiculo not found"));
            mantenimiento.setVehiculo(vehiculo);
        }
        
        Mantenimiento savedMantenimiento = mantenimientoRepository.save(mantenimiento);
        return ResponseEntity.ok(savedMantenimiento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mantenimiento> updateMantenimiento(@PathVariable Long id, @RequestBody Mantenimiento mantenimientoDetails) {
        return mantenimientoRepository.findById(id)
                .map(mantenimiento -> {
                    mantenimiento.setStartTime(mantenimientoDetails.getStartTime());
                    mantenimiento.setEndTime(mantenimientoDetails.getEndTime());
                    
                    // Update vehiculo if provided
                    if (mantenimientoDetails.getVehiculo() != null) {
                        Vehiculo vehiculo = vehiculoRepository.findById(mantenimientoDetails.getVehiculo().getId())
                                .orElseThrow(() -> new RuntimeException("Vehiculo not found"));
                        mantenimiento.setVehiculo(vehiculo);
                    }
                    
                    return ResponseEntity.ok(mantenimientoRepository.save(mantenimiento));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMantenimiento(@PathVariable Long id) {
        return mantenimientoRepository.findById(id)
                .map(mantenimiento -> {
                    mantenimientoRepository.delete(mantenimiento);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 