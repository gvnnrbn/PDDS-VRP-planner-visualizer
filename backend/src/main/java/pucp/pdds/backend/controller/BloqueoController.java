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

import pucp.pdds.backend.model.Bloqueo;
import pucp.pdds.backend.repository.BloqueoRepository;

@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    @Autowired
    private BloqueoRepository bloqueoRepository;

    @GetMapping
    public ResponseEntity<List<Bloqueo>> getAllBloqueos() {
        List<Bloqueo> bloqueos = bloqueoRepository.findAll();
        return ResponseEntity.ok(bloqueos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bloqueo> getBloqueoById(@PathVariable Long id) {
        return bloqueoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Bloqueo> createBloqueo(@RequestBody Bloqueo bloqueo) {
        // Ensure ID is null for new entities
        bloqueo.setId(null);
        
        Bloqueo savedBloqueo = bloqueoRepository.save(bloqueo);
        return ResponseEntity.ok(savedBloqueo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bloqueo> updateBloqueo(@PathVariable Long id, @RequestBody Bloqueo bloqueoDetails) {
        return bloqueoRepository.findById(id)
                .map(bloqueo -> {
                    bloqueo.setStartTime(bloqueoDetails.getStartTime());
                    bloqueo.setEndTime(bloqueoDetails.getEndTime());
                    bloqueo.setVerticesJson(bloqueoDetails.getVerticesJson());
                    
                    return ResponseEntity.ok(bloqueoRepository.save(bloqueo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBloqueo(@PathVariable Long id) {
        return bloqueoRepository.findById(id)
                .map(bloqueo -> {
                    bloqueoRepository.delete(bloqueo);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 