package pucp.pdds.backend.algos.scheduler;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.algos.utils.SimulationProperties;

public class SchedulerState {
    public SchedulerState(List<PlannerVehicle> vehicles, List<PlannerOrder> orders, List<PlannerBlockage> blockages, List<PlannerWarehouse> warehouses, List<PlannerFailure> failures, List<PlannerMaintenance> maintenances, Time currTime, int minutesToSimulate) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
        this.orders = orders != null ? orders : new ArrayList<>();
        this.blockages = blockages != null ? blockages : new ArrayList<>();
        this.warehouses = warehouses != null ? warehouses : new ArrayList<>();
        this.failures = failures != null ? failures : new ArrayList<>();
        this.maintenances = maintenances != null ? maintenances : new ArrayList<>();
        this.currTime = currTime;
        this.minutesToSimulate = minutesToSimulate;
    }

    private List<PlannerVehicle> vehicles = new ArrayList<>();
    private List<PlannerOrder> orders = new ArrayList<>();
    private List<PlannerBlockage> blockages = new ArrayList<>();
    private List<PlannerWarehouse> warehouses = new ArrayList<>();
    private List<PlannerFailure> failures = new ArrayList<>();
    private List<PlannerMaintenance> maintenances = new ArrayList<>();

    private Time currTime;
    public final int minutesToSimulate;

    public List<PlannerVehicle> getVehicles() {
        return vehicles;
    }

    public List<PlannerOrder> getOrders() {
        return orders;
    }

    public List<PlannerBlockage> getBlockages() {
        return blockages;
    }

    public List<PlannerWarehouse> getWarehouses() {
        return warehouses;
    }

    public List<PlannerFailure> getFailures() {
        return failures;
    }

    public List<PlannerMaintenance> getMaintenances() {
        return maintenances;
    }

    public Time getCurrTime() {
        return currTime;
    }

    public void setVehicles(List<PlannerVehicle> vehicles) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
    }

    public void setOrders(List<PlannerOrder> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
    }

    public void setBlockages(List<PlannerBlockage> blockages) {
        this.blockages = blockages != null ? blockages : new ArrayList<>();
    }

    public void setWarehouses(List<PlannerWarehouse> warehouses) {
        this.warehouses = warehouses != null ? warehouses : new ArrayList<>();
    }

    public void setFailures(List<PlannerFailure> failures) {
        this.failures = failures != null ? failures : new ArrayList<>();
    }

    public void setMaintenances(List<PlannerMaintenance> maintenances) {
        this.maintenances = maintenances != null ? maintenances : new ArrayList<>();
    }

    public void setCurrTime(Time currTime) {
        this.currTime = currTime;
    }

    public List<PlannerBlockage> getActiveBlockages() {
        return blockages.stream()
            .filter(blockage -> blockage.isActive(currTime, currTime.addMinutes(minutesToSimulate)))
            .collect(Collectors.toList());
    }

    public List<PlannerOrder> getActiveOrders() {
        return orders.stream()
            .filter(order -> order.isActive(currTime))
            .collect(Collectors.toList());
    }

    public List<PlannerMaintenance> getActiveMaintenances() {
        return maintenances.stream()
            .filter(maintenance -> maintenance.isActive(currTime))
            .collect(Collectors.toList());
    }

    public List<PlannerVehicle> getActiveVehicles() {
        return vehicles.stream()
            .filter(vehicle -> vehicle.isActive(currTime))
            .collect(Collectors.toList());
    }

    public void initializeVehicles() {
        for (PlannerVehicle plannerVehicle : getActiveVehicles()) {
            plannerVehicle.nextNodeIndex = 1;
            if (plannerVehicle.state == PlannerVehicle.VehicleState.FINISHED) {
                plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
            }
        }
    }

    public List<PlannerOrder> getPastOrders() {  
        return orders.stream().filter(order -> order.arrivalTime.isBeforeOrAt(currTime)).collect(Collectors.toList());
    }

    public synchronized void advance(Solution sol) {
        debugPrint("--- Time: " + currTime + " ---");

        if (currTime.getHour() == 0 && currTime.getMinute() == 0) {
            for(PlannerWarehouse warehouse : warehouses) {
                warehouse.currentGLP = warehouse.maxGLP;
            }
        }

        for (PlannerVehicle plannerVehicle : getActiveVehicles()) {
            // If vehicle should pass to maintenance
            if (plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE && getActiveMaintenances().stream().anyMatch(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque))) {
                plannerVehicle.state = PlannerVehicle.VehicleState.MAINTENANCE;
                plannerVehicle.currentMaintenance = getActiveMaintenances().stream().filter(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque)).findFirst().get();
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
                    List<Position> path = PathBuilder.buildPath(plannerVehicle.position, route.get(1).getPosition(), getActiveBlockages());
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
                List<Position> path = PathBuilder.buildPath(plannerVehicle.position, mainWarehouse.position, getActiveBlockages());

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

            if (!getActiveVehicles().contains(plannerVehicle)) {
                continue;
            }

            if (plannerVehicle.waitTransition > 0) {
                debugPrint("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
                plannerVehicle.waitTransition--;
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
                if (Math.abs(plannerVehicle.position.x - nextNode.getPosition().x) > 0.1 || Math.abs(plannerVehicle.position.y - nextNode.getPosition().y) > 0.1) {
                    // Not at node yet: build path to it
                    plannerVehicle.currentPath = PathBuilder.buildPath(plannerVehicle.position, nextNode.getPosition(), getActiveBlockages());
                    continue;
                }
                // Has arrived at location
                debugPrint("Vehicle " + plannerVehicle.id + " has arrived at location of node " + nextNode);
                plannerVehicle.processNode(nextNode, plannerVehicle, orders, warehouses, currTime);

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
                plannerVehicle.advancePath(SimulationProperties.speed / 60.0);
                plannerVehicle.state = PlannerVehicle.VehicleState.ONTHEWAY;
            }
        }

        currTime = currTime.addMinutes(1);
    }

    public SchedulerState clone() {
        List<PlannerVehicle> clonedVehicles = vehicles.stream()
            .map(PlannerVehicle::clone)
            .collect(Collectors.toList());
        List<PlannerOrder> clonedOrders = orders.stream()
            .map(PlannerOrder::clone)
            .collect(Collectors.toList());
        List<PlannerBlockage> clonedBlockages = blockages.stream()
            .map(PlannerBlockage::clone)
            .collect(Collectors.toList());
        List<PlannerWarehouse> clonedWarehouses = warehouses.stream()
            .map(PlannerWarehouse::clone)
            .collect(Collectors.toList());
        List<PlannerFailure> clonedFailures = failures.stream()
            .map(PlannerFailure::clone)
            .collect(Collectors.toList());
        List<PlannerMaintenance> clonedMaintenances = maintenances.stream()
            .map(PlannerMaintenance::clone)
            .collect(Collectors.toList());
        Time clonedTime = currTime.clone();

        return new SchedulerState(clonedVehicles, clonedOrders, clonedBlockages, clonedWarehouses, clonedFailures, clonedMaintenances, clonedTime, minutesToSimulate);
    }

    private void debugPrint(String message) {
        System.out.println(currTime + " | " + message);
    }
}
