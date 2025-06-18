package pucp.pdds.backend.algos.data;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pucp.pdds.backend.algos.algorithm.Node;

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
        public int minuto;
        public List<Almacen> almacenes;
        public List<Vehiculo> vehiculos;
        public List<Pedido> pedidos;
        public List<Incidencia> incidencias;

        public SimulacionMinuto(int minuto) {
            this.minuto = minuto;
            this.almacenes = new ArrayList<>();
            this.vehiculos = new ArrayList<>();
            this.pedidos = new ArrayList<>();
            this.incidencias = new ArrayList<>();
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
    }

    public static class Almacen {
        public int idAlmacen;
        public int posX;
        public int posY;
        public int currentGLP;
        public int maxGLP;
        public boolean isMain;
        public boolean wasVehicle;

        public Almacen(int idAlmacen, int posX, int posY, int currentGLP, int maxGLP, boolean isMain, boolean wasVehicle) {
            this.idAlmacen = idAlmacen;
            this.posX = posX;
            this.posY = posY;
            this.currentGLP = currentGLP;
            this.maxGLP = maxGLP;
            this.isMain = isMain;
            this.wasVehicle = wasVehicle;
        }
    }

    public static class Vehiculo {
        public int idVehiculo;
        public String tipo;
        public int combustible;
        public int maxCombustible;
        public int maxGLP;
        public int currGLP;
        public String placa;
        public int posicionX;
        public int posicionY;
        public List<RutaPunto> rutaActual;

        public Vehiculo(int idVehiculo, String tipo, int combustible, int maxCombustible, int maxGLP, int currGLP, String placa, int posicionX, int posicionY) {
            this.idVehiculo = idVehiculo;
            this.tipo = tipo;
            this.combustible = combustible;
            this.maxCombustible = maxCombustible;
            this.maxGLP = maxGLP;
            this.currGLP = currGLP;
            this.placa = placa;
            this.posicionX = posicionX;
            this.posicionY = posicionY;
            this.rutaActual = new ArrayList<>();
        }

        public void setRutaActual(List<RutaPunto> rutaActual) {
            this.rutaActual = rutaActual;
        }
    }

    public static class RutaPunto {
        public int posX;
        public int posY;
        public String estado;
        public String accion;
        public int idPedido;
        public Integer traspasoGLP;

        public RutaPunto(int posX, int posY, String estado, String accion, int idPedido, Integer traspasoGLP) {
            this.posX = posX;
            this.posY = posY;
            this.estado = estado;
            this.accion = accion;
            this.idPedido = idPedido;
            this.traspasoGLP = traspasoGLP;
        }
    }

    public static class Pedido {
        public int idPedido;
        public String estado;
        public int glp;
        public String tiempoLimite;
        public List<VehiculoAtendiendo> vehiculosAtendiendo;
        public int posX;
        public int posY;

        public Pedido(int idPedido, String estado, int glp, String tiempoLimite, int posX, int posY) {
            this.idPedido = idPedido;
            this.estado = estado;
            this.glp = glp;
            this.tiempoLimite = tiempoLimite;
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
                Vehiculo vehiculo = new Vehiculo(
                    vehicle.id,
                    vehicle.type,
                    (int)vehicle.currentFuel,
                    vehicle.maxFuel,
                    vehicle.maxGLP,
                    vehicle.currentGLP,
                    vehicle.plaque,
                    (int)vehicle.position.x,
                    (int)vehicle.position.y
                );
                // Map route points if they exist
                if (vehicle.currentPath != null) {
                    List<RutaPunto> rutaActual = vehicle.currentPath.stream()
                        .map(point -> {
                            int glpAmount = 0;
                            // Only try to get next node if vehicle is still active
                            if (routes.containsKey(vehicle.id) && vehicle.nextNodeIndex < routes.get(vehicle.id).size()) {
                                pucp.pdds.backend.algos.algorithm.Node nextNode = routes.get(vehicle.id).get(vehicle.nextNodeIndex);
                                if (nextNode instanceof pucp.pdds.backend.algos.algorithm.OrderDeliverNode) {
                                    glpAmount = ((pucp.pdds.backend.algos.algorithm.OrderDeliverNode)nextNode).order.amountGLP;
                                } else if (nextNode instanceof pucp.pdds.backend.algos.algorithm.ProductRefillNode) {
                                    glpAmount = ((pucp.pdds.backend.algos.algorithm.ProductRefillNode)nextNode).amountGLP;
                                }
                            }
                            return new RutaPunto(
                                (int)point.x,
                                (int)point.y, 
                                vehicle.state.toString(),
                                vehicle.state.toString(),
                                routes.containsKey(vehicle.id) && vehicle.nextNodeIndex < routes.get(vehicle.id).size() ? 
                                    (routes.get(vehicle.id).get(vehicle.nextNodeIndex) instanceof pucp.pdds.backend.algos.algorithm.OrderDeliverNode ? 
                                        ((pucp.pdds.backend.algos.algorithm.OrderDeliverNode) routes.get(vehicle.id).get(vehicle.nextNodeIndex)).order.id : 0) : 0,
                                glpAmount
                            );
                        })
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
                warehouse.currentGLP,
                warehouse.maxGLP,
                warehouse.isMain,
                warehouse.wasVehicle
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Pedido> convertOrdersToDataChunk(List<pucp.pdds.backend.algos.entities.PlannerOrder> activeOrders, 
            List<pucp.pdds.backend.algos.entities.PlannerVehicle> activeVehicles,
            java.util.Map<Integer, List<pucp.pdds.backend.algos.algorithm.Node>> routes,
            pucp.pdds.backend.algos.utils.Time currTime) {
        return activeOrders.stream()
            .map(order -> {
                Pedido pedido = new Pedido(
                    order.id,
                    order.isDelivered() ? "Completado" : "Pendiente",
                    order.amountGLP,
                    order.deadline.toString(),
                    (int)order.position.x,
                    (int)order.position.y
                );
                // Map attending vehicles if they exist
                List<VehiculoAtendiendo> vehiculosAtendiendo = new ArrayList<>();
                for (pucp.pdds.backend.algos.entities.PlannerVehicle vehicle : activeVehicles) {
                    if (vehicle.currentPath != null && !vehicle.currentPath.isEmpty()) {
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
                pedido.setVehiculosAtendiendo(vehiculosAtendiendo);
                return pedido;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public static List<Incidencia> convertIncidentsToDataChunk(
            List<pucp.pdds.backend.algos.entities.PlannerFailure> failures,
            List<pucp.pdds.backend.algos.entities.PlannerMaintenance> activeMaintenances) {
        List<Incidencia> incidencias = new ArrayList<>();
        
        // Add failures as incidents
        failures.forEach(failure -> incidencias.add(new Incidencia(
            failure.id,
            failure.timeOccuredOn != null ? failure.timeOccuredOn.toString() : "",
            failure.timeOccuredOn != null ? failure.timeOccuredOn.toString() : "",
            failure.shiftOccurredOn.toString(),
            failure.type.toString(),
            failure.vehiclePlaque,
            failure.timeOccuredOn != null ? "FINISHED" : "ACTIVE"
        )));

        // Add maintenances as incidents
        activeMaintenances.forEach(maintenance -> incidencias.add(new Incidencia(
            maintenance.id,
            maintenance.startDate.toString(),
            maintenance.endDate.toString(),
            "T1", // Default shift since it's not specified in PlannerMaintenance
            "MAINTENANCE",
            maintenance.vehiclePlaque,
            "ACTIVE"
        )));

        return incidencias;
    }
}
