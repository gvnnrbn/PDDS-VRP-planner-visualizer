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
        // Time startTime = state.currentTime;
        // Time endTime = startTime.addMinutes(minutesToSimulate);

        // Copy all mutable state objetcs
        // List<SchedulerOrder> updatedSchedulerOrders = new ArrayList<>(state.orders);
        // List<SchedulerVehicle> updatedSchedulerVehicles = new ArrayList<>();

        // Map<Integer, SchedulerOrder> orderMap = updatedSchedulerOrders.stream()
        //         .collect(Collectors.toMap(SchedulerOrder::id, o -> o));

        // Map<Integer, Warehouse> warehouseMap = state.warehouses.stream()
        //         .collect(Collectors.toMap(Warehouse::id, w -> w));


        /*
         * loop (i = 0; i <minutesToSImulate; i+=1) // probar primero con 1 min
                for (vehiculo in Vehiculos)
         * 
         */
        
        
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
    
                for (int i = 0; i < route.size() - 1; i++) {
                    Node from = route.get(i);
                    Node to = route.get(i + 1);
                    int nodesDIstance = Environment.calculateManhattanDistance(route.get.getPosition(), to.getPosition());
    
                    
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
                        int progress = Environment.calculateManhattanDistance(vehicle.position, to.getPosition());
                        double progressPercentage = (double) progress / totalDistance * 100;

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

                    ///////////////////////
                    // old
                    //////////////////////
                    int totalTime = (int) Math.ceil((double) totalDistance / Environment.speed * 60); // min
                    Time arrivalTime = currentTime.addMinutes(totalTime);
    
                    if (arrivalTime.isAfter(endTime)) // colapso
                        break;
    
                    // Avanzar el tiempo
                    currentTime = arrivalTime;
                    fuel -= Environment.calculateFuelCost(from, to, new Environment().getDistances(), vehicle); // simplified
                    currentPos = to.getPosition();
    
                    // Manejar entrega o recarga
                    if (to instanceof SchedulerOrderDeliverNode deliverNode) {
                        int deliverAmount = deliverNode.amountGLP;
    
                        if (glp >= deliverAmount) {
                            glp -= deliverAmount;
                            SchedulerOrder o = orderMap.get(deliverNode.order.id());
                            if (o != null) {
                                int remaining = o.amountGLP() - deliverAmount;
                                if (remaining <= 0) {
                                    orderMap.remove(o.id());
                                } else {
                                    orderMap.put(o.id(), new SchedulerOrder(o.id(), remaining, o.position(), o.deadline()));
                                }
                            }
                            currentTime = currentTime.addMinutes(Environment.timeAfterDelivery);
                        }
                    } else if (to instanceof ProductRefillNode refillNode) {
                        glp += refillNode.amountGLP;
                        fuel = vehicle.maxFuel();
                        currentTime = currentTime.addMinutes(Environment.timeAfterRefill);
                    }
                }
            }

            // Update state's time, warehouse, check if blockage still active
            state.currentTime.addMinutes(t);
        }


    }
}