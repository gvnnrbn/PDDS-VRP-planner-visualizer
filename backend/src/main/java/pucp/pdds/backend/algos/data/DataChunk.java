package pucp.pdds.backend.algos.data;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.utils.Time;

public class DataChunk {
    public LocalDateTime fechaInicio;
    public List<Bloqueo> bloqueos;
    public List<SimulacionMinuto> simulacion;

    public DataChunk() {
        this.fechaInicio = LocalDateTime.now();
        this.bloqueos = new ArrayList<>();
        this.simulacion = new ArrayList<>();
    }

    // For backward compatibility
    public List<Node> deliveryNodes;
    public List<Node> refillNodes;

    public static class Bloqueo {
        public int idBloqueo;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
        public List<Segmento> segmentos;

        public Bloqueo(int idBloqueo, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
            this.idBloqueo = idBloqueo;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.segmentos = new ArrayList<>();
        }

        public void setSegmentos(List<Segmento> segmentos) {
            this.segmentos = segmentos;
        }

        public static class Segmento {
            public int posX;
            public int posY;

            public Segmento(int posX, int posY) {
                this.posX = posX;
                this.posY = posY;
            }
        }
    }

    public static class SimulacionMinuto {
        public String minuto;
        public List<Almacen> almacenes;
        public List<Vehiculo> vehiculos;
        public List<Pedido> pedidos;
        public List<Incidencia> incidencias;
        public List<Mantenimiento> mantenimientos;

        public SimulacionMinuto(String minuto) {
            this.minuto = minuto;
            this.almacenes = new ArrayList<>();
            this.vehiculos = new ArrayList<>();
            this.pedidos = new ArrayList<>();
            this.incidencias = new ArrayList<>();
            this.mantenimientos = new ArrayList<>();
        }

        public void setAlmacenes(List<Almacen> almacenes) {
            this.almacenes = almacenes;
        }

        public void setVehiculos(List<Vehiculo> vehiculos) {
            this.vehiculos = vehiculos;
        }

        public void setPedidos(List<Pedido> pedidos) {
            this.pedidos = pedidos;
        }

        public void setIncidencias(List<Incidencia> incidencias) {
            this.incidencias = incidencias;
        }

        public void setMantenimientos(List<Mantenimiento> mantenimientos) {
            this.mantenimientos = mantenimientos;
        }
    }

    public static class Posicion {
        public float posX;
        public float posY;

        public Posicion(float posX, float posY) {
            this.posX = posX;
            this.posY = posY;
        }
    }

    public static class Almacen {
        public int idAlmacen;
        public Posicion posicion;
        public int currentGLP;
        public int maxGLP;
        public boolean isMain;
        public boolean wasVehicle;

        public Almacen(int idAlmacen, int posX, int posY, int currentGLP, int maxGLP, boolean isMain, boolean wasVehicle) {
            this.idAlmacen = idAlmacen;
            this.posicion = new Posicion(posX, posY);
            this.currentGLP = currentGLP;
            this.maxGLP = maxGLP;
            this.isMain = isMain;
            this.wasVehicle = wasVehicle;
        }
    }

    public static class Vehiculo {
        public int idVehiculo;
        public String estado;
        public String eta;
        public String tipo;
        public int combustible;
        public int maxCombustible;
        public int currGLP;
        public int maxGLP;
        public String placa;
        public int idPedido;
        public int posicionX;
        public int posicionY;
        public List<Posicion> rutaActual;

        public Vehiculo(int idVehiculo, String estado, String eta, String tipo, int combustible, int maxCombustible, int currGLP, int maxGLP, String placa, int idPedido, int posicionX, int posicionY) {
            this.idVehiculo = idVehiculo;
            this.estado = estado;
            this.eta = eta;
            this.tipo = tipo;
            this.combustible = combustible;
            this.maxCombustible = maxCombustible;
            this.currGLP = currGLP;
            this.maxGLP = maxGLP;
            this.placa = placa;
            this.idPedido = idPedido;
            this.posicionX = posicionX;
            this.posicionY = posicionY;
            this.rutaActual = new ArrayList<>();
        }

        public void setRutaActual(List<Posicion> rutaActual) {
            this.rutaActual = rutaActual;
        }
    }

    public static class Pedido {
        public int idPedido;
        public String estado;
        public int glp;
        public String fechaLimite;
        public List<VehiculoAtendiendo> vehiculosAtendiendo;
        public int posX;
        public int posY;

        public Pedido(int idPedido, String estado, int glp, String fechaLimite, int posX, int posY) {
            this.idPedido = idPedido;
            this.estado = estado;
            this.glp = glp;
            this.fechaLimite = fechaLimite;
            this.posX = posX;
            this.posY = posY;
            this.vehiculosAtendiendo = new ArrayList<>();
        }

        public void setVehiculosAtendiendo(List<VehiculoAtendiendo> vehiculosAtendiendo) {
            this.vehiculosAtendiendo = vehiculosAtendiendo;
        }
    }

    public static class VehiculoAtendiendo {
        public String placa;
        public String eta;

        public VehiculoAtendiendo(String placa, String eta) {
            this.placa = placa;
            this.eta = eta;
        }
    }

    public static class Incidencia {
        public int idIncidencia;
        public String fechaInicio;
        public String fechaFin;
        public String turno;
        public String tipo;
        public String placa;
        public String estado;

        public Incidencia(int idIncidencia, String fechaInicio, String fechaFin, String turno, String tipo, String placa, String estado) {
            this.idIncidencia = idIncidencia;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.turno = turno;
            this.tipo = tipo;
            this.placa = placa;
            this.estado = estado;
        }
    }

    public static class Mantenimiento {
        public String idMantenimiento;
        public VehiculoMantenimiento vehiculo;
        public String estado;
        public String fechaInicio;
        public String fechaFin;

        public Mantenimiento(String idMantenimiento, VehiculoMantenimiento vehiculo, String estado, String fechaInicio, String fechaFin) {
            this.idMantenimiento = idMantenimiento;
            this.vehiculo = vehiculo;
            this.estado = estado;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
        }
    }

    public static class VehiculoMantenimiento {
        public String placa;
        public String tipo;

        public VehiculoMantenimiento(String placa, String tipo) {
            this.placa = placa;
            this.tipo = tipo;
        }
    }

    // Method to generate JSON
    public String toJson() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
        return gson.toJson(this);
    }

    // LocalDateTime adapter for Gson
    public static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    // Getters and setters for backward compatibility
    public List<Node> getDeliveryNodes() {
        return deliveryNodes;
    }

    public void setDeliveryNodes(List<Node> deliveryNodes) {
        this.deliveryNodes = deliveryNodes;
    }

    public List<Node> getRefillNodes() {
        return refillNodes;
    }

    public void setRefillNodes(List<Node> refillNodes) {
        this.refillNodes = refillNodes;
    }

    // New getters and setters
    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public List<Bloqueo> getBloqueos() {
        return bloqueos;
    }

    public void setBloqueos(List<Bloqueo> bloqueos) {
        this.bloqueos = bloqueos;
    }

    public List<SimulacionMinuto> getSimulacion() {
        return simulacion;
    }

    public void setSimulacion(List<SimulacionMinuto> simulacion) {
        this.simulacion = simulacion;
    }

    public static java.util.Map<String, Double> convertIndicatorsToDataChunk(Indicator indicators) {

        java.util.Map<String, Double> activeIndicators = java.util.stream.Stream.of(
                new java.util.AbstractMap.SimpleEntry<>("fuelCounterTA", indicators.getFuelCounterTA()),
                new java.util.AbstractMap.SimpleEntry<>("fuelCounterTB", indicators.getFuelCounterTB()),
                new java.util.AbstractMap.SimpleEntry<>("fuelCounterTC", indicators.getFuelCounterTC()),
                new java.util.AbstractMap.SimpleEntry<>("fuelCounterTD", indicators.getFuelCounterTD()),
                new java.util.AbstractMap.SimpleEntry<>("fuelCounterTotal", indicators.getFuelCounterTotal()),
                new java.util.AbstractMap.SimpleEntry<>("glpFilledNorth", indicators.getGlpFilledNorth()),
                new java.util.AbstractMap.SimpleEntry<>("glpFilledEast", indicators.getGlpFilledEast()),
                new java.util.AbstractMap.SimpleEntry<>("glpFilledMain", indicators.getGlpFilledMain()),
                new java.util.AbstractMap.SimpleEntry<>("glpFilledTotal", indicators.getGlpFilledTotal()),
                new java.util.AbstractMap.SimpleEntry<>("meanDeliveryTime", indicators.getMeanDeliveryTime()),
                new java.util.AbstractMap.SimpleEntry<>("completedOrders", (double) indicators.getCompletedOrders()),
                new java.util.AbstractMap.SimpleEntry<>("totalOrders", (double) indicators.getTotalOrders())
        ).collect(java.util.stream.Collectors.toMap(
                java.util.Map.Entry::getKey,
                java.util.Map.Entry::getValue
        ));

        return activeIndicators;
    }

    public static List<Bloqueo> convertBlockagesToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerBlockage> activeBlockages) {
        return activeBlockages.stream()
            .map(blockage -> {
                Bloqueo bloqueo = new Bloqueo(
                    blockage.id,
                    blockage.startTime.toLocalDateTime(),
                    blockage.endTime.toLocalDateTime()
                );
                // Add segments if they exist
                if (blockage.vertices != null) {
                    List<Bloqueo.Segmento> segmentos = blockage.vertices.stream()
                        .map(vertex -> new Bloqueo.Segmento(
                            (int)vertex.x,
                            (int)vertex.y
                        ))
                        .collect(java.util.stream.Collectors.toList());
                    bloqueo.setSegmentos(segmentos);
                }
                return bloqueo;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Vehiculo> convertVehiclesToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerVehicle> vehicles, java.util.Map<Integer, List<pucp.pdds.backend.algos.algorithm.Node>> routes) {
        return vehicles.stream()
            .map(vehicle -> {
                String estado = vehicle.state != null ? vehicle.state.toString() : "-";
                String eta = "-"; // No eta field in PlannerVehicle
                int idPedido = 0; // No currentOrderId field in PlannerVehicle
                Vehiculo vehiculo = new Vehiculo(
                    vehicle.id,
                    estado,
                    eta,
                    vehicle.type,
                    (int)Math.max(25, vehicle.currentFuel),
                    vehicle.maxFuel,
                    (int)Math.max((1 + (int)(Math.random() * 5)), vehicle.currentGLP),
                    vehicle.maxGLP,
                    vehicle.plaque,
                    idPedido,
                    (int)vehicle.position.x,
                    (int)vehicle.position.y
                );
                if (vehicle.currentPath != null) {
                    List<Posicion> rutaActual = vehicle.currentPath.stream()
                        .map(point -> new Posicion((float)point.x, (float)point.y))
                        .collect(java.util.stream.Collectors.toList());
                    vehiculo.setRutaActual(rutaActual);
                }
                return vehiculo;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Almacen> convertWarehousesToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerWarehouse> warehouses) {
        return warehouses.stream()
            .map(warehouse -> new Almacen(
                warehouse.id,
                (int)warehouse.position.x,
                (int)warehouse.position.y,
                (int)Math.max(0, warehouse.currentGLP),
                warehouse.maxGLP,
                warehouse.isMain,
                warehouse.wasVehicle
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Pedido> convertOrdersToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerOrder> orders, 
            List<pucp.pdds.backend.algos.entities.PlannerVehicle> activeVehicles,
            java.util.Map<Integer, List<pucp.pdds.backend.algos.algorithm.Node>> routes,
            pucp.pdds.backend.algos.utils.Time currTime) {
        return orders.stream()
                .filter(o -> !(o.deadline.isBefore(currTime) && o.amountGLP > 0))
            .sorted((o1, o2) -> {
                boolean isO1Delivered = o1.isDelivered();
                boolean isO2Delivered = o2.isDelivered();

                if (isO1Delivered && !isO2Delivered) {
                    return 1;
                }
                if (!isO1Delivered && isO2Delivered) {
                    return -1;
                }

                if (o1.arrivalTime == null && o2.arrivalTime != null) {
                    return 1;
                }
                if (o1.arrivalTime != null && o2.arrivalTime == null) {
                    return -1;
                }
                if (o1.arrivalTime == null && o2.arrivalTime == null) {
                    return 0;
                }
                
                return o1.arrivalTime.compareTo(o2.arrivalTime);
            })
            .map(order -> {
                Pedido pedido = new Pedido(
                    order.id,
                    order.isDelivered() ? "Completado" : "Pendiente",
                    order.amountGLP,
                    order.deadline.toString(), // now fechaLimite
                    (int)order.position.x,
                    (int)order.position.y
                );
                // Map attending vehicles if they exist
                List<VehiculoAtendiendo> vehiculosAtendiendo = new ArrayList<>();
                for (pucp.pdds.backend.algos.entities.PlannerVehicle vehicle : activeVehicles) {
                    if (vehicle.currentPath != null && !vehicle.currentPath.isEmpty()) {
                        if (routes.containsKey(vehicle.id) && vehicle.nextNodeIndex < routes.get(vehicle.id).size()) {
                            pucp.pdds.backend.algos.algorithm.Node nextNode = routes.get(vehicle.id).get(vehicle.nextNodeIndex);
                            if (nextNode instanceof pucp.pdds.backend.algos.algorithm.OrderDeliverNode && 
                                ((pucp.pdds.backend.algos.algorithm.OrderDeliverNode) nextNode).order.id == order.id) {
                                vehiculosAtendiendo.add(new VehiculoAtendiendo(
                                    vehicle.plaque,
                                    currTime.toString()
                                ));
                            }
                        }
                    }
                }
                pedido.setVehiculosAtendiendo(vehiculosAtendiendo);
                return pedido;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Incidencia> convertIncidentsToDataChunk(
        List<PlannerFailure> failures,
        Time currTime
    ) {
        List<Incidencia> incidencias = new ArrayList<>();
        // Add failures as incidents
		for(PlannerFailure f : failures){
            String failureState = "";
            Time reincorporationTime = null;
			if(f.timeOccuredOn!=null){
                // if(f.timeOccuredOn.isAfterOrAt(currTime)){
                    switch (f.type) {
                        case Ti1:
                            reincorporationTime = new Time(f.timeOccuredOn).addMinutes(120);
                            break;
                        case Ti2:						
                            // Determine reincorporation time based on the shift
                            switch (f.shiftOccurredOn) {
                                case T1:  // 00:00-08:00
                                    // Available in T3 of same day
                                    reincorporationTime = new Time(
                                        f.timeOccuredOn.getYear(),
                                        f.timeOccuredOn.getMonth(),
                                        f.timeOccuredOn.getDay(),
                                        16,  // T3 starts at 16:00
                                        0
                                    );
                                    break;
                                case T2:  // 08:00-16:00
                                    // Available in T1 of next day
                                    reincorporationTime = new Time(
                                        f.timeOccuredOn.getYear(),
                                        f.timeOccuredOn.getMonth(),
                                        f.timeOccuredOn.getDay() + 1,
                                        0,  // T1 starts at 00:00
                                        0
                                    );
                                    break;
                                case T3:  // 16:00-24:00
                                    // Available in T2 of next day
                                    reincorporationTime = new Time(
                                        f.timeOccuredOn.getYear(),
                                        f.timeOccuredOn.getMonth(),
                                        f.timeOccuredOn.getDay() + 1,
                                        8,  // T2 starts at 08:00
                                        0
                                    );
                                    break;
                                default:
                                    throw new RuntimeException("Invalid shift");
                            }
                            break;
                        case Ti3:
                            reincorporationTime = new Time(
                                f.timeOccuredOn.getYear(),
                                f.timeOccuredOn.getMonth(),
                                f.timeOccuredOn.getDay() + 2,
                                0,
                                0
                            );
                            break;
                        }
                        failureState = currTime.isAfterOrAt(f.timeOccuredOn) & reincorporationTime.isAfterOrAt(currTime) ? "EN CURSO" 
                        : currTime.isBefore(f.timeOccuredOn) ? "INMINENTE" : "RESUELTA";
                // } else {
                //     failureState = "INMINENTE";
                // }
			}
			else{
				failureState = "INMINENTE";
			}
            incidencias.add(new Incidencia(
                f.id,
                f.timeOccuredOn != null ? f.timeOccuredOn.toString() : "",
                reincorporationTime != null ? reincorporationTime.toString() : "",
                f.shiftOccurredOn != null ? f.shiftOccurredOn.toString() : "",
                f.type != null ? f.type.toString() : "",
                f.vehiclePlaque,
                failureState
            ));
		}
        return incidencias;
    }

    public static List<Mantenimiento> convertMaintenancesToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerMaintenance> maintenances) {
        List<Mantenimiento> result = new ArrayList<>();
        for (pucp.pdds.backend.algos.entities.PlannerMaintenance m : maintenances) {
            VehiculoMantenimiento vehiculo = new VehiculoMantenimiento(m.vehiclePlaque, "-"); // No vehicleType field
            result.add(new Mantenimiento(
                String.valueOf(m.id),
                vehiculo,
                "Programado", // No state field
                m.startDate != null ? m.startDate.toString() : "",
                m.endDate != null ? m.endDate.toString() : ""
            ));
        }
        return result;
    }
}
