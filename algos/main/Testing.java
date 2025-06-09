package main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import algorithm.Algorithm;
import algorithm.Environment;
import algorithm.Node;
import algorithm.Solution;
import utils.DataParser;
import utils.PathBuilder;
import utils.Position;
import utils.SimulationProperties;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
        int minutesToSimulate = 120;
        Time currTime = new Time(2025, 1, 1, 0, 0).addMinutes(minutesToSimulate);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        List<PlannerBlockage> activeBlockages = getActiveBlockages(blockages, currTime);
        List<PlannerOrder> activeOrders = getActiveOrders(orders, currTime);
        List<PlannerMaintenance> activeMaintenances = getActiveMaintenances(maintenances, currTime);
        List<PlannerVehicle> activeVehicles = getActiveVehicles(vehicles, currTime);
        activeVehicles = activeVehicles.subList(0, 1);

        Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
        Solution sol = Algorithm.run(environment, minutesToSimulate);
        System.out.println(sol.getReport());

        for (PlannerVehicle vehicle : activeVehicles) {
            vehicle.nextNodeIndex = 1;
        }

        for (int i = 0; i < minutesToSimulate; i++) {
            System.out.println("--- Time: " + currTime + " ---");

            for (PlannerVehicle plannerVehicle : activeVehicles) {
                System.out.println("Vehicle " + plannerVehicle.id + " started at position: " + plannerVehicle.position);

                // Create path when there's no path or the path is empty
                if (plannerVehicle.currentPath == null || plannerVehicle.currentPath.isEmpty()) {
                    System.out.println("Before currentPath correction the path is: " + plannerVehicle.currentPath);

                    // Has arrived at location
                    Node arrivedNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex);
                    System.out.println("Vehicle " + plannerVehicle.id + " has arrived at location of node " + arrivedNode);

                    // HERE GOES ON_REACHING_NODE_LOCATION
                    System.out.println("Vehicle " + plannerVehicle.id + " is processing node " + arrivedNode);
                    plannerVehicle.processNode(arrivedNode, plannerVehicle, activeOrders, warehouses, currTime);

                    plannerVehicle.nextNodeIndex++;

                    if (plannerVehicle.nextNodeIndex >= sol.routes.get(plannerVehicle.id).size()) {
                        // Has reached last node
                        System.out.println("HAS REACHED LAST NODE");
                        continue;
                    }

                    Node currNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex - 1);
                    Node nextNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex);

                    plannerVehicle.currentPath = PathBuilder.buildPath(currNode.getPosition(), nextNode.getPosition(), activeBlockages);
                    System.out.println("After currentPath correction the path is: " + plannerVehicle.currentPath);
                } else {

                    if (plannerVehicle.waitTransition > 0) {
                        System.out.println("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
                        plannerVehicle.waitTransition--;
                    } else {
                        System.out.println("Vehicle " + plannerVehicle.id + " is advancing path");
                        plannerVehicle.advancePath(SimulationProperties.speed / 60.0);
                    }
                }
                System.out.println("Vehicle " + plannerVehicle.id + " ended at position: " + plannerVehicle.position);
            }

            currTime = currTime.addMinutes(1);
        }
    }
}
