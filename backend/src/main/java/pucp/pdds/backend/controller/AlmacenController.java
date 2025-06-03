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

import pucp.pdds.backend.model.Almacen;
import pucp.pdds.backend.repository.AlmacenRepository;

@RestController
@RequestMapping("/api/almacenes")
public class AlmacenController {

    @Autowired
    private AlmacenRepository almacenRepository;

    @GetMapping
    public List<Almacen> getAll() {
        return almacenRepository.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Almacen> getById(@PathVariable Long id) {
        return almacenRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping
    public Almacen create(@RequestBody Almacen almacen) {
        return almacenRepository.save(almacen);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Almacen> update(@PathVariable Long id, @RequestBody Almacen details) {
        return almacenRepository.findById(id)
                .map(almacen -> {
                    almacen.setPosicionX(details.getPosicionX());
                    almacen.setPosicionY(details.getPosicionY());
                    almacen.setEsPrincipal(details.isEsPrincipal());
                    almacen.setCapacidadEfectivam3(details.getCapacidadEfectivam3());
                    almacen.setHorarioAbastecimiento(details.getHorarioAbastecimiento());
                    return ResponseEntity.ok(almacenRepository.save(almacen));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return almacenRepository.findById(id)
                .map(a -> {
                    almacenRepository.delete(a);
                    return ResponseEntity.ok().<Void>build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
