package utils;

import domain.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentBuilder {

    public static Environment build(SimulationState state, int scMinutes) {
        // Calcular el rango de tiempo de planificación
        Time currentTime = state.currentTime;
        Time maxDeadline = currentTime.addMinutes(scMinutes);

        // Filtrar pedidos que entran en el rango actual de planificación
        List<Order> filteredOrders = state.pendingOrders.stream()
            .filter(order -> !order.deadline().isAfter(maxDeadline)) // incluye solo los que vencen antes o dentro del SC
            .collect(Collectors.toList());

        // Copiar vehículos y almacenes tal como están en el estado actual
        List<Vehicle> copiedVehicles = deepCopyVehicles(state.vehicles);
        List<Warehouse> copiedWarehouses = deepCopyWarehouses(state.warehouses);

        // Bloqueos: si decides hacerlos dinámicos, aquí se actualizarían
        List<Blockage> blockages = new ArrayList<>(); // vacíos por ahora, o puedes traerlos del estado
        System.out.println(
                "Planificando " + filteredOrders.size() + " pedidos entre " + currentTime + " y " + maxDeadline);

        return new Environment(copiedVehicles, filteredOrders, copiedWarehouses, blockages, currentTime);
    }

    private static List<Vehicle> deepCopyVehicles(List<Vehicle> original) {
        return original.stream()
                .map(v -> new Vehicle(v.id(), v.weight(), v.maxFuel(), v.currentFuel(), v.maxGLP(), v.currentGLP(),
                        v.initialPosition()))
                .collect(Collectors.toList());
    }

    private static List<Warehouse> deepCopyWarehouses(List<Warehouse> original) {
        return original.stream()
                .map(w -> new Warehouse(w.id(), w.position(), w.currentGLP(), w.maxGLP(), w.isMain()))
                .collect(Collectors.toList());
    }
}
