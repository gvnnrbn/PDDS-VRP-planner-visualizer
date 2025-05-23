package pucp.pdds.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pucp.pdds.backend.model.Pedido;
import pucp.pdds.backend.repository.PedidoRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        List<Map<String, Object>> pedidoMaps = pedidos.stream()
            .map(Pedido::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(pedidoMaps);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPedidoById(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(pedido -> ResponseEntity.ok(pedido.toMap()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Pedido> createPedido(@RequestBody Pedido pedido) {
        // Ensure ID is null for new entities
        pedido.setId(null);
        Pedido savedPedido = pedidoRepository.save(pedido);
        return ResponseEntity.ok(savedPedido);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Pedido> updatePedido(@PathVariable Long id, @RequestBody Pedido pedidoDetails) {
        return pedidoRepository.findById(id)
                .map(pedido -> {
                    pedido.setCodigoCliente(pedidoDetails.getCodigoCliente());
                    pedido.setFechaRegistro(pedidoDetails.getFechaRegistro());
                    pedido.setPosicionX(pedidoDetails.getPosicionX());
                    pedido.setPosicionY(pedidoDetails.getPosicionY());
                    pedido.setCantidadGLP(pedidoDetails.getCantidadGLP());
                    pedido.setTiempoTolerancia(pedidoDetails.getTiempoTolerancia());
                    Pedido updatedPedido = pedidoRepository.save(pedido);
                    return ResponseEntity.ok(updatedPedido);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePedido(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(pedido -> {
                    pedidoRepository.delete(pedido);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
