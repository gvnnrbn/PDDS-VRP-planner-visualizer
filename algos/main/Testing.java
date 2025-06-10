package main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import algorithm.Algorithm;
import algorithm.Environment;
import algorithm.Node;
import algorithm.Solution;
import data.DataChunk;
import utils.DataParser;
import utils.PathBuilder;
import utils.SimulationProperties;
import utils.SimulationVisualizer;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import utils.DataExporter;

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
        Time currTime = new Time(2025, 1, 1, 0, 0);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        int totalIntervals = 7 * 24 * 60 / minutesToSimulate;
        for(int i=0; i<totalIntervals; i++) {
            List<PlannerBlockage> activeBlockages = getActiveBlockages(blockages, currTime);
            List<PlannerOrder> activeOrders = getActiveOrders(orders, currTime);
            List<PlannerMaintenance> activeMaintenances = getActiveMaintenances(maintenances, currTime);
            List<PlannerVehicle> activeVehicles = getActiveVehicles(vehicles, currTime);

            Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
            if (SimulationProperties.isDebug) {
                System.out.println("Planning interval " + i + " started at " + currTime + " with " + activeVehicles.size() + " vehicles and " + activeOrders.size() + " orders");
            }
            Solution sol = Algorithm.run(environment, minutesToSimulate);
            if (SimulationProperties.isDebug) {
                System.out.println(sol.getReport());
            }

            if (!sol.isFeasible(environment)) {
                throw new RuntimeException("Solution is not feasible");
            }

            for (PlannerVehicle vehicle : activeVehicles) {
                vehicle.nextNodeIndex = 1;
                if (vehicle.state == PlannerVehicle.VehicleState.FINISHED) {
                    vehicle.state = PlannerVehicle.VehicleState.IDLE;
                }
            }

            DataChunk dataChunk = new DataChunk();
            DataChunk.MinuteData minuteData = new DataChunk.MinuteData(currTime);
            minuteData.vehicles.addAll(activeVehicles.stream().map(vehicle -> new DataChunk.VehicleData(vehicle.plaque, vehicle.position.clone(), vehicle.state)).collect(Collectors.toList()));

            for (int iteration = 0; iteration < minutesToSimulate; iteration++) {
                if (SimulationProperties.isDebug) {
                    System.out.println("--- Time: " + currTime + " ---");
                }

                for (PlannerVehicle plannerVehicle : vehicles) {
                    // If vehicle should pass to maintenance
                    if (plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE && activeMaintenances.stream().anyMatch(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque))) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.MAINTENANCE;
                        plannerVehicle.currentMaintenance = activeMaintenances.stream().filter(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque)).findFirst().get();
                        if (SimulationProperties.isDebug) {
                            System.out.println("Vehicle " + plannerVehicle.id + " is going into maintenance: " + plannerVehicle.currentMaintenance);
                        }
                    } 
                    
                    // If vehicle should leave maintenance
                    if (plannerVehicle.state == PlannerVehicle.VehicleState.MAINTENANCE && plannerVehicle.currentMaintenance.endDate.isBefore(currTime)) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                        plannerVehicle.currentMaintenance = null;
                        if (SimulationProperties.isDebug) {
                            System.out.println("Vehicle " + plannerVehicle.id + " is leaving maintenance");
                        }
                    }

                    if (plannerVehicle.state == PlannerVehicle.VehicleState.FINISHED || !activeVehicles.contains(plannerVehicle)) {
                        continue;
                    }

                    // If no path or path is empty, check if at next node; if not, build path
                    if (plannerVehicle.currentPath == null || plannerVehicle.currentPath.isEmpty()) {
                        List<Node> route = sol.routes.get(plannerVehicle.id);
                        if (route == null || plannerVehicle.nextNodeIndex >= route.size()) {
                            continue;
                        }
                        Node nextNode = route.get(plannerVehicle.nextNodeIndex);
                        // Check if at the node's position
                        if (!plannerVehicle.position.equals(nextNode.getPosition())) {
                            // Not at node yet: build path to it
                            plannerVehicle.currentPath = PathBuilder.buildPath(plannerVehicle.position, nextNode.getPosition(), activeBlockages);
                            continue;
                        }
                        // Has arrived at location
                        if (SimulationProperties.isDebug) {
                            System.out.println("Vehicle " + plannerVehicle.id + " has arrived at location of node " + nextNode);
                        }
                        plannerVehicle.processNode(nextNode, plannerVehicle, activeOrders, warehouses, currTime);

                        if (plannerVehicle.nextNodeIndex == route.size() - 1) {
                            // Just processed the FinalNode
                            if (SimulationProperties.isDebug) {
                                System.out.println("HAS REACHED FINAL NODE (base)");
                            }
                            plannerVehicle.state = PlannerVehicle.VehicleState.FINISHED;
                            plannerVehicle.nextNodeIndex++; // Optional: move index past end
                            continue;
                        }
                        plannerVehicle.nextNodeIndex++;
                        // No need to build path here; will do so on next iteration if needed
                    } else {
                        if (plannerVehicle.waitTransition > 0) {
                            if (SimulationProperties.isDebug) {
                                System.out.println("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
                            }
                            plannerVehicle.waitTransition--;
                        } else {
                            plannerVehicle.advancePath(SimulationProperties.speed / 60.0);
                            plannerVehicle.state = PlannerVehicle.VehicleState.ONTHEWAY;
                        }
                    }
                }

                // Collect only delivery nodes currently being served (next node for each vehicle if it's an OrderDeliverNode)
                List<Node> deliveryNodes = new ArrayList<>();
                List<Node> refillNodes = new ArrayList<>();
                for (PlannerVehicle v : activeVehicles) {
                    List<Node> route = sol.routes.get(v.id);
                    if (route != null && v.nextNodeIndex < route.size()) {
                        Node nextNode = route.get(v.nextNodeIndex);
                        if (nextNode instanceof algorithm.OrderDeliverNode) {
                            deliveryNodes.add(nextNode);
                        } else if (nextNode instanceof algorithm.ProductRefillNode) {
                            refillNodes.add(nextNode);
                        }
                    }
                }

                minuteData = new DataChunk.MinuteData(currTime);
                minuteData.vehicles.addAll(activeVehicles.stream().map(vehicle -> new DataChunk.VehicleData(vehicle.plaque, vehicle.position, vehicle.state)).collect(Collectors.toList()));
                dataChunk.minutes.add(minuteData);

                SimulationVisualizer.draw(activeVehicles, activeBlockages, deliveryNodes, refillNodes, currTime.toString());
                try {
                    Thread.sleep(SimulationProperties.timeUnitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currTime = currTime.addMinutes(1);
            }

            if (SimulationProperties.isDebug) {
                DataExporter.exportToJson(dataChunk, currTime);
            }
        }
    }
}
