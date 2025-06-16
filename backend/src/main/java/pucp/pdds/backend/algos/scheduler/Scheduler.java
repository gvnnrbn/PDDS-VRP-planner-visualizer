package pucp.pdds.backend.algos.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.algorithm.OrderDeliverNode;
import pucp.pdds.backend.algos.algorithm.ProductRefillNode;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.SimulationVisualizer;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.Position;

public class Scheduler {
    private SchedulerAgent agent;
    private int minutesToSimulate;

    private boolean isDebug = false;
    private boolean isVisualize = false;

    private Time currTime;

    private List<PlannerVehicle> vehicles;
    private List<PlannerOrder> orders;
    private List<PlannerBlockage> blockages;
    private List<PlannerWarehouse> warehouses;
    private List<PlannerFailure> failures;
    private List<PlannerMaintenance> maintenances;

    private List<PlannerBlockage> activeBlockages;
    private List<PlannerOrder> activeOrders;
    private List<PlannerMaintenance> activeMaintenances;
    private List<PlannerVehicle> activeVehicles;

    private int totalIntervals;

    public Scheduler(SchedulerAgent agent, Time currTime, int totalMinutes, int minutesToSimulate) {
        this.agent = agent;
        this.currTime = currTime;
        this.minutesToSimulate = minutesToSimulate;

        this.vehicles = agent.getVehicles();
        this.orders = agent.getOrders();
        this.blockages = agent.getBlockages();
        this.warehouses = agent.getWarehouses();
        this.failures = agent.getFailures();
        this.maintenances = agent.getMaintenances();

        this.totalIntervals = totalMinutes / minutesToSimulate;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public void setVisualize(boolean isVisualize) {
        this.isVisualize = isVisualize;
    }

    public void run() {
        for(int i=0; i<totalIntervals; i++) {
            updateActiveBlockages();
            updateActiveOrders();
            updateActiveMaintenances();
            updateActiveVehicles();

            Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
            debugPrint("Planning interval " + i + " started at " + currTime + " with " + activeVehicles.size() + " vehicles and " + activeOrders.size() + " orders");
            Algorithm algorithm = new Algorithm(isDebug);
            Solution sol = algorithm.run(environment, minutesToSimulate);
            debugPrint(sol.getReport());

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
            dataChunk.setBloqueos(DataChunk.convertBlockagesToDataChunk(activeBlockages));
            
            // Create a new SimulacionMinuto for each minute
            for (int iteration = 0; iteration < minutesToSimulate; iteration++) {
                DataChunk.SimulacionMinuto simulacionMinuto = new DataChunk.SimulacionMinuto(iteration);
                simulacionMinuto.setVehiculos(DataChunk.convertVehiclesToDataChunk(activeVehicles, sol.routes));
                simulacionMinuto.setAlmacenes(DataChunk.convertWarehousesToDataChunk(warehouses));
                simulacionMinuto.setPedidos(DataChunk.convertOrdersToDataChunk(activeOrders, activeVehicles, sol.routes, currTime));
                simulacionMinuto.setIncidencias(DataChunk.convertIncidentsToDataChunk(failures, activeMaintenances));
                dataChunk.getSimulacion().add(simulacionMinuto);

                debugPrint("--- Time: " + currTime + " ---");

                for (PlannerVehicle plannerVehicle : vehicles) {
                    // If vehicle should pass to maintenance
                    if (plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE && activeMaintenances.stream().anyMatch(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque))) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.MAINTENANCE;
                        plannerVehicle.currentMaintenance = activeMaintenances.stream().filter(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque)).findFirst().get();
                        debugPrint("Vehicle " + plannerVehicle.id + " is going into maintenance: " + plannerVehicle.currentMaintenance);
                    } 
                    
                    // If vehicle should leave maintenance
                    if (plannerVehicle.state == PlannerVehicle.VehicleState.MAINTENANCE && plannerVehicle.currentMaintenance.endDate.isBefore(currTime)) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                        plannerVehicle.currentMaintenance = null;
                        debugPrint("Vehicle " + plannerVehicle.id + " is leaving maintenance");
                    }

                    // If vehicle should schedule a failure
                    Time currTimeCopy = currTime.clone();
                    PlannerFailure matchingFailure = failures.stream().filter(
                        failure -> 
                            failure.vehiclePlaque.equals(plannerVehicle.plaque) &&
                            !failure.hasBeenAssigned() &&
                            (failure.shiftOccurredOn == PlannerFailure.Shift.T1 && currTimeCopy.getHour() >= 0 && currTimeCopy.getHour() < 8) ||
                            (failure.shiftOccurredOn == PlannerFailure.Shift.T2 && currTimeCopy.getHour() >= 8 && currTimeCopy.getHour() < 16) ||
                            (failure.shiftOccurredOn == PlannerFailure.Shift.T3 && currTimeCopy.getHour() >= 16 && currTimeCopy.getHour() < 24)
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
                                debugPrint("Assigned failure to happen to vehicle " + plannerVehicle.id + " in " + plannerVehicle.minutesUntilFailure + " minutes");
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
                        debugPrint("Vehicle " + plannerVehicle.id + " has failed");
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
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti1");
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
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti2, will be available at " + reincorporationTime);
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
                                debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti3");
                                break;
                        }
                        plannerVehicle.currentPath = path;
                    }

                    if (plannerVehicle.state == PlannerVehicle.VehicleState.RETURNING_TO_BASE &&
                    plannerVehicle.reincorporationTime.isSameDate(currTime)) {
                        plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                        plannerVehicle.currentFailure = null;
                        debugPrint("Vehicle " + plannerVehicle.id + " has finished repairing");
                    }
                    
                    if (plannerVehicle.minutesUntilFailure > 0) {
                        plannerVehicle.minutesUntilFailure--;
                        debugPrint("Vehicle " + plannerVehicle.id + " has " + plannerVehicle.minutesUntilFailure + " minutes until failure");
                    }

                    updateActiveVehicles();
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
                        debugPrint("Vehicle " + plannerVehicle.id + " has arrived at location of node " + nextNode);
                        plannerVehicle.processNode(nextNode, plannerVehicle, activeOrders, warehouses, currTime);

                        if (plannerVehicle.nextNodeIndex == route.size() - 1) {
                            // Just processed the FinalNode
                            debugPrint("Vehicle " + plannerVehicle.id + " has reached final node");
                            plannerVehicle.state = PlannerVehicle.VehicleState.FINISHED;
                            plannerVehicle.nextNodeIndex++; // Optional: move index past end
                            continue;
                        }
                        plannerVehicle.nextNodeIndex++;
                        // No need to build path here; will do so on next iteration if needed
                    } else {
                        if (plannerVehicle.waitTransition > 0) {
                            debugPrint("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
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
                        if (nextNode instanceof OrderDeliverNode) {
                            deliveryNodes.add(nextNode);
                        } else if (nextNode instanceof ProductRefillNode) {
                            refillNodes.add(nextNode);
                        }
                    }
                }

                updateActiveBlockages();
                if (isVisualize) {
                    SimulationVisualizer.draw(vehicles, activeBlockages, deliveryNodes, refillNodes, currTime, minutesToSimulate, warehouses);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currTime = currTime.addMinutes(1);
            }

            agent.export(dataChunk, i);
        }
    }

    private void updateActiveBlockages() {
        activeBlockages = blockages.stream()
            .filter(blockage -> blockage.isActive(currTime, currTime.addMinutes(minutesToSimulate)))
            .collect(Collectors.toList());
    }

    private void updateActiveOrders() {
        activeOrders = orders.stream()
            .filter(order -> order.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void updateActiveMaintenances() {
        activeMaintenances = maintenances.stream()
            .filter(maintenance -> maintenance.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void updateActiveVehicles() {
        activeVehicles = vehicles.stream()
            .filter(vehicle -> vehicle.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void debugPrint(String message) {
        if (isDebug) {
            System.out.println(currTime + " | " + message);
        }
    }
}