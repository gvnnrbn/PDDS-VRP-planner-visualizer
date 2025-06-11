package pucp.pdds.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SimulacionController {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    
    public Queue<String> simulationChunks = new LinkedList<>();
    public boolean chunkSent;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/simulation-start")
    public void iniciarSimulacion(@Payload String fechaInicioStr) {
      System.out.println("Iniciando simulacion con fecha: " + fechaInicioStr);
      try {
        LocalDateTime fechaInicio = LocalDateTime.parse(fechaInicioStr, DATE_FORMATTER);
        // obtener cola de planificaciones
        simulationChunks.clear();  
        chunkSent = false;
        simulationChunks.add("""
        {
  "bloqueos": [],
  "simulacion": [
    {
      "minuto": "08/05/2025 08:00",
      "pedidos": [
        {
          "idPedido": 1,
          "estado": "Asignado",
          "posX": 30,
          "posY": 30,
          "glp": 12
        }
      ],
      "vehiculos": [
        {
          "idVehiculo": 1,
          "tipo": "TA",
          "placa": "TA123",
          "posicionX": 5,
          "posicionY": 10,
          "estado": "Moviendo",
          "rutaActual": [
            {
              "posX": 5,
              "posY": 10
            },
            {
              "posX": 10,
              "posY": 15
            },
            {
              "posX": 20,
              "posY": 25
            },
            {
              "posX": 30,
              "posY": 30
            }
          ]
        }
      ]
    },
    {
      "minuto": "08/05/2025 08:01",
      "vehiculos": [
        {
          "idVehiculo": 1,
          "posicionX": 8,
          "posicionY": 12,
          "estado": "Moviendo"
        }
      ]
    },
    {
      "minuto": "08/05/2025 08:02",
      "vehiculos": [
        {
          "idVehiculo": 1,
          "posicionX": 12,
          "posicionY": 18,
          "estado": "Moviendo"
        }
      ]
    }
  ]
}
        """);

        // Chunk 2: Minutes 3-4 (Vehicle approaches destination)
        simulationChunks.add("""
        {
            "bloqueos": [],
            "simulacion": [
                {
                    "minuto": "08/05/2025 08:03",
                    "vehiculos": [
                        {
                            "idVehiculo": 1,
                            "posicionX": 20,
                            "posicionY": 25,
                            "estado": "Moviendo"
                        }
                    ]
                },
                {
                    "minuto": "08/05/2025 08:04",
                    "vehiculos": [
                        {
                            "idVehiculo": 1,
                            "posicionX": 28,
                            "posicionY": 29,
                            "estado": "Moviendo"
                        }
                    ]
                }
            ]
        }
        """);

        // Chunk 3: Minute 5 (Vehicle arrives and delivers)
        simulationChunks.add("""
        {
            "bloqueos": [],
            "simulacion": [
                {
                    "minuto": "08/05/2025 08:05",
                    "pedidos": [
                        {
                            "idPedido": 1,
                            "estado": "Entregado"
                        }
                    ],
                    "vehiculos": [
                        {
                            "idVehiculo": 1,
                            "posicionX": 30,
                            "posicionY": 30,
                            "estado": "Descargando",
                            "currGLP": 18 
                        }
                    ]
                }
            ]
        }
        """);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + fechaInicioStr);
        }
    }

    @MessageMapping("/request-chunk")
    public void handleChunkRequest() {
      if (simulationChunks.isEmpty()) {
          System.out.println("Chunk request recibido pero la simulación aún no ha sido iniciada.");
          return;
      }

      // if (!chunkSent) {
        boolean hasMoreChunks = sendNextChunk();

        if (!hasMoreChunks) {
            System.out.println("Sending COMPLETED status");
            messagingTemplate.convertAndSend("/topic/simulation-status", "COMPLETED");
        }
        else{
            System.out.println("More chunks pending. Waiting for request...");
        }

        // chunkSent = true;
      }
    // }

    private boolean sendNextChunk() {
        if (!simulationChunks.isEmpty()) {
            String chunk = simulationChunks.poll();
            System.out.println("Sending chunk... ");
            messagingTemplate.convertAndSend("/topic/simulation-data", chunk);
            
            // Return true if more chunks are available
            return !simulationChunks.isEmpty();
        }
        return false; // No more chunks available
    }

    @MessageMapping("/chunk-received")
    public void handleChunkReceived() {
        // chunkSent = false;
        System.out.println("Frontend confirmó recepción del chunk. Listo para enviar el siguiente.");
    }
}
