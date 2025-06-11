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
import utils.Position;
import utils.SimulationProperties;
import utils.SimulationVisualizer;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure.Shift;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import utils.DataExporter;

public class Testing {
    private static List<PlannerBlockage> getActiveBlockages(List<PlannerBlockage> blockages, Time time) {
        return blockages.stream()
            .filter(blockage -> blockage.isActive(time, time.addMinutes(SimulationProperties.minutesToSimulate)))
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

    private static void debugPrint(String message, Time currentTime, boolean outsideDebug) {
        if (SimulationProperties.isDebug || outsideDebug) {
            if (currentTime != null) {
                System.out.println(currentTime + " | " + message);
            } else {
                System.out.println(message);
            }
        }
    }

    public static void main(String[] args) {
        Time currTime = new Time(2025, 1, 1, 0, 0);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        int totalIntervals = 7 * 24 * 60 / SimulationProperties.minutesToSimulate;
        for(int i=0; i<totalIntervals; i++) {
            List<PlannerBlockage> activeBlockages = getActiveBlockages(blockages, currTime);
            List<PlannerOrder> activeOrders = getActiveOrders(orders, currTime);
            List<PlannerMaintenance> activeMaintenances = getActiveMaintenances(maintenances, currTime);
            List<PlannerVehicle> activeVehicles = getActiveVehicles(vehicles, currTime);

            Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, SimulationProperties.minutesToSimulate);
            debugPrint("Planning interval " + i + " started at " + currTime + " with " + activeVehicles.size() + " vehicles and " + activeOrders.size() + " orders", currTime, true);
            Solution sol = Algorithm.run(environment, SimulationProperties.minutesToSimulate);
            debugPrint(sol.getReport(), currTime, false);

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

            for (int iteration = 0; iteration < SimulationProperties.minutesToSimulate; iteration++) {
                debugPrint("--- Time: " + currTime + " ---", currTime, false);

                for (PlannerVehicle plannerVehicle : vehicles) {
                    // If vehicle should pass to maintenance
                    if (plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE && activeMaintenances.stream().anyMatch(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque))) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.MAINTENANCE;
                        plannerVehicle.currentMaintenance = activeMaintenances.stream().filter(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque)).findFirst().get();
                        debugPrint("Vehicle " + plannerVehicle.id + " is going into maintenance: " + plannerVehicle.currentMaintenance, currTime, false);
                    } 
                    
                    // If vehicle should leave maintenance
                    if (plannerVehicle.state == PlannerVehicle.VehicleState.MAINTENANCE && plannerVehicle.currentMaintenance.endDate.isBefore(currTime)) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                        plannerVehicle.currentMaintenance = null;
                        debugPrint("Vehicle " + plannerVehicle.id + " is leaving maintenance", currTime, false);
                    }

                    // If vehicle should schedule a failure
                    Time currTimeCopy = currTime.clone();
                    PlannerFailure matchingFailure = failures.stream().filter(
                        failure -> 
                            failure.vehiclePlaque.equals(plannerVehicle.plaque) &&
                            !failure.hasBeenAssigned() &&
                            (failure.shiftOccurredOn == Shift.T1 && currTimeCopy.getHour() >= 0 && currTimeCopy.getHour() < 8) ||
                            (failure.shiftOccurredOn == Shift.T2 && currTimeCopy.getHour() >= 8 && currTimeCopy.getHour() < 16) ||
                            (failure.shiftOccurredOn == Shift.T3 && currTimeCopy.getHour() >= 16 && currTimeCopy.getHour() < 24)
                            ).findFirst().orElse(null);
                    if (plannerVehicle.state != PlannerVehicle.VehicleState.STUCK &&
                    plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE &&
                     plannerVehicle.currentFailure == null &&
                      matchingFailure != null) {
                        List<Node> route = sol.routes.get(plannerVehicle.id);
                        if (route != null && route.size() > 0) {
                            List<Position> path = PathBuilder.buildPath(plannerVehicle.position, route.get(1).getPosition(), activeBlockages);
                            int distance = (int)(PathBuilder.calculateDistance(path) * (0.05 + Math.random() * 0.35));
                            if (distance > 0) {
                                plannerVehicle.minutesUntilFailure = distance;
                                plannerVehicle.currentFailure = matchingFailure;
                                debugPrint("Assigned failure to happen to vehicle " + plannerVehicle.id + " in " + plannerVehicle.minutesUntilFailure + " minutes", currTime, false);
                            }
                        }
                    }
                    // If vehicle should fail
                    else if (plannerVehicle.minutesUntilFailure <= 0 &&
                     plannerVehicle.currentFailure != null &&
                     plannerVehicle.state != PlannerVehicle.VehicleState.STUCK) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.STUCK;
                        plannerVehicle.currentFailure.timeOccuredOn = currTime;
                        plannerVehicle.currentPath = null;
                        debugPrint("Vehicle " + plannerVehicle.id + " has failed", currTime, false);
                    } 
                    // If vehicle stuck time has ended
                    else if (plannerVehicle.state == PlannerVehicle.VehicleState.STUCK &&
                     plannerVehicle.currentFailure != null &&
                     plannerVehicle.currentFailure.timeOccuredOn.addMinutes(plannerVehicle.currentFailure.type.getMinutesStuck()).isBefore(currTime)) {
                        PlannerWarehouse mainWarehouse = warehouses.stream().filter(warehouse -> warehouse.isMain).findFirst().orElse(null);
                        if (mainWarehouse == null) {
                            throw new RuntimeException("No main warehouse found");
                        }
                        List<Position> path = PathBuilder.buildPath(plannerVehicle.position, mainWarehouse.position, activeBlockages);

                        switch (plannerVehicle.currentFailure.type) {
                            case Ti1:
                                plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                                plannerVehicle.currentFailure = null;
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti1", currTime, false);
                                break;
                            case Ti2:
                                plannerVehicle.state = PlannerVehicle.VehicleState.RETURNING_TO_BASE;
                                Time failureTime = plannerVehicle.currentFailure.timeOccuredOn;
                                Time reincorporationTime;
                                
                                // Determine reincorporation time based on the shift
                                switch (plannerVehicle.currentFailure.shiftOccurredOn) {
                                    case T1:  // 00:00-08:00
                                        // Available in T3 of same day
                                        reincorporationTime = new Time(
                                            failureTime.getYear(),
                                            failureTime.getMonth(),
                                            failureTime.getDay(),
                                            16,  // T3 starts at 16:00
                                            0
                                        );
                                        break;
                                    case T2:  // 08:00-16:00
                                        // Available in T1 of next day
                                        reincorporationTime = new Time(
                                            failureTime.getYear(),
                                            failureTime.getMonth(),
                                            failureTime.getDay() + 1,
                                            0,  // T1 starts at 00:00
                                            0
                                        );
                                        break;
                                    case T3:  // 16:00-24:00
                                        // Available in T2 of next day
                                        reincorporationTime = new Time(
                                            failureTime.getYear(),
                                            failureTime.getMonth(),
                                            failureTime.getDay() + 1,
                                            8,  // T2 starts at 08:00
                                            0
                                        );
                                        break;
                                    default:
                                        throw new RuntimeException("Invalid shift");
                                }
                                
                                plannerVehicle.reincorporationTime = reincorporationTime;
                                plannerVehicle.currentFailure = null;
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti2, will be available at " + reincorporationTime, currTime, false);
                                break;
                            case Ti3:
                                plannerVehicle.state = PlannerVehicle.VehicleState.RETURNING_TO_BASE;
                                plannerVehicle.reincorporationTime = new Time(
                                    plannerVehicle.currentFailure.timeOccuredOn.getYear(),
                                    plannerVehicle.currentFailure.timeOccuredOn.getMonth(),
                                    plannerVehicle.currentFailure.timeOccuredOn.getDay() + 2,
                                    0,
                                    0
                                );
                                plannerVehicle.currentFailure = null;
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti3", currTime, false);
                                break;
                        }
                        plannerVehicle.currentPath = path;
                    }

                    if (plannerVehicle.state == PlannerVehicle.VehicleState.RETURNING_TO_BASE &&
                    plannerVehicle.reincorporationTime.isSameDate(currTime)) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                        plannerVehicle.currentFailure = null;
                        debugPrint("Vehicle " + plannerVehicle.id + " has finished repairing", currTime, false);
                    }
                    
                    if (plannerVehicle.minutesUntilFailure > 0) {
                        plannerVehicle.minutesUntilFailure--;
                        debugPrint("Vehicle " + plannerVehicle.id + " has " + plannerVehicle.minutesUntilFailure + " minutes until failure", currTime, false);
                    }

                    activeVehicles = getActiveVehicles(vehicles, currTime);
                    if (!activeVehicles.contains(plannerVehicle)) {
                        continue;
                    }

                    // If no path or path is empty, check if at next node; if not, build path
                    if (plannerVehicle.currentPath == null || plannerVehicle.currentPath.isEmpty() ) {
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
                        debugPrint("Vehicle " + plannerVehicle.id + " has arrived at location of node " + nextNode, currTime, false);
                        plannerVehicle.processNode(nextNode, plannerVehicle, activeOrders, warehouses, currTime);

                        if (plannerVehicle.nextNodeIndex == route.size() - 1) {
                            // Just processed the FinalNode
                            debugPrint("Vehicle " + plannerVehicle.id + " has reached final node", currTime, false);
                            plannerVehicle.state = PlannerVehicle.VehicleState.FINISHED;
                            plannerVehicle.nextNodeIndex++; // Optional: move index past end
                            continue;
                        }
                        plannerVehicle.nextNodeIndex++;
                        // No need to build path here; will do so on next iteration if needed
                    } else {
                        if (plannerVehicle.waitTransition > 0) {
                            debugPrint("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes", currTime, false);
                            plannerVehicle.waitTransition--;
                        } else {
                            //DEBUG
                            debugPrint(currTime + " Vehicle " + plannerVehicle.id + " is advancing path with state " + plannerVehicle.state, currTime, false);
                            //DEBUG
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
                minuteData.vehicles.addAll(vehicles.stream().map(vehicle -> new DataChunk.VehicleData(vehicle.plaque, vehicle.position, vehicle.state)).collect(Collectors.toList()));
                dataChunk.minutes.add(minuteData);

                activeBlockages = getActiveBlockages(blockages, currTime);
                SimulationVisualizer.draw(vehicles, activeBlockages, deliveryNodes, refillNodes, currTime);
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
