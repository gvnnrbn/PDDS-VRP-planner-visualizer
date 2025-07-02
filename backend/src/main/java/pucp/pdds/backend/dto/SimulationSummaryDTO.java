package pucp.pdds.backend.dto;

import java.util.List;
import java.util.Map;

public class SimulationSummaryDTO {
    private String fechaInicio;
    private String fechaFin;
    private String duracion;
    private int pedidosEntregados;
    private double consumoPetroleo;
    private String tiempoPlanificacion;
    private List<Map<String, Object>> simulacionCompleta;
    private Map<String, Object> estadisticas;

    public SimulationSummaryDTO() {}

    public SimulationSummaryDTO(String fechaInicio, String fechaFin, String duracion, 
                               int pedidosEntregados, double consumoPetroleo, 
                               String tiempoPlanificacion, List<Map<String, Object>> simulacionCompleta,
                               Map<String, Object> estadisticas) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.duracion = duracion;
        this.pedidosEntregados = pedidosEntregados;
        this.consumoPetroleo = consumoPetroleo;
        this.tiempoPlanificacion = tiempoPlanificacion;
        this.simulacionCompleta = simulacionCompleta;
        this.estadisticas = estadisticas;
    }

    // Getters
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public String getDuracion() { return duracion; }
    public int getPedidosEntregados() { return pedidosEntregados; }
    public double getConsumoPetroleo() { return consumoPetroleo; }
    public String getTiempoPlanificacion() { return tiempoPlanificacion; }
    public List<Map<String, Object>> getSimulacionCompleta() { return simulacionCompleta; }
    public Map<String, Object> getEstadisticas() { return estadisticas; }

    // Setters
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
    public void setDuracion(String duracion) { this.duracion = duracion; }
    public void setPedidosEntregados(int pedidosEntregados) { this.pedidosEntregados = pedidosEntregados; }
    public void setConsumoPetroleo(double consumoPetroleo) { this.consumoPetroleo = consumoPetroleo; }
    public void setTiempoPlanificacion(String tiempoPlanificacion) { this.tiempoPlanificacion = tiempoPlanificacion; }
    public void setSimulacionCompleta(List<Map<String, Object>> simulacionCompleta) { this.simulacionCompleta = simulacionCompleta; }
    public void setEstadisticas(Map<String, Object> estadisticas) { this.estadisticas = estadisticas; }
} 