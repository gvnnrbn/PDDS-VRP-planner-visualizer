package pucp.pdds.backend.controller;

// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SimulacionController {
    // private static final DateTimeFormatter DATE_FORMATTER = 
    //     DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    
    public Queue<String> simulationChunks = new LinkedList<>();
    public boolean chunkSent;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @MessageMapping("/simulation-start")
    public void iniciarSimulacion(@Payload String fechaInicioStr) {
        System.out.println("Iniciando simulación con fecha: " + fechaInicioStr);
        try {
            // LocalDateTime fechaInicio = LocalDateTime.parse(fechaInicioStr, DATE_FORMATTER);

            // Datos simulados por minuto
            String[] simulatedMinutes = new String[] {
              """
              {
                "bloqueos": [
                {
                  "idBloqueo": 1,
                  "fechaInicio": "2025-05-06T00:00:00",
                  "fechaFin": "2025-05-09T00:00:00",
                  "segmentos": [
                  { "posX": 50, "posY": 45 },
                  { "posX": 50, "posY": 40 },
                  { "posX": 44, "posY": 40 }
                  ]
                }
                ],
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
                    "placa": "TA001",
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
                  { "posX": 2, "posY": 2 },
                  { "posX": 3, "posY": 3 }
                  ]
                }
                ],
                "incidencias": [
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
                "almacenes": [
                {
                  "idAlmacen": 1,
                  "posicion": { "posX": 1, "posY": 1 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": true,
                  "wasVehicle": false
                },
                {
                  "idAlmacen": 2,
                  "posicion": { "posX": 10, "posY": 10 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": false,
                  "wasVehicle": false
                }
                ]
              }
              """,
              """
              {
                "bloqueos": [
                {
                  "idBloqueo": 1,
                  "fechaInicio": "2025-05-06T00:00:00",
                  "fechaFin": "2025-05-09T00:00:00",
                  "segmentos": [
                  { "posX": 50, "posY": 45 },
                  { "posX": 50, "posY": 40 },
                  { "posX": 44, "posY": 40 }
                  ]
                }
                ],
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
                  { "posX": 4, "posY": 4 },
                  { "posX": 5, "posY": 5 }
                  ]
                }
                ],
                "incidencias": [
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
                "almacenes": [
                {
                  "idAlmacen": 1,
                  "posicion": { "posX": 1, "posY": 1 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": true,
                  "wasVehicle": false
                }
                ]
              }
              """,
              """
              {
                "bloqueos": [
                {
                  "idBloqueo": 1,
                  "fechaInicio": "2025-05-06T00:00:00",
                  "fechaFin": "2025-05-09T00:00:00",
                  "segmentos": [
                  { "posX": 50, "posY": 45 },
                  { "posX": 50, "posY": 40 },
                  { "posX": 44, "posY": 40 }
                  ]
                }
                ],
                "minuto": "08/05/2025 08:02",
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
                    "placa": "TA001",
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
                "incidencias": [
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
                "almacenes": [
                {
                  "idAlmacen": 1,
                  "posicion": { "posX": 1, "posY": 1 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": true,
                  "wasVehicle": false
                },
                {
                  "idAlmacen": 2,
                  "posicion": { "posX": 5, "posY": 5 },
                  "currentGLP": 20,
                  "maxGLP": 20,
                  "isMain": false,
                  "wasVehicle": true
                }
                ]
              }
              """,
              """
              {
                "bloqueos": [
                {
                  "idBloqueo": 1,
                  "fechaInicio": "2025-05-06T00:00:00",
                  "fechaFin": "2025-05-09T00:00:00",
                  "segmentos": [
                  { "posX": 50, "posY": 45 },
                  { "posX": 50, "posY": 40 },
                  { "posX": 44, "posY": 40 }
                  ]
                }
                ],
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
                    "placa": "TA001",
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
                "incidencias": [
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
                "almacenes": [
                {
                  "idAlmacen": 1,
                  "posicion": { "posX": 1, "posY": 1 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": true,
                  "wasVehicle": false
                },
                {
                  "idAlmacen": 2,
                  "posicion": { "posX": 5, "posY": 5 },
                  "currentGLP": 20,
                  "maxGLP": 20,
                  "isMain": false,
                  "wasVehicle": true
                }
                ]
              }
              """,
              """
              {
                "bloqueos": [
                {
                  "idBloqueo": 1,
                  "fechaInicio": "2025-05-06T00:00:00",
                  "fechaFin": "2025-05-09T00:00:00",
                  "segmentos": [
                  { "posX": 50, "posY": 45 },
                  { "posX": 50, "posY": 40 },
                  { "posX": 44, "posY": 40 }
                  ]
                }
                ],
                "minuto": "08/05/2025 08:04",
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
                  { "posX": 4, "posY": 4 },
                  { "posX": 3, "posY": 3 }
                  ]
                }
                ],
                "incidencias": [
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
                "almacenes": [
                {
                  "idAlmacen": 1,
                  "posicion": { "posX": 1, "posY": 1 },
                  "currentGLP": 30,
                  "maxGLP": 30,
                  "isMain": true,
                  "wasVehicle": false
                }
                ]
              }
              """
            };

            for (int i = 0; i < simulatedMinutes.length; i++) {
                int finalI = i;
                scheduler.schedule(() -> {
                    String json = simulatedMinutes[finalI];
                    System.out.println("Enviando minuto " + finalI + ": ");
                    messagingTemplate.convertAndSend("/topic/simulation", json);
                }, i * 500L, TimeUnit.MILLISECONDS); // 500ms por minuto simulado
            }

        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + fechaInicioStr);
        }
    }
}
