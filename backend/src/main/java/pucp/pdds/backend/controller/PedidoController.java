package pucp.pdds.backend.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pucp.pdds.backend.model.Pedido;
import pucp.pdds.backend.repository.PedidoRepository;

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
    @PostMapping("/importar")
    public ResponseEntity<String> importarPedidos(@RequestParam("file") MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<Pedido> pedidos = new ArrayList<>();

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.matches("ventas\\d{6}\\.txt")) {
                return ResponseEntity.badRequest().body("Nombre de archivo inválido. Debe ser ventasYYYYMM.txt");
            }

            int year = Integer.parseInt(filename.substring(6, 10));
            int month = Integer.parseInt(filename.substring(10, 12));
            int count = 0;
            if (year < 2020 || year > LocalDate.now().getYear() || month < 1 || month > 12) {
                return ResponseEntity.badRequest().body("Fecha inválida en el nombre del archivo.");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                Pedido pedido = parsearLineaPedido(line, year, month);
                if (pedido != null) {
                    pedido.setId(null); // asegurar que se cree nuevo
                    pedidoRepository.save(pedido); // aquí usamos save individualmente
                    count++;
                    // System.out.println("Insertando pedido: " + pedido.getCodigoCliente());

                }
            }

            //pedidoRepository.saveAll(pedidos);
            return ResponseEntity.ok(count + "Pedidos importados correctamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al importar: " + e.getMessage());
        }
    }

    private Pedido parsearLineaPedido(String linea, int year, int month) {
        try {
            // Ejemplo línea: 01d00h24m:16,13,c-198,3m3,4h
            String[] partes = linea.split(":");
            if (partes.length != 2) throw new RuntimeException("Formato inválido: " + linea);

            String tiempo = partes[0];  // "01d00h24m"
            String[] datos = partes[1].split(",");

            int day = Integer.parseInt(tiempo.substring(0, 2));
            int hour = Integer.parseInt(tiempo.substring(3, 5));
            int minute = Integer.parseInt(tiempo.substring(6, 8));

            int posicionX = Integer.parseInt(datos[0]);
            int posicionY = Integer.parseInt(datos[1]);
            String codigoCliente = datos[2]; // c-198
            int cantidadGLP = Integer.parseInt(datos[3].replace("m3", ""));
            int tiempoTolerancia = Integer.parseInt(datos[4].replace("h", ""));

            LocalDateTime fechaRegistro = LocalDateTime.of(year, month, day, hour, minute);

            Pedido pedido = new Pedido();
            pedido.setCodigoCliente(codigoCliente);
            pedido.setFechaRegistro(fechaRegistro);
            pedido.setPosicionX(posicionX);
            pedido.setPosicionY(posicionY);
            pedido.setCantidadGLP(cantidadGLP);
            pedido.setTiempoTolerancia(tiempoTolerancia);

            return pedido;
        } catch (Exception e) {
            return null;
        }
    }


}
