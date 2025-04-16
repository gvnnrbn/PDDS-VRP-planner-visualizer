import java.util.ArrayList;
import java.util.List;

public class Solution implements Cloneable {
    private class VehicleWithMovements implements Cloneable {
        int vehicleId;
        List<Movement> movements;

        @Override
        public VehicleWithMovements clone() {
            VehicleWithMovements clone = new VehicleWithMovements();
            clone.vehicleId = this.vehicleId;
            clone.movements = new ArrayList<Movement>(this.movements);
            return clone;
        }
    }

    private abstract class Movement implements Cloneable {
        List<PathFragment> pathFragments;

        public int calculateTimeSpentTravelling(Environment environment) {
            int timeSpent = 0;
            for (PathFragment pathFragment : pathFragments) {
                timeSpent += pathFragment.calculateTimeSpent(environment);
            }
            return timeSpent;
        }

        public abstract int calculateTotalTimeSpent(Environment environment);
    }

    private class DischargeMovement extends Movement {
        int GLPToDischarge;
        int orderId;

        @Override
        public DischargeMovement clone() {
            DischargeMovement clone = new DischargeMovement();
            clone.GLPToDischarge = this.GLPToDischarge;
            clone.orderId = this.orderId;
            return clone;
        }

        @Override
        public int calculateTotalTimeSpent(Environment environment) {
            return calculateTimeSpentTravelling(environment) + environment.dischargeSpeed * GLPToDischarge;
        }
    }

    private class LoadMovement extends Movement {
        int GLPToLoad;

        @Override
        public LoadMovement clone() {
            LoadMovement clone = new LoadMovement();
            clone.GLPToLoad = this.GLPToLoad;
            return clone;
        }

        @Override
        public int calculateTotalTimeSpent(Environment environment) {
            // Find the warehouse at the end of the path
            Position endPos = pathFragments.getLast().end;
            Warehouse warehouse = null;
            for (Warehouse w : environment.warehouses) {
                if (w.position.x == endPos.x && w.position.y == endPos.y) {
                    warehouse = w;
                    break;
                }
            }

            int loadTime;
            if (warehouse != null && warehouse.isBrokenVehicle) {
                loadTime = environment.transferSpeed * GLPToLoad;
            } else {
                loadTime = environment.chargeSpeed * GLPToLoad;
            }

            return calculateTimeSpentTravelling(environment) + loadTime;
        }
    }

    private class FuelLoadMovement extends Movement {
        int fuelToLoad;

        @Override
        public FuelLoadMovement clone() {
            FuelLoadMovement clone = new FuelLoadMovement();
            clone.fuelToLoad = this.fuelToLoad;
            return clone;
        }

        @Override
        public int calculateTotalTimeSpent(Environment environment) {
            return calculateTimeSpentTravelling(environment) + environment.fuelLoadSpeed * fuelToLoad;
        }
    }

    public List<VehicleWithMovements> vehiclesWithMovements = new ArrayList<VehicleWithMovements>();

    public int calculateFitness(Environment environment) {
        // Main factor, time left in each order
        int timeLeft = 0;
        for (Order order : environment.orders) {
            TimeMoment latestCompletionTime = null;
            
            // Find all completion times for this order
            for (VehicleWithMovements vehicle : vehiclesWithMovements) {
                TimeMoment currentTime = null;
                
                // Find the vehicle's start time
                for (Vehicle v : environment.vehicles) {
                    if (v.id == vehicle.vehicleId) {
                        currentTime = v.startAvailabilityMoment.clone();
                        break;
                    }
                }
                if (currentTime == null) continue;
                
                // Calculate time for each movement until we find the latest discharge
                for (Movement movement : vehicle.movements) {
                    if (movement instanceof DischargeMovement) {
                        DischargeMovement discharge = (DischargeMovement) movement;
                        if (discharge.orderId == order.id) {
                            TimeMoment completionTime = currentTime.clone();
                            completionTime.addMinutes(movement.calculateTotalTimeSpent(environment));
                            
                            if (latestCompletionTime == null || completionTime.compareTo(latestCompletionTime) > 0) {
                                latestCompletionTime = completionTime;
                            }
                        }
                    }
                    currentTime.addMinutes(movement.calculateTotalTimeSpent(environment));
                }
            }
            
            if (latestCompletionTime != null) {
                timeLeft += order.deadline.compareTo(latestCompletionTime);
            } else {
                timeLeft -= -1000_000; // If the order is not completed, we penalize it
            }
        }

        // Second factor, distance traveled
        int distanceTraveled = 0;
        for (VehicleWithMovements vehicle : vehiclesWithMovements) {
            for (Movement movement : vehicle.movements) {
                for (PathFragment pathFragment : movement.pathFragments) {
                    distanceTraveled += Math.abs(pathFragment.end.x - pathFragment.start.x) + Math.abs(pathFragment.end.y - pathFragment.start.y);
                }
            }
        }

        // To try to balance the two factors, we scale down distance traveled
        return timeLeft - distanceTraveled / 5;
    }

    public boolean isValid(Environment environment) {
        // First condition: All orders have been completed with the exact amount of GLP required
        for (Order order : environment.orders) {
            int totalGLPDelivered = 0;
            for (VehicleWithMovements vehicle : vehiclesWithMovements) {
                for (Movement movement : vehicle.movements) {
                    if (movement instanceof DischargeMovement) {
                        DischargeMovement discharge = (DischargeMovement) movement;
                        if (discharge.orderId == order.id) {
                            totalGLPDelivered += discharge.GLPToDischarge;
                        }
                    }
                }
            }
            if (totalGLPDelivered != order.GLPRequired) {
                return false;
            }
        }

        // Second condition: Orders should be completed before their deadline
        for (Order order : environment.orders) {
            boolean orderCompleted = false;
            for (VehicleWithMovements vehicle : vehiclesWithMovements) {
                // Find the vehicle's start time
                Vehicle correspondingVehicle = null;
                for (Vehicle v : environment.vehicles) {
                    if (v.id == vehicle.vehicleId) {
                        correspondingVehicle = v;
                        break;
                    }
                }
                if (correspondingVehicle == null) continue;

                TimeMoment currentTime = correspondingVehicle.startAvailabilityMoment.clone();
                
                for (Movement movement : vehicle.movements) {
                    if (movement instanceof DischargeMovement) {
                        DischargeMovement discharge = (DischargeMovement) movement;
                        if (discharge.orderId == order.id) {
                            TimeMoment completionTime = currentTime.clone();
                            completionTime.addMinutes(movement.calculateTimeSpentTravelling(environment));
                            if (completionTime.compareTo(order.deadline) > 0) {
                                return false;
                            }
                            orderCompleted = true;
                        }
                    }
                    currentTime.addMinutes(movement.calculateTotalTimeSpent(environment));
                }
            }
            if (!orderCompleted) {
                return false;
            }
        }

        // Third condition: Vehicles can't move during a maintenance
        for (Vehicle vehicle : environment.vehicles) {
            for (Maintenance maintenance : vehicle.maintenances) {
                for (VehicleWithMovements vehicleWithMovements : vehiclesWithMovements) {
                    if (vehicleWithMovements.vehicleId == vehicle.id) {
                        TimeMoment currentTime = vehicle.startAvailabilityMoment.clone();
                        
                        // Check if there are any movements during maintenance
                        for (Movement movement : vehicleWithMovements.movements) {
                            TimeMoment movementEndTime = currentTime.clone();
                            movementEndTime.addMinutes(movement.calculateTimeSpentTravelling(environment));
                            
                            // If movement overlaps with maintenance period
                            if ((currentTime.compareTo(maintenance.startTime) >= 0 && 
                                 currentTime.compareTo(maintenance.endTime) <= 0) ||
                                (movementEndTime.compareTo(maintenance.startTime) >= 0 && 
                                 movementEndTime.compareTo(maintenance.endTime) <= 0)) {
                                return false;
                            }
                            
                            currentTime.addMinutes(movement.calculateTotalTimeSpent(environment));
                        }
                        
                        // Check if vehicle can reach main warehouse before maintenance
                        if (!vehicleWithMovements.movements.isEmpty()) {
                            Movement lastMovement = vehicleWithMovements.movements.getLast();
                            TimeMoment lastMovementEnd = currentTime.clone();
                            
                            // Calculate time needed to return to main warehouse using shortest path
                            List<PathFragment> returnPath = environment.getShortestPath(
                                lastMovement.pathFragments.getLast().end,
                                environment.warehouses.get(0).position
                            );
                            
                            int returnTime = 0;
                            for (PathFragment fragment : returnPath) {
                                returnTime += fragment.calculateTimeSpent(environment);
                            }
                            
                            TimeMoment arrivalTime = lastMovementEnd.clone();
                            arrivalTime.minute += returnTime;
                            
                            if (arrivalTime.compareTo(maintenance.startTime) > 0) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        // Fourth condition: Vehicles can't move without fuel
        for (Vehicle vehicle : environment.vehicles) {
            for (VehicleWithMovements vehicleWithMovements : vehiclesWithMovements) {
                if (vehicleWithMovements.vehicleId == vehicle.id) {
                    int currentFuel = vehicle.currentFuel;
                    
                    for (Movement movement : vehicleWithMovements.movements) {
                        // Calculate fuel needed for movement
                        int fuelNeeded = 0;
                        for (PathFragment fragment : movement.pathFragments) {
                            fuelNeeded += fragment.calculateFuelSpent(vehicle, environment);
                        }
                        
                        // Check if we have enough fuel for this movement
                        if (fuelNeeded > currentFuel) {
                            return false;
                        }
                        
                        // Update fuel level after movement
                        currentFuel -= fuelNeeded;
                        
                        // If this is a fuel loading movement, add the loaded fuel
                        if (movement instanceof FuelLoadMovement) {
                            FuelLoadMovement fuelLoad = (FuelLoadMovement) movement;
                            currentFuel += fuelLoad.fuelToLoad;
                        }
                    }
                }
            }
        }

        // Fifth condition: Vehicles can't extract more GLP than they can carry
        for (Vehicle vehicle : environment.vehicles) {
            for (VehicleWithMovements vehicleWithMovements : vehiclesWithMovements) {
                if (vehicleWithMovements.vehicleId == vehicle.id) {
                    // Check if there are any movements that exceed vehicle's GLP capacity
                    for (Movement movement : vehicleWithMovements.movements) {
                        if (movement instanceof DischargeMovement) {
                            DischargeMovement discharge = (DischargeMovement) movement;
                            if (discharge.GLPToDischarge > vehicle.maxGLP) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        
        // Sixth condition: Vehicles can't extract GLP from a warehouse that can't provide it
        for (Vehicle vehicle : environment.vehicles) {
            for (VehicleWithMovements vehicleWithMovements : vehiclesWithMovements) {
                if (vehicleWithMovements.vehicleId == vehicle.id) {
                    // Check if there are any movements that exceed warehouse's GLP capacity
                    for (Movement movement : vehicleWithMovements.movements) {
                        if (movement instanceof LoadMovement) {
                            LoadMovement load = (LoadMovement) movement;
                            // Get the warehouse at the start position of the first path fragment
                            Position loadPosition = movement.pathFragments.get(0).start;
                            for (Warehouse warehouse : environment.warehouses) {
                                if (warehouse.position.equals(loadPosition)) {
                                    if (!warehouse.canLoadGLP(load.GLPToLoad)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Seventh condition: Vehicles can't extract fuel from a warehouse that can't provide it
        for (Vehicle vehicle : environment.vehicles) {
            for (VehicleWithMovements vehicleWithMovements : vehiclesWithMovements) {
                if (vehicleWithMovements.vehicleId == vehicle.id) {
                    // Check if there are any movements that exceed warehouse's fuel capacity
                    for (Movement movement : vehicleWithMovements.movements) {
                        if (movement instanceof FuelLoadMovement) {
                            FuelLoadMovement fuelLoad = (FuelLoadMovement) movement;
                            if (fuelLoad.fuelToLoad > vehicle.maxFuel) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public Solution clone() {
        Solution clone = new Solution();
        clone.vehiclesWithMovements = new ArrayList<VehicleWithMovements>();
        for (VehicleWithMovements vehicleWithMovements : this.vehiclesWithMovements) {
            clone.vehiclesWithMovements.add(vehicleWithMovements.clone());
        }
        return clone;
    }
}