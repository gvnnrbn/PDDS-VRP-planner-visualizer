package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimulationState {
    public Time currentTime;
    public List<Vehicle> vehicles;
    public List<Order> pendingOrders;
    public List<Warehouse> warehouses;

    public SimulationState(Time startTime, List<Vehicle> initialVehicles, List<Order> allOrders,
            List<Warehouse> warehouses) {
        this.currentTime = startTime;
        this.vehicles = deepCopyVehicles(initialVehicles);
        this.pendingOrders = new ArrayList<>(allOrders);
        this.warehouses = deepCopyWarehouses(warehouses);
    }

    private List<Vehicle> deepCopyVehicles(List<Vehicle> original) {
        return original.stream()
                .map(v -> new Vehicle(v.id(), v.weight(), v.maxFuel(), v.currentFuel(), v.maxGLP(), v.currentGLP(),
                        v.initialPosition()))
                .collect(Collectors.toList());
    }

    private List<Warehouse> deepCopyWarehouses(List<Warehouse> original) {
        return original.stream()
                .map(w -> new Warehouse(w.id(), w.position(), w.currentGLP(), w.maxGLP(), w.isMain()))
                .collect(Collectors.toList());
    }

    public Vehicle getVehicleById(int id) {
        return vehicles.stream().filter(v -> v.id() == id).findFirst().orElse(null);
    }

    public void advanceTime(int minutes) {
        currentTime = currentTime.addMinutes(minutes);
    }

    public void updateOrders(List<Order> remaining) {
        this.pendingOrders = new ArrayList<>(remaining);
    }

    public void updateVehicles(List<Vehicle> updatedVehicles) {
        this.vehicles = updatedVehicles;
    }

    public void updateWarehouses(List<Warehouse> updatedWarehouses) {
        this.warehouses = updatedWarehouses;
    }

    @Override
    public String toString() {
        return String.format("Time: %s\nVehicles: %d\nOrders pending: %d\n",
                currentTime.toString(), vehicles.size(), pendingOrders.size());
    }

    public void printState(int iteration) {
        System.out.println("\n📦 Estado tras la planificación #" + iteration);
        System.out.println("🕒 Tiempo actual: " + currentTime);

        System.out.println("\n🚛 Vehículos:");
        for (Vehicle v : vehicles) {
            System.out.printf("- Vehículo %d → Pos: (%d, %d), Fuel: %.2f, GLP: %d\n",
                    v.id(), v.initialPosition().x(), v.initialPosition().y(), v.currentFuel(), v.currentGLP());
        }

        System.out.println("\n📦 Pedidos pendientes:");
        for (Order o : pendingOrders) {
            System.out.printf("- Pedido %d → Pos: (%d, %d), GLP: %d, deadline: %s\n",
                    o.id(), o.position().x(), o.position().y(), o.amountGLP(), o.deadline());
        }
    }
}