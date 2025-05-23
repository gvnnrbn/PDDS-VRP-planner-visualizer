package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pucp.pdds.backend.dto.TipoVehiculoDto;
import pucp.pdds.backend.service.TipoVehiculoService;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-vehiculos")
public class TipoVehiculoController {
    
    @Autowired
    private TipoVehiculoService tipoVehiculoService;
    
    @GetMapping
    public ResponseEntity<List<TipoVehiculoDto>> getAllTipos() {
        return ResponseEntity.ok(tipoVehiculoService.getAllTipos());
    }
    
    @GetMapping("/{tipoId}")
    public ResponseEntity<TipoVehiculoDto> getTipoById(@PathVariable String tipoId) {
        return ResponseEntity.ok(tipoVehiculoService.getTipoById(tipoId));
    }
}
