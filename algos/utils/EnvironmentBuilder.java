package utils;

import domain.*;
import scheduler.EnumVehicleState;
import scheduler.ScheduleState;
import scheduler.SchedulerBlockage;
import scheduler.SchedulerOrder;
import scheduler.SchedulerVehicle;
import scheduler.SchedulerWarehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentBuilder {

    // copy or convert into Environment attributes
    public static Environment build(ScheduleState state,int scMinutes ) {
        Time currentTime = state.currentTime;
        Time previousTime = currentTime.subtractMinutes(scMinutes);

        List<Vehicle> environmentVehicles = convertToVehicles(state.vehicles);
        List<Warehouse> environmentWarehouses = convertToWarehouses(state.warehouses, state.vehicles);
        List<Blockage> environmentBlockages = convertToBlockages(state.blockages, state.currentTime);
        List<Order> environmentOrders = convertToOrders(state.orders);
        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : environmentOrders) {
            Time releaseTime = order.releaseTime();
            if (releaseTime.isAfter(previousTime) && releaseTime.isBefore(currentTime)) {
                filteredOrders.add(order);
            }
        }
        environmentOrders = filteredOrders;

        return new Environment(environmentVehicles, environmentOrders, environmentWarehouses, environmentBlockages, currentTime);
    }

    private static List<Vehicle> convertToVehicles(List<SchedulerVehicle> original) {
        return original.stream()
            .filter(v -> v.state == EnumVehicleState.IDLE || v.state == EnumVehicleState.ONTHEWAY )
            .map(v -> new Vehicle(v.id, v.weight, v.maxFuel,
            v.currentFuel, v.maxGLP, v.currentGLP, v.position))
            .collect(Collectors.toList());
    }

    private static List<Warehouse> convertToWarehouses(List<SchedulerWarehouse> original, List<SchedulerVehicle> stuckedVehicles) {
        List<Warehouse> list = original.stream()
            .map(w -> new Warehouse(w.id, w.position, w.currentGLP, w.maxGLP, w.isMain, w.wasVehicle))
            .collect(Collectors.toList());
        for(SchedulerVehicle v : stuckedVehicles){
            if(v.state == EnumVehicleState.STUCK){
                list.add(new Warehouse(v.id+1000, v.position, v.currentGLP, v.maxGLP, false, true));
            }
        }
        return list;
    }
    
    private static List<Blockage> convertToBlockages(List<SchedulerBlockage> original, Time currentTime) {
        return original.stream()
        .filter(b -> ! b.endTime.isBeforeOrAt(currentTime))
        .map(b -> new Blockage(b.vertices))
        .collect(Collectors.toList());
    }
    private static List<Order> convertToOrders(List<SchedulerOrder> original) {
        return original.stream()
            .filter(o -> o.deliverTime == null)
            .map(o -> new Order(o.id, o.amountGLP, o.position, o.deadline, o.arrivalTime))
            .collect(Collectors.toList());
    }
}
