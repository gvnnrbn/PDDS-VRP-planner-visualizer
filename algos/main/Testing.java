package main;

import java.util.List;
import java.util.stream.Collectors;

import algorithm.Algorithm;
import algorithm.Environment;
import algorithm.Solution;
import utils.DataParser;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;

public class Testing {
    private static List<PlannerBlockage> getActiveBlockages(List<PlannerBlockage> blockages, Time time) {
        return blockages.stream()
            .filter(blockage -> blockage.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerOrder> getActiveOrders(List<PlannerOrder> orders, Time time) {
        return orders.stream()
            .filter(order -> order.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerMaintenance> getActiveMaintenances(List<PlannerMaintenance> maintenances, Time time) {
        return maintenances.stream()
            .filter(maintenance -> maintenance.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerVehicle> getActiveVehicles(List<PlannerVehicle> vehicles, Time time) {
        return vehicles.stream()
            .filter(vehicle -> vehicle.isActive(time))
            .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        int minutesToSimulate = 60;
        Time currTime = new Time(2025, 1, 1, 0, 0).addMinutes(minutesToSimulate);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        // Simulate a week
        // final Time endTime = currTime.addMinutes(7 * 24 * 60); // 1 week later
        // while (currTime.isBefore(endTime)) {
            List<PlannerBlockage> activeBlockages = getActiveBlockages(blockages, currTime);
            List<PlannerOrder> activeOrders = getActiveOrders(orders, currTime);
            List<PlannerMaintenance> activeMaintenances = getActiveMaintenances(maintenances, currTime);
            List<PlannerVehicle> activeVehicles = getActiveVehicles(vehicles, currTime);

            Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
            System.out.println("Environment sizes:");
            System.out.println("  Active vehicles: " + activeVehicles.size());
            System.out.println("  Active orders: " + activeOrders.size()); 
            System.out.println("  Warehouses: " + warehouses.size());
            System.out.println("  Active blockages: " + activeBlockages.size());
            System.out.println("  Failures: " + failures.size());
            System.out.println("  Active maintenances: " + activeMaintenances.size());
            Solution sol = Algorithm.run(environment, minutesToSimulate);
            System.out.println(sol.getReport());

            // for (PlannerVehicle plannerVehicle : vehicles) {
            //     plannerVehicle.currentNode = sol.routes.get(plannerVehicle.id).get(0);
            // }

            // // Iterate over time
            // for(int i = 0; i < minutesToSimulate; i++) {
            //     // Iterate over vehicles
            //     for (PlannerVehicle plannerVehicle : vehicles) {
            //     }
            // }

            // currTime = currTime.addMinutes(minutesToSimulate);
        // }
    }
}
