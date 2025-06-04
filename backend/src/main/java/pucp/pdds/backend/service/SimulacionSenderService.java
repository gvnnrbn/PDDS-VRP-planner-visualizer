package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class SimulacionSenderService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void enviarSimulacionPorBatches() {
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
                                "tiempoLimite", LocalDateTime.now().plusHours(3).toString(),
                                "vehiculosAtendiendo", List.of(
                                        Map.of("placa", "TA123", "eta", LocalDateTime.now().plusMinutes(10).toString())
                                ),
                                "posX", 30,
                                "posY", 30
                        )
                ));

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("fechaInicio", LocalDateTime.now().toString());
                payload.put("simulacion", List.of(minutoInfo));
                payload.put("bloqueos", List.of());

                messagingTemplate.convertAndSend("/topic/simulacion", payload);
            }, minuto * 2, TimeUnit.SECONDS); // 2 segundos de delay entre mensajes
        }
    }
    public void enviarTest() {
        // Simula los datos de 3 minutos (en realidad puede ser hasta 75)
        List<Map<String, Object>> simulacion = new ArrayList<>();

        for (int minuto = 0; minuto < 3; minuto++) {
            Map<String, Object> minutoInfo = new LinkedHashMap<>();
            minutoInfo.put("minuto", minuto);

            minutoInfo.put("vehiculos", List.of(
                    Map.of(
                            "idVehiculo", 1,
                            "tipo", "TA",
                            "posicionX", 5 + minuto,
                            "posicionY", 10,
                            "estado", "Entregando",
                            "accion", "moviendose",
                            "placa", "TA123",
                            "rutaActual", List.of(
                                    Map.of("posX", 5 + minuto, "posY", 10),
                                    Map.of("posX", 6 + minuto, "posY", 10)
                            )
                    )
            ));

            minutoInfo.put("pedidos", List.of(
                    Map.of(
                            "idPedido", 1,
                            "estado", "Pendiente",
                            "glp", 12,
                            "tiempoLimite", LocalDateTime.now().plusHours(3).toString(),
                            "vehiculosAtendiendo", List.of(
                                    Map.of("placa", "TA123", "eta", LocalDateTime.now().plusMinutes(10).toString())
                            ),
                            "posX", 30,
                            "posY", 30
                    )
            ));

            simulacion.add(minutoInfo);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fechaInicio", LocalDateTime.now().toString());
        payload.put("simulacion", simulacion);
        payload.put("bloqueos", List.of()); // vacío por simplicidad

        messagingTemplate.convertAndSend("/topic/simulacion", payload);
    }
}



