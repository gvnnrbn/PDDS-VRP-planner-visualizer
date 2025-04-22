package algorithms;
import java.util.ArrayList;
import java.util.List;

import domain.Environment;
import domain.Solution;
import domain.Vehicle;
import domain.Solution.VehicleWithMovements;

public class RandomSolutionInitializer implements SolutionInitializer {
    public Solution initializeSolution(Environment environment) {
        Solution sol = new Solution();
        sol.vehiclesWithMovements = new ArrayList<>();
        
        // Initialize vehicles with empty movement lists
        for (Vehicle vehicle : environment.vehicles) {
            Solution.VehicleWithMovements vwm = sol.new VehicleWithMovements();
            vwm.vehicleId = vehicle.id;
            vwm.movements = new ArrayList<>();
            sol.vehiclesWithMovements.add(vwm);
        }
        
        List<Integer> assignedOrders = new ArrayList<>();
        
        for (VehicleWithMovements vwm : sol.vehiclesWithMovements) {
            Solution.FuelLoadMovement fuelLoad = sol.new FuelLoadMovement();
            Vehicle vehicle = null;
            for (Vehicle v : environment.vehicles) {
                if (v.id == vwm.vehicleId) {
                    vehicle = v;
                    break;
                }
            }

            fuelLoad.fuelToLoad = vehicle.maxFuel - vehicle.currentFuel;
            fuelLoad.pathFragments = environment.getShortestPath(vehicle.position, environment.warehouses.get(0).position);

            vwm.movements.add(fuelLoad);
            
            Solution.LoadMovement glpLoad = sol.new LoadMovement();
            glpLoad.GLPToLoad = vehicle.maxGLP - vehicle.currentGLP;
            glpLoad.pathFragments = environment.getShortestPath(vehicle.position, environment.warehouses.get(0).position);
            vwm.movements.add(glpLoad);

            // Find next unassigned order
            int orderIndex = 0;
            while (orderIndex < environment.orders.size() && assignedOrders.contains(orderIndex)) {
                orderIndex++;
            }
            
            if (orderIndex < environment.orders.size()) {
                Solution.DischargeMovement pickup = sol.new DischargeMovement();
                pickup.GLPToDischarge = environment.orders.get(orderIndex).GLPRequired;
                pickup.pathFragments = environment.getShortestPath(environment.warehouses.get(0).position, environment.orders.get(orderIndex).position);
                vwm.movements.add(pickup);
                assignedOrders.add(orderIndex);
            }
        }
        
        return sol;
    }
}
