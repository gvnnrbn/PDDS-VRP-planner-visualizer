package pucp.pdds.backend.algos.data;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.utils.Position;

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
}
