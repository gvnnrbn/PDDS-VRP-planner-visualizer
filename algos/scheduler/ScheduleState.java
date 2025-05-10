package scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import domain.Environment;
import utils.Time;

public class ScheduleState {
    public Time currentTime;
    public List<SchedulerVehicle> vehicles;
    public List<SchedulerWarehouse> warehouses;
    public List<SchedulerBlockage> blockages;
    public List<SchedulerMaintenance> maintenances;
    public List<SchedulerFailure> failures;
    public List<SchedulerOrder> orders;
    
    // todo:
    public List<SchedulerFailure> failuresOccured;

    public ScheduleState(Time startTime) {
        this.currentTime = startTime;
        this.vehicles = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.warehouses = new ArrayList<>();
        this.blockages = new ArrayList<>();
        this.maintenances = new ArrayList<>();
        this.failures = new ArrayList<>();
    }

    // check all data present during currentTime-endOfScheduling
    public void updateData(Time currentTime, List<SchedulerVehicle> allvehicles,
        List<SchedulerOrder> allOrders, List<SchedulerWarehouse> allwarehouses,
        List<SchedulerBlockage> allblockages, List<SchedulerMaintenance> allmaintenances,
        List<SchedulerFailure> allFailures) {

                
        // Filter and add allvehicles with state WAITING or ONTHEWAY
        this.vehicles.addAll(allvehicles.stream()
            .filter(v -> (v.state == EnumVehicleState.IDLE || v.state == EnumVehicleState.ONTHEWAY))
            .collect(Collectors.toList()));

        // Filter and add allOrders
        this.orders.addAll(allOrders.stream()
            .filter(o -> o.arrivalTime.isBeforeOrAt(currentTime))
            .collect(Collectors.toList()));
        
        // Filter and add AVAILABLE warehouses
        this.warehouses.addAll(allwarehouses.stream()
            .filter(w -> w.currentGLP  >= Environment.refillChunkSize)
            .collect(Collectors.toList()));

        // Filter and add AVAILABLE blockages
        this.blockages.addAll(allblockages.stream()
            .filter(b -> b.startTime.isBeforeOrAt(currentTime))// endTime!!!!!
            .collect(Collectors.toList()));

        // Filter and add maintenances
        this.maintenances.addAll(allmaintenances.stream()
            .filter(m -> m.date.isBeforeOrAt(currentTime))
            .collect(Collectors.toList()));
        
        // Add all failures which could happen in this update
        this.failures.addAll(allFailures.stream()
            .filter(f -> (f.shiftOccurredOn-1) * 8 < currentTime.getHour())
            .collect(Collectors.toList()));
        
    }

    // // Check pending:reusable
    // private List<SchedulerVehicle> deepCopyVehicles(List<SchedulerVehicle> original) {
    //     return original.stream()
    //             .map(v -> new SchedulerVehicle(v.id, v.type, v.state, v.weight, v.maxFuel,
    //                     v.currentFuel, v.maxGLP, v.currentGLP, v.initialPosition))
    //             .collect(Collectors.toList());
    // }

    // private List<SchedulerWarehouse> deepCopyWarehouses(List<SchedulerWarehouse> original) {
    //     return original.stream()
    //             .map(w -> new SchedulerWarehouse(w.id, w.position, w.currentGLP, w.maxGLP, w.isMain()))
    //             .collect(Collectors.toList());
    // }

    // public SchedulerVehicle getVehicleById(String id) {
    //     return vehicles.stream().filter(v -> v.id.equals(id)).findFirst().orElse(null);
    // }

    // public void advanceTime(int minutes) {
    //     currentTime = currentTime.addMinutes(minutes);
    // }

    // public void updateOrders(List<SchedulerOrder> remaining) {
    //     this.orders = new ArrayList<>(remaining);
    // }

    // public void updateVehicles(List<SchedulerVehicle> updatedVehicles) {
    //     this.vehicles = updatedVehicles;
    // }

    // public void updateWarehouses(List<SchedulerWarehouse> updatedWarehouses) {
    //     this.warehouses = updatedWarehouses;
    // }

    // @Override
    // public String toString() {
    //     return String.format("Time: %s\nVehicles: %d\nOrders pending: %d\n",
    //             currentTime.toString(), vehicles.size(), orders.size());
    // }

    // public void printState(int iteration) {
    //     System.out.println("\n📦 Estado tras la planificación #" + iteration);
    //     System.out.println("🕒 Tiempo actual: " + currentTime);

    //     System.out.println("\n🚛 Vehículos:");
    //     for (SchedulerVehicle v : vehicles) {
    //         System.out.printf("- Vehículo %d → Pos: (%d, %d), Fuel: %.2f, GLP: %d\n",
    //                 v.id, v.initialPosition.x, v.initialPosition.y, v.currentFuel, v.currentGLP);
    //     }

    //     System.out.println("\n📦 Pedidos pendientes:");
    //     for (SchedulerOrder o : orders) {
    //         System.out.printf("- Pedido %d → Pos: (%d, %d), GLP: %d, deadline: %s\n",
    //                 o.id, o.position.x, o.position.y, o.amountGLP, o.deadline);
    //     }
    // }
}