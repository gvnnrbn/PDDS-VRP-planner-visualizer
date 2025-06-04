package main;

import java.util.List;
import java.util.stream.Collectors;

import algorithm.Algorithm;
import algorithm.Environment;
import algorithm.Node;
import algorithm.Solution;
import utils.DataParser;
import utils.PathBuilder;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;

public class Testing {
    public static void main(String[] args) {
        int minutesToSimulate = 60;
        Time currTime = (new Time(2025, 1, 1, 0, 0)).addMinutes(minutesToSimulate);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        // filter all blockages that are active at currTime
        List<PlannerBlockage> activeBlockages = blockages.stream().filter(blockage -> blockage.isActive(currTime)).collect(Collectors.toList());

        // filter all orders that are active at currTime
        List<PlannerOrder> activeOrders = orders.stream().filter(order -> order.isActive(currTime)).collect(Collectors.toList());

        // filter all maintenances that are active at currTime
        List<PlannerMaintenance> activeMaintenances = maintenances.stream().filter(maintenance -> maintenance.isActive(currTime)).collect(Collectors.toList());

        List<PlannerVehicle> activeVehicles = vehicles.stream().filter(vehicle -> vehicle.isActive(currTime)).collect(Collectors.toList());

        System.out.println("Active vehicles: " + activeVehicles.size());
        for (PlannerVehicle vehicle : activeVehicles) {
            System.out.println(vehicle);
        }

        System.out.println("Active orders: " + activeOrders.size());
        for (PlannerOrder order : activeOrders) {
            System.out.println(order);
        }

        System.out.println("Active blockages: " + activeBlockages.size());
        for (PlannerBlockage blockage : activeBlockages) {
            System.out.println(blockage);
        }

        System.out.println("Active maintenances: " + activeMaintenances.size());
        for (PlannerMaintenance maintenance : activeMaintenances) {
            System.out.println(maintenance);
        }

        System.out.println("Active failures: " + failures.size());
        for (PlannerFailure failure : failures) {
            System.out.println(failure);
        }

        System.out.println("Active warehouses: " + warehouses.size());
        for (PlannerWarehouse warehouse : warehouses) {
            System.out.println(warehouse);
        }

        Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime);
        System.out.println(environment);

        Solution sol = Algorithm.run(environment, minutesToSimulate);
        sol.simulate(environment, minutesToSimulate);
        System.out.println(sol);
    }
}
