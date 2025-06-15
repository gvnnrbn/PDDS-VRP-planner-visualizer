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
            "estado": "Pendiente",
            "glp": 10,
            "posX": 7,
            "posY": 5,
            "fechaLimite": "12/06/2025 19:00",
            "vehiculosAtendiendo": [
              {
                "placa":"TA001",
                "eta": "12/06/2025 12:00"
              }
            ]
        }
      ],
      "vehiculos": [
        {
          "idVehiculo": 1,
          "estado": "En Ruta",
          "eta": "12/06/2025 10:00",
          "tipo": "TA",
          "combustible": 10,
          "maxCombustible": 10,
          "currGLP": 20,
          "maxGLP": 20,
          "placa": "TA001",
          "posicionX": 1,
          "posicionY": 1,
          "idPedido": 1,
          "rutaActual": [
            {
              "posX": 2, 
              "posY": 2
            },
            {
              "posX": 3, 
              "posY": 3
            }
          ] 
        }
      ],
      "incidencias":[
        {
          "idIncidencia": 1,
          "fechaInicio": "-",
          "fechaFin": "-",
          "turno": "T1",
          "tipo": "TI1",
          "placa": "TA001",
          "estado": "Estimada"
        }
      ],
      "mantenimientos": [
        {
          "idMantenimiento": "1",
          "vehiculo": {
              "placa": "TA001",
              "tipo": "TA"
          },
          "estado": "Programado",
          "fechaInicio": "08/05/2025 20:00",
          "fechaFin": "09/05/2025 20:00"
        }
      ],
      "almacenes":[
        {
          "idAlmacen": 1,
          "posicion": {
            "posX":1, 
            "posY":1
          },
          "currentGLP": 30,
          "maxGLP": 30,
          "isMain": true,
          "wasVehicle": false
        },
        {
          "idAlmacen": 2,
          "posicion": {
            "posX":10, 
            "posY":10
          },
          "currentGLP": 30,
          "maxGLP": 30,
          "isMain": false,
          "wasVehicle": false
        }
      ]
    },
    {
      "minuto": "08/05/2025 08:01",
      "vehiculos": [
        {
          "idVehiculo": 1,
          "estado": "En Ruta",
          "eta": "12/06/2025 10:00",
          "tipo": "TA",
          "combustible": 10,
          "maxCombustible": 10,
          "currGLP": 20,
          "maxGLP": 20,
          "placa": "TA001",
          "posicionX": 3,
          "posicionY": 3,
          "idPedido": 1,
          "rutaActual": [
            {
              "posX": 4, 
              "posY": 4
            },
            {
              "posX": 5, 
              "posY": 5
            }
          ] 
        }
      ],
      "incidencias":[
        {
          "idIncidencia": 1,
          "fechaInicio": "-",
          "fechaFin": "-",
          "turno": "T1",
          "tipo": "TI1",
          "placa": "TA001",
          "estado": "Estimada"
        }
      ],
      "mantenimientos": [
        {
          "idMantenimiento": "1",
            "vehiculo": {
                "placa": "TA001",
                "tipo": "TA"
            },
            "estado": "Estimada",
            "fechaInicio": "08/05/2025 20:00",
            "fechaFin": "09/05/2025 20:00"
        }
      ],
      "almacenes":[
        {
          "idAlmacen": 1,
          "posicion": {
            "posX":1, 
            "posY":1
          },
          "currentGLP": 30,
          "maxGLP": 30,
          "isMain": true,
          "wasVehicle": false
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
                    "pedidos": [
                        {
                            "idPedido": 1,
                            "estado": "Pendiente",
                            "glp": 10,
                            "posX": 7,
                            "posY": 5,
                            "fechaLimite": "12/06/2025 19:00",
                            "vehiculosAtendiendo": [
                                {
                                    "placa":"TA001",
                                    "eta": "12/06/2025 12:00"
                                }
                            ]
                        }
                    ],
                    "vehiculos": [
                        {
                            "idVehiculo": 1,
                            "estado": "Averiado",
                            "eta": "12/06/2025 12:00",
                            "tipo": "TA",
                            "combustible": 9,
                            "maxCombustible": 10,
                            "currGLP": 20,
                            "maxGLP": 20,
                            "placa": "TA001",
                            "posicionX": 5,
                            "posicionY": 5,
                            "idPedido": 1,
                            "rutaActual": [] 
                        }
                    ],
                    "incidencias":[
                        {
                            "idIncidencia": 1,
                            "fechaInicio": "08/05/2025 08:03",
                            "fechaFin": "08/05/2025 12:03",
                            "turno": "T1",
                            "tipo": "TI1",
                            "placa": "TA001",
                            "estado": "En Curso"
                        }
                    ],
                    "mantenimientos": [
                        {
                            "idMantenimiento": "1",
                            "vehiculo": {
                                "placa": "TA001",
                                "tipo": "TA"
                            },
                            "estado": "Programado",
                            "fechaInicio": "08/05/2025 20:00",
                            "fechaFin": "09/05/2025 20:00"
                        }
                    ],
                    "almacenes":[
                        {
                            "idAlmacen": 1,
                            "posicion": {
                                "posX":1, 
                                "posY":1
                            },
                            "currentGLP": 30,
                            "maxGLP": 30,
                            "isMain": true,
                            "wasVehicle": false
                        },
                        {
                            "idAlmacen": 2,
                            "posicion": {
                                "posX":5, 
                                "posY":5
                            },
                            "currentGLP": 20,
                            "maxGLP": 20,
                            "isMain": false,
                            "wasVehicle": true
                        }
                    ]
                },
                {
                    "minuto": "08/05/2025 08:04",
                    "pedidos": [
                        {
                            "idPedido": 1,
                            "estado": "Pendiente",
                            "glp": 10,
                            "posX": 7,
                            "posY": 5,
                            "fechaLimite": "12/06/2025 19:00",
                            "vehiculosAtendiendo": [
                                {
                                    "placa":"TA001",
                                    "eta": "12/06/2025 12:00"
                                }
                            ]
                        }
                    ],
                    "vehiculos": [
                        {
                            "idVehiculo": 1,
                            "estado": "Averiado",
                            "eta": "12/06/2025 12:00",
                            "tipo": "TA",
                            "combustible": 9,
                            "maxCombustible": 10,
                            "currGLP": 20,
                            "maxGLP": 20,
                            "placa": "TA001",
                            "posicionX": 5,
                            "posicionY": 5,
                            "idPedido": 1,
                            "rutaActual": [] 
                        }
                    ],
                    "incidencias":[
                        {
                            "idIncidencia": 1,
                            "fechaInicio": "08/05/2025 08:03",
                            "fechaFin": "08/05/2025 12:03",
                            "turno": "T1",
                            "tipo": "TI1",
                            "placa": "TA001",
                            "estado": "En Curso"
                        }
                    ],
                    "mantenimientos": [
                        {
                            "idMantenimiento": "1",
                            "vehiculo": {
                                "placa": "TA001",
                                "tipo": "TA"
                            },
                            "estado": "Programado",
                            "fechaInicio": "08/05/2025 20:00",
                            "fechaFin": "09/05/2025 20:00"
                        }
                    ],
                    "almacenes":[
                        {
                            "idAlmacen": 1,
                            "posicion": {
                                "posX":1, 
                                "posY":1
                            },
                            "currentGLP": 30,
                            "maxGLP": 30,
                            "isMain": true,
                            "wasVehicle": false
                        },
                        {
                            "idAlmacen": 2,
                            "posicion": {
                                "posX":5, 
                                "posY":5
                            },
                            "currentGLP": 20,
                            "maxGLP": 20,
                            "isMain": false,
                            "wasVehicle": true
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
                "estado": "Pendiente",
                "glp": 10,
                "posX": 7,
                "posY": 5,
                "fechaLimite": "12/06/2025 19:00",
                "vehiculosAtendiendo": []
            }
              ],
              "vehiculos": [
            {
                "idVehiculo": 1,
                "estado": "Averiado",
                "eta": "-",
                "tipo": "TA",
                "combustible": 9,
                "maxCombustible": 10,
                "currGLP": 20,
                "maxGLP": 20,
                "placa": "TA001",
                "posicionX": 5,
                "posicionY": 5,
                "idPedido": 0,
                "rutaActual": [
              {
                  "posX": 4, 
                  "posY": 4
              },
              {
                  "posX": 3, 
                  "posY": 3
              }
                ] 
            }
              ],
              "incidencias":[
            {
                "idIncidencia": 1,
                "fechaInicio": "08/05/2025 08:03",
                "fechaFin": "08/05/2025 12:03",
                "turno": "T1",
                "tipo": "TI1",
                "placa": "TA001",
                "estado": "En Curso"
            }
              ],
              "mantenimientos": [
            {
                "idMantenimiento": "1",
                "vehiculo": {
              "placa": "TA001",
              "tipo": "TA"
                },
                "estado": "Programado",
                "fechaInicio": "08/05/2025 20:00",
                "fechaFin": "09/05/2025 20:00"
            }
              ],
              "almacenes":[
            {
                "idAlmacen": 1,
                "posicion": {
              "posX":1, 
              "posY":1
                },
                "currentGLP": 30,
                "maxGLP": 30,
                "isMain": true,
                "wasVehicle": false
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
