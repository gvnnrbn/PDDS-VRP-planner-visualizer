package utils;

import domain.*;
import scheduler.EnumVehicleState;
import scheduler.ScheduleState;
import scheduler.SchedulerFailure;
import scheduler.SchedulerOrder;
import scheduler.SchedulerVehicle;
import scheduler.SchedulerWarehouse;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationEngine {
    public static void apply(Solution plan, ScheduleState state, int minutesToSimulate, int timeUnit, SchedulerWarehouse mainWarehouse) {      
        
        boolean vehicleNotAvailable = false;

        for (int t = 0; t < minutesToSimulate; t += timeUnit) {
            for (SchedulerVehicle vehicle : state.vehicles) {
                // check failure stage if occured
                if(vehicle.state == EnumVehicleState.STUCK && vehicle.failure.endStuckTime.isBefore(state.currentTime)) {
                        vehicle.state = EnumVehicleState.REPAIR;
                        vehicleNotAvailable = true; // en la siguiente llamada al algoritmo entra como warehouse??
                }
                if(vehicle.state == EnumVehicleState.REPAIR && vehicle.failure.endRepairTime.isBefore(state.currentTime)){
                    vehicle.state = EnumVehicleState.IDLE;
                    vehicle.failure = null;
                    vehicleNotAvailable = true;
                }
                if(vehicleNotAvailable){
                    continue; // vehicle without schedule. will be available in future scheduling iteration
                }

                // update vehicle's route
                List<Node> route = plan.routes.get(vehicle.id);
                if (route == null || route.size() < 2/* state.IDLE (didnt move) */) {
                    continue;
                }
    
                Position currentPos = vehicle.position;
                double fuel = vehicle.currentFuel;
                int glp = vehicle.currentGLP;
                Time currentTime = state.currentTime;
    
                // LOOP DE AVANCE: CARLOS
                for (int i = 0; i < route.size() - 1; i++) {
                    Node from = route.get(i);
                    Node to = route.get(i + 1);
                    int nodesDistance = Environment.calculateManhattanDistance(route.get.getPosition(), to.getPosition());
                    /* JORGE 
                    ADAPTAR para conseguir distancia y posicion real
                    + actualizar la posicion de vehicle

                     * public void generateDistances() {
                            Map<Position, Map<Position, Integer>> distances = new HashMap<>();
                            List<Position> positions = new ArrayList<>();
                            Set<Position> uniquePositions = new HashSet<>();
                            for (Node node : getNodes()) {
                                uniquePositions.add(node.getPosition());
                            }
                            positions.addAll(uniquePositions);

                            // Initialize the distance map for all positions
                            for (Position position : positions) {
                                distances.put(position, new HashMap<>());
                                // Set diagonal to 0
                                distances.get(position).put(position, 0);
                            }

                            // Only calculate distances for the upper triangle
                            for (int i = 0; i < positions.size(); i++) {
                                Position position = positions.get(i);
                                for (int j = i + 1; j < positions.size(); j++) {
                                    Position otherPosition = positions.get(j);
                                    int distance;
                                    if (isManhattanAvailable(position, otherPosition)) {
                                        distance = calculateManhattanDistance(position, otherPosition);
                                    } else {
                                        distance = calculateAStarDistance(position, otherPosition);
                                    }
                                    // Set distance in both directions
                                    distances.get(position).put(otherPosition, distance);
                                    distances.get(otherPosition).put(position, distance);
                                }
                            }

                            this.distances = distances;
                            areDistancesGenerated = true;
                        }
                     * 
                     */
                    
                    // totalDistance = vehicle position at t=0 to position at t=minutesToSimulate

                    /////////////////////////////////////////////////////
                    ///              FAILURE HANDLING
                    /////////////////////////////////////////////////////
                    SchedulerFailure failure = state.failures.stream()
                            .filter(f -> f.vehiclePlaque.equals(vehicle.plaque))
                            .findFirst()
                            .orElse(null);
                    if (failure != null && (failure.shiftOccurredOn-1)*8 < currentTime.getHour()) {
                        // Calculate the percentage of progress along the route
                        //int progress = Environment.calculateManhattanDistance(vehicle.position, to.getPosition());
                        //double progressPercentage = (double) progress / totalDistance * 100;

                        // Check if the progress is between 5% and 35%
                        if (progressPercentage >= 5 && progressPercentage <= 35) {
                            // Randomly determine if a failure occurs
                            boolean hasFailed = new Random().nextBoolean(); // Randomly true or false
                            if (hasFailed) {
                                System.out.println("\n Failure occurred for vehicle: " + vehicle.plaque + " at " + progressPercentage + "% of the route.");
                                // Set new state and return time for repair at main warehouse
                                vehicle.state = EnumVehicleState.STUCK;
                                int minutesToMainWarehouse = 0; // type 1 failure: repair time == 0
                                if(failure.type != 1) {
                                    int distanceToMainWarehouse = Environment.calculateManhattanDistance(vehicle.position, mainWarehouse.position);
                                    minutesToMainWarehouse = (int) Math.ceil((double) distanceToMainWarehouse / Environment.speed * 60);
                                }
                                vehicle.failure = failure.register(currentTime, minutesToMainWarehouse);
                                break; // next vehicle or timeUnit
                            }
                        }
                    }
                    
                    if(vehicle.transferMinutes > 0){
                        vehicle.transferMinutes -= timeUnit;
                        continue;
                    }

                    switch(currNode){
                        case OrderDeliverNode orderNode:
                            if(vehicle.position.equals(orderNode.getPosition())){
                                Node nextNode = plan.getNextNode(vehicle.id, orderNode);
                                while(true) {
                                    if(!orderNode.getPosition().equals(nextNode.getPosition())){
                                        break;
                                    }
                                    vehicle.currentGLP -= orderNode.amountGLP;
                                    SchedulerOrder order = state.orders.stream()
                                        .filter(o -> o.id == orderNode.order.id())
                                        .findFirst()
                                        .orElse(null);
                                    if(order != null) {
                                        order.amountGLP -= orderNode.amountGLP;
                                    }
                                    orderNode = (OrderDeliverNode) nextNode;
                                    nextNode = plan.getNextNode(vehicle.id, orderNode);
                                }
                                vehicle.transferMinutes = 15;
                            }
                            else{
                                // vehicle.position += vehicle.speed // abstracto todavía
                            }
                            break;
                        case ProductRefillNode refillNode:
                            if(vehicle.position.equals(refillNode.getPosition())){
                                Node nextNode = plan.getNextNode(vehicle.id, refillNode);
                                while(nextNode != null) {
                                    if(!refillNode.getPosition().equals(nextNode.getPosition())){
                                        break;
                                    }
                                    vehicle.currentGLP += refillNode.amountGLP;
                                    SchedulerWarehouse warehouse = state.warehouses.stream()
                                        .filter(w -> w.id == refillNode.warehouse.id())
                                        .findFirst()
                                        .orElse(null);
                                    if(warehouse != null) {
                                        warehouse.currentGLP -= refillNode.amountGLP;
                                    }
                                    nextNode = plan.getNextNode(vehicle.id, refillNode);
                                }
                                if(refillNode.warehouse.wasVehicle){ // wait 15 min if vehicle
                                    vehicle.transferMinutes = 15;
                                }
                                else{ // refill fuel if warehouse
                                    vehicle.currentFuel = vehicle.maxFuel;
                                }
                                refillNode = (ProductRefillNode) nextNode;
                            }
                            else{
                                // vehicle.position += vehicle.speed // abstracto todavía
                            }
                            break;
                        default:
                            // vehicle.position += vehicle.speed // abstracto todavía
                            break;
                    }                    
                }
            }

            // Update state's time, warehouse, check if blockage still active
            state.currentTime.addMinutes(timeUnit);
        }


    }
}