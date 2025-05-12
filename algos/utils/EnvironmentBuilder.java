package utils;

import domain.*;
import scheduler.ScheduleState;
import scheduler.SchedulerBlockage;
import scheduler.SchedulerOrder;
import scheduler.SchedulerVehicle;
import scheduler.SchedulerWarehouse;

import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentBuilder {

    // copy or convert into Environment attributes
    public static Environment build(ScheduleState state) {
        Time currentTime = state.currentTime;
        List<Vehicle> environmentVehicles = convertToVehicles(state.vehicles);
        List<Warehouse> environmentWarehouses = convertToWarehouses(state.warehouses);
        List<Blockage> environmentBlockages = convertToBlockages(state.blockages);
        List<Order> environmentOrders = convertToOrders(state.orders);

        return new Environment(environmentVehicles, environmentOrders, environmentWarehouses, environmentBlockages, currentTime);
    }

    private static List<Vehicle> convertToVehicles(List<SchedulerVehicle> original) {
        return original.stream()
            .map(v -> new Vehicle(v.id, v.weight, v.maxFuel,
            v.currentFuel, v.maxGLP, v.currentGLP, v.position))
            .collect(Collectors.toList());
    }

    private static List<Warehouse> convertToWarehouses(List<SchedulerWarehouse> original) {
        return original.stream()
            .map(w -> new Warehouse(w.id, w.position, w.currentGLP, w.maxGLP, w.isMain, w.wasVehicle))
            .collect(Collectors.toList());
    }
    
    private static List<Blockage> convertToBlockages(List<SchedulerBlockage> original) {
        return original.stream()
        .map(b -> new Blockage(b.vertices))
        .collect(Collectors.toList());
    }
    private static List<Order> convertToOrders(List<SchedulerOrder> original) {
        return original.stream()
            .map(o -> new Order(o.id, o.amountGLP,o.position, o.deadline))
            .collect(Collectors.toList());
    }
}
