package pucp.pdds.backend.seeder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pucp.pdds.backend.model.Pedido;
import pucp.pdds.backend.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class PedidoSeeder implements CommandLineRunner {
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Override
    public void run(String... args) throws Exception {
        pedidoRepository.deleteAll();
        
        // Generate test data based on the orders.csv format
        String[][] pedidos = {
            {"c-130", "2025-05-23 08:00", "10", "15", "300", "30"},
            {"c-131", "2025-05-23 08:15", "15", "20", "200", "25"},
            {"c-132", "2025-05-23 08:30", "20", "25", "400", "40"},
            {"c-133", "2025-05-23 08:45", "25", "30", "250", "35"},
            {"c-134", "2025-05-23 09:00", "30", "35", "350", "45"}
        };
        
        for (String[] pedidoData : pedidos) {
            Pedido pedido = new Pedido(
                pedidoData[0], // codigoCliente
                LocalDateTime.parse(pedidoData[1], DATE_FORMATTER), // fechaRegistro
                Integer.parseInt(pedidoData[2]), // posicionX
                Integer.parseInt(pedidoData[3]), // posicionY
                Integer.parseInt(pedidoData[4]), // cantidadGLP
                Integer.parseInt(pedidoData[5]) // tiempoTolerancia
            );
            pedidoRepository.save(pedido);
        }
        
        System.out.println("Pedidos seeded successfully! Created 5 test pedidos.");
    }
}
