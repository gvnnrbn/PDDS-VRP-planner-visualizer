package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SimulacionSenderService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public void enviarSimulacionPorBatches(LocalDateTime fechaInicio) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        for (int minuto = 0; minuto < 3; minuto++) {
            int finalMinuto = minuto;

            scheduler.schedule(() -> {
                Map<String, Object> minutoInfo = new LinkedHashMap<>();
                minutoInfo.put("minuto", finalMinuto);

                minutoInfo.put("vehiculos", List.of(
                        Map.of(
                                "idVehiculo", 1,
                                "tipo", "TA",
                                "posicionX", 5 + finalMinuto,
                                "posicionY", 10,
                                "estado", "Entregando",
                                "accion", "moviendose",
                                "placa", "TA123",
                                "rutaActual", List.of(
                                        Map.of("posX", 5 + finalMinuto, "posY", 10),
                                        Map.of("posX", 6 + finalMinuto, "posY", 10)
                                )
                        )
                ));

                minutoInfo.put("pedidos", List.of(
                        Map.of(
                                "idPedido", 1,
                                "estado", "Pendiente",
                                "glp", 12,
                                "tiempoLimite", fechaInicio.plusHours(3).toString(),
                                "vehiculosAtendiendo", List.of(
                                        Map.of("placa", "TA123", "eta", fechaInicio.plusMinutes(10).toString())
                                ),
                                "posX", 30,
                                "posY", 30
                        )
                ));

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("fechaInicio", fechaInicio.format(DATE_FORMATTER));
                payload.put("simulacion", List.of(minutoInfo));
                payload.put("bloqueos", List.of());

                messagingTemplate.convertAndSend("/topic/simulacion-start", payload);
                System.out.println("Sent minute " + finalMinuto);
            }, minuto * 2, TimeUnit.SECONDS);
        }
    }

    public void enviarTest() {
        String payload = "Informacion de backend";

        messagingTemplate.convertAndSend("/topic/simulacion", payload);
    }
}



