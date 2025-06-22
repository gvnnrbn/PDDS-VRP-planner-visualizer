package pucp.pdds.backend.dto;

import java.io.Serializable;
import java.util.List;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;

public class SimulationStateDTO implements Serializable {
    private final String minuto;
    private final List<PlannerVehicle> vehiculos;
    private final List<PlannerOrder> pedidos;
    private final List<PlannerBlockage> bloqueos;
    private final List<PlannerWarehouse> almacenes;

    public SimulationStateDTO(String minuto, List<PlannerVehicle> vehiculos, List<PlannerOrder> pedidos, List<PlannerBlockage> bloqueos, List<PlannerWarehouse> almacenes) {
        this.minuto = minuto;
        this.vehiculos = vehiculos;
        this.pedidos = pedidos;
        this.bloqueos = bloqueos;
        this.almacenes = almacenes;
    }

    public String getMinuto() {
        return minuto;
    }

    public List<PlannerVehicle> getVehiculos() {
        return vehiculos;
    }

    public List<PlannerOrder> getPedidos() {
        return pedidos;
    }

    public List<PlannerBlockage> getBloqueos() {
        return bloqueos;
    }

    public List<PlannerWarehouse> getAlmacenes() {
        return almacenes;
    }
} 