package utils;

import domain.*;
import scheduler.EnumVehicleState;
import scheduler.ScheduleState;
import scheduler.SchedulerFailure;
import scheduler.SchedulerMaintenance;
import scheduler.SchedulerOrder;
import scheduler.SchedulerVehicle;
import scheduler.SchedulerWarehouse;

import java.util.*;



public class SimulationEngine {
    private static final int ROUTINE_MAINTENANCE_time = 15; // minutes
    private static final int GLP_TRANSFER_TIME = 15; // minutes

    public static void apply(Solution plan, ScheduleState state, int minutesToSimulate, int timeUnit, SchedulerWarehouse mainWarehouse) {      
        
        for (SchedulerVehicle vehicle : state.vehicles){
            vehicle.currentNode = plan.routes.get(vehicle.id).get(0);
        }
        
        for (int t = 0; t < minutesToSimulate; t += timeUnit) {
            for (SchedulerVehicle vehicle : state.vehicles) {
                // wait for 'GLP transfer' or 'routine maintenance' to end 
                if(vehicle.waitTransition > 0){
                    vehicle.waitTransition -= timeUnit;
                    continue;
                }

                // Handle maintenances
                SchedulerMaintenance maintenance = state.maintenances.stream()
                        .filter(m -> m.vehiclePlaque.equals(vehicle.plaque))
                        .findFirst()
                        .orElse(null);
                if (maintenance != null && maintenance.startDate.isSameDate(state.currentTime)) {
                    vehicle.state = EnumVehicleState.MAINTENANCE;
                    vehicle.maintenance = maintenance;
                }

                // check if vehicle has planned route
                boolean noPlan = false;
                if(vehicle.state == EnumVehicleState.STUCK && vehicle.failure.endStuckTime.isBefore(state.currentTime)) {
                    vehicle.state = EnumVehicleState.REPAIR;
                    noPlan = true; // en la siguiente llamada al algoritmo entra como warehouse??
                }
                if(vehicle.state == EnumVehicleState.REPAIR && vehicle.failure.endRepairTime.isBefore(state.currentTime)){
                    vehicle.state = EnumVehicleState.IDLE;
                    vehicle.failure = null;
                    noPlan = true;
                }
                if(vehicle.state == EnumVehicleState.MAINTENANCE && vehicle.maintenance.endDate.isBeforeOrAt(state.currentTime)){
                    vehicle.state = EnumVehicleState.IDLE;
                    vehicle.maintenance = null;
                    noPlan = true;
                    
                }
                if(noPlan 
                || vehicle.state == EnumVehicleState.MAINTENANCE
                || vehicle.state == EnumVehicleState.STUCK
                || vehicle.state == EnumVehicleState.REPAIR
                || vehicle.state == EnumVehicleState.IDLE
                ) {
                    continue; // vehicle without schedule. will be available in future scheduling iteration
                }
                vehicle.state = EnumVehicleState.ONTHEWAY;

                // update vehicle's route
                List<Node> route = plan.routes.get(vehicle.id);
                if (route == null || route.size() < 2/* state.IDLE (didnt move) */) {
                    vehicle.state = EnumVehicleState.IDLE;
                    continue;
                }
    
                // Handle path
                if (vehicle.currentPath == null || vehicle.currentPath.isEmpty()) {
                    Node nextNode = plan.getNextNode(vehicle.id, vehicle.currentNode);

                    List<Blockage> blockages = state.blockages.stream()
                        .map(sb -> new Blockage(sb.vertices))
                        .toList();
                    List<Position> newPath = PathBuilder.buildPath(
                        vehicle.currentNode.getPosition(),nextNode.getPosition(),
                        blockages, Environment.gridLength, Environment.gridWidth);
                    vehicle.currentPath = newPath;
                }

                vehicle.advancePath(timeUnit);

                // /////////////////////////////////////////////////////
                // ///              FAILURE HANDLING
                // /////////////////////////////////////////////////////
                // SchedulerFailure failure = state.failures.stream()
                //         .filter(f -> f.vehiclePlaque.equals(vehicle.plaque))
                //         .findFirst()
                //         .orElse(null);
                // if (failure != null && vehicle.failure != null && (failure.shiftOccurredOn-1)*8 < currentTime.getHour()) {
                //     // Calculate the percentage of progress along the route
                //     //int progress = Environment.calculateManhattanDistance(vehicle.position, to.getPosition());
                //     //double progressPercentage = (double) progress / totalDistance * 100;
                //     vehicle.unitsOfTimeTilFailure = ...;
                //     vehicle.failure = failure;

                //     // Check if the progress is between 5% and 35%
                //     if (progressPercentage >= 5 && progressPercentage <= 35) {
                //         // Randomly determine if a failure occurs
                //         boolean hasFailed = new Random().nextBoolean(); // Randomly true or false
                //         if (hasFailed) {
                //             System.out.println("\n Failure occurred for vehicle: " + vehicle.plaque + " at " + progressPercentage + "% of the route.");
                //             // Set new state and return time for repair at main warehouse
                //             vehicle.state = EnumVehicleState.STUCK;
                //             int minutesToMainWarehouse = 0; // type 1 failure: repair time == 0
                //             if(failure.type != 1) {
                //                 int distanceToMainWarehouse = Environment.calculateManhattanDistance(vehicle.position, mainWarehouse.position);
                //                 minutesToMainWarehouse = (int) Math.ceil((double) distanceToMainWarehouse / Environment.speed * 60);
                //             }
                //             vehicle.failure = failure.register(currentTime, minutesToMainWarehouse);
                //             break; // next vehicle or timeUnit
                //         }
                //     }
                // }

                // handle delivery and refill
                switch(vehicle.currentNode){
                    case OrderDeliverNode orderNode:
                        if (!vehicle.position.equals(orderNode.getPosition())){
                            break;
                        }

                        // Precondition: vehicle has arrived to the expected node (currNode)
                        vehicle.currentPath = null; // To create a new one in next iteration

                        while (true) {
                            int deliveredGLP = orderNode.amountGLP;
                            int orderId = orderNode.order.id();

                            vehicle.currentGLP -= deliveredGLP;
                            state.orders.stream()
                                .filter(o -> o.id == orderId)
                                .findFirst()
                                .ifPresent(o -> {
                                    o.amountGLP -= deliveredGLP;
                                    if (o.amountGLP <= 0) {
                                        o.deliverTime = state.currentTime;
                                    }
                                });

                            Node nextNode = plan.getNextNode(vehicle.id, orderNode);
                            if (nextNode == null || 
                                !(nextNode instanceof OrderDeliverNode && nextNode.getPosition().equals(orderNode.getPosition())) ) {
                                break;
                            } 

                            orderNode = (OrderDeliverNode) nextNode;
                        }
                        vehicle.currentNode = orderNode;
                        vehicle.waitTransition = GLP_TRANSFER_TIME;

                        break;
                    case ProductRefillNode refillNode:
                        if (!vehicle.position.equals(refillNode.getPosition())){
                            break;
                        }

                        vehicle.currentPath = null; // To create a new one in next iteration

                        while (true) {
                            int refilledGLP = refillNode.amountGLP;
                            int warehouseId = refillNode.warehouse.id();
                            
                            vehicle.currentGLP += refillNode.amountGLP;
                            vehicle.currentGLP = Math.min(vehicle.currentGLP, vehicle.maxGLP);
                            state.warehouses.stream()
                                .filter(w -> w.id == warehouseId)
                                .findFirst()
                                .ifPresent(w -> w.currentGLP -= refilledGLP);

                            Node nextNode = plan.getNextNode(vehicle.id, refillNode);
                            if (nextNode == null || 
                                !(nextNode instanceof ProductRefillNode && nextNode.getPosition().equals(refillNode.getPosition())) ) {
                                break;
                            } 

                            refillNode = (ProductRefillNode) nextNode;
                        }
                        vehicle.currentNode = refillNode;
                        vehicle.waitTransition = GLP_TRANSFER_TIME;

                        break;
                    case FinalNode finalNode:
                        vehicle.state = EnumVehicleState.IDLE;
                        break;
                    case EmptyNode emptyNode:
                        break;
                    default:
                        break;
                    }    
                }
            }

            // Update state's time, warehouse, check if blockage still active
            state.currentTime.addMinutes(timeUnit);
        }

    }