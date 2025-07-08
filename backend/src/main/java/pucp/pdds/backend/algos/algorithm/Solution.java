package pucp.pdds.backend.algos.algorithm;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.Time;

public class Solution implements Cloneable {
    // Solution model
    public Map<Integer, List<Node>> routes; // routes[vehicleId] -> nodes

    private Environment environment;
    public Environment getEnvironment() {
        return environment;
    }

    private Time startingTime;

    private boolean hasRunSimulation = false;
    private boolean isFeasible = true;
    private double fitness = 0;

    // Add fields to store fitness components
    private double timeGLPPointsComponent = 0;
    private double fulfilledOrdersComponent = 0;
    private double imaginaryFuelComponent = 0;
    private double imaginaryGLPComponent = 0;
    private double missedOrdersComponent = 0;

    // Add fields to store the raw values
    private double rawTimeGLPPoints = 0;
    private int rawFulfilledOrders = 0;
    private double rawImaginaryFuel = 0;
    private int rawImaginaryGLP = 0;
    private int rawMissedOrders = 0;

    // Add normalization factors
    private static double maxTimeGLPPoints = 1000000.0;
    private static double maxOrders = 100.0;
    private static double maxFuel = 1000.0;
    private static double maxGLP = 1000.0;

    private List<String> errors = new ArrayList<>();

    private static int weightTimeGLPPoints = 10;
    private static int weightFulfilledOrders = 2;
    private static int weightImaginaryFuel = 2;
    private static int weightImaginaryGLP = 2;
    private static int weightMissedOrders = 10;

    // Add normalization helper methods
    private double normalizeTimeGLPPoints(double value) {
        return value / maxTimeGLPPoints;
    }

    private double normalizeOrders(double value) {
        return value / maxOrders;
    }

    private double normalizeFuel(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return 0.0;
        if (maxFuel == 0.0) return 0.0;
        return value / maxFuel;
    }

    private double normalizeGLP(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return 0.0;
        if (maxGLP == 0.0) return 0.0;
        return value / maxGLP;
    }

    public void updateNormalizationFactors() {
        // Calculate maximum possible time-GLP points
        maxTimeGLPPoints = 0;
        for (PlannerOrder order : environment.orders) {
            // Maximum points would be if delivered at start time
            double maxTimePoints = environment.currentTime.minutesUntil(order.deadline) * order.amountGLP;
            maxTimeGLPPoints = Math.max(maxTimePoints, maxTimePoints);
        }
        maxTimeGLPPoints *= environment.orders.size(); // Multiply by number of orders for total possible

        // Update max orders
        maxOrders = environment.orders.size();

        // Find maximum fuel and GLP capacity
        maxFuel = 0;
        maxGLP = 0;
        for (PlannerVehicle vehicle : environment.vehicles) {
            maxFuel = Math.max(maxFuel, vehicle.maxFuel);
            maxGLP = Math.max(maxGLP, vehicle.maxGLP);
        }

        // Add some margin to avoid edge cases
        maxTimeGLPPoints *= 1.2;
        maxFuel *= 1.2;
        maxGLP *= 1.2;
    }

    public Solution(Environment environment) {
        routes = new HashMap<>();
        this.environment = environment;
    }

    public void validate() {
        for (Map.Entry<Integer, List<Node>> entry : routes.entrySet()) {
            if (entry.getValue().size() < 2) {
                throw new RuntimeException("Solution has a route with less than 2 nodes. This should not happen.");
            }
            Node firstNode = entry.getValue().get(0);
            Node lastNode = entry.getValue().get(entry.getValue().size() - 1);

            if (!(firstNode instanceof EmptyNode)) {
                throw new RuntimeException("First node of route " + entry.getKey() + " is not an empty node. This should not happen.");
            }
            if (!(lastNode instanceof FinalNode)) {
                throw new RuntimeException("Last node of route " + entry.getKey() + " is not an empty node. This should not happen.");
            }

            List<Node> route = entry.getValue();
            long emptyNodes = route.stream().filter(n -> n instanceof EmptyNode).count();
            long finalNodes = route.stream().filter(n -> n instanceof FinalNode).count();

            if (emptyNodes != 1 || finalNodes != 1) {
                throw new RuntimeException("Solution has a route with more than one empty node or more than one final node. This should not happen.");
            }
        }
    }

    @Override
    public Solution clone() {
        Solution clone = new Solution(this.environment);

        // Deep copy of routes map with cloned nodes
        for (Map.Entry<Integer, List<Node>> entry : routes.entrySet()) {
            List<Node> clonedNodes = new ArrayList<>();
            for (Node node : entry.getValue()) {
                clonedNodes.add(node.clone());
            }
            clone.routes.put(entry.getKey(), clonedNodes);
        }
        clone.hasRunSimulation = false;
        clone.isFeasible = true;
        clone.fitness = 0;
        clone.startingTime = this.startingTime;
        return clone;
    }

    public double fitness() {
        if (!hasRunSimulation) {
            updateNormalizationFactors();  // Update factors before simulation
            simulate();
        }
        return fitness;
    }

    public boolean isFeasible() {
        if (!hasRunSimulation) {
            simulate();
        }
        return isFeasible;
    }

    public void simulate() {
        if (hasRunSimulation) {
            return;
        }

        // Store raw values
        rawTimeGLPPoints = 0;
        rawFulfilledOrders = 0;
        rawImaginaryFuel = 0;
        rawImaginaryGLP = 0;
        rawMissedOrders = 0;

        Map<Integer, PlannerOrder> orderMap = new HashMap<>();
        for (PlannerOrder order : environment.orders) {
            orderMap.put(order.id, order.clone());
        }

        for (PlannerOrder order : environment.orders) {
                order.deadline =  order.deadline.subtractMinutes(30);
        }

        Map<Integer, PlannerVehicle> vehicleMap = new HashMap<>();
        for (PlannerVehicle vehicle : environment.vehicles) {
            vehicleMap.put(vehicle.id, vehicle.clone());
        }

        // Find main warehouse position
        PlannerWarehouse mainWarehouse = null;
        for (PlannerWarehouse wh : environment.warehouses) {
            if (wh.isMain) {
                mainWarehouse = wh;
                break;
            }
        }
        if (mainWarehouse == null) {
            throw new RuntimeException("No main warehouse found");
        }
        Position mainWarehousePos = mainWarehouse.position;
        Map<PlannerOrder, Double> orderDistanceMap = new HashMap<>();
        for (PlannerOrder order : orderMap.values()) {
            double dist = environment.getDistances().get(mainWarehousePos).get(order.position);
            orderDistanceMap.put(order, dist);
        }

        // Sort orders by deadline (soonest first)
        List<PlannerOrder> sortedOrders = new ArrayList<>(orderMap.values());
        sortedOrders.sort((o1, o2) -> o1.deadline.compareTo(o2.deadline));

        // Map orderId to deadline-based priority weight: 5,4,3,2,1,1,1...
        Map<Integer, Integer> deadlinePriorityMap = new HashMap<>();
        for (int i = 0; i < sortedOrders.size(); i++) {
            int prio = 1;
            if (i == 0) prio = 5;
            else if (i == 1) prio = 4;
            else if (i == 2) prio = 3;
            else if (i == 3) prio = 2;
            deadlinePriorityMap.put(sortedOrders.get(i).id, prio);
        }

        // Sort orders by distance (farthest first)
        List<PlannerOrder> sortedByDistance = new ArrayList<>(orderMap.values());
        sortedByDistance.sort((o1, o2) -> Double.compare(orderDistanceMap.get(o2), orderDistanceMap.get(o1)));
        Map<Integer, Integer> distancePriorityMap = new HashMap<>();
        for (int i = 0; i < sortedByDistance.size(); i++) {
            int prio = 1;
            if (i == 0) prio = 5;
            else if (i == 1) prio = 4;
            else if (i == 2) prio = 3;
            else if (i == 3) prio = 2;
            distancePriorityMap.put(sortedByDistance.get(i).id, prio);
        }

        // Combine priorities
        Map<Integer, Integer> orderPriorityMap = new HashMap<>();
        for (PlannerOrder order : orderMap.values()) {
            int deadlinePrio = deadlinePriorityMap.getOrDefault(order.id, 1);
            int distancePrio = distancePriorityMap.getOrDefault(order.id, 1);
            orderPriorityMap.put(order.id, deadlinePrio + distancePrio);
        }

        for (PlannerVehicle vehicle : vehicleMap.values()) {
            List<Node> route = routes.get(vehicle.id);
            Time currentTime = environment.currentTime;

            if (vehicle.waitTransition > 0){
                currentTime = currentTime.addMinutes(vehicle.waitTransition);
                vehicle.waitTransition = 0;
            }

            for (int i = 0; i < route.size() - 1 && currentTime.minutesSince(environment.currentTime) < environment.minutesToSimulate; i++) {
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                double distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
                int timeSpent = (int) Math.ceil(distance / SimulationProperties.speed) * 60; // Convert hours to minutes
                currentTime = currentTime.addMinutes(timeSpent);

                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
                if (vehicle.currentFuel < fuelCost) {
                    errors.add("Vehicle " + vehicle.id + " has not enough fuel to travel from " + originNode.getPosition() + " to " + destinationNode.getPosition() + ".");
                    rawImaginaryFuel += fuelCost - vehicle.currentFuel;
                } 

                vehicle.currentFuel -= fuelCost;

                if (destinationNode instanceof OrderDeliverNode) {
                    OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                    PlannerOrder order = orderMap.get(deliverNode.order.id);

                    int GLPToDeliver = deliverNode.amountGLP;

                    if (vehicle.currentGLP < GLPToDeliver) {
                        errors.add("Vehicle " + vehicle.id + " has not enough GLP to deliver order " + order.id + ".");
                        rawImaginaryGLP += GLPToDeliver - vehicle.currentGLP;
                    } 

                    vehicle.currentGLP -= GLPToDeliver;
                    order.amountGLP -= GLPToDeliver;

                    if (currentTime.isAfter(order.deadline)) {
                        errors.add("Vehicle " + vehicle.id + " has delivered order " + order.id + " after the deadline.");
                    } 

                    // --- Exponential decay for lateness (reward on-time, penalize late) ---
                    double minutesLate = Math.max(0, currentTime.minutesSince(order.deadline));
                    double alpha = 0.01; // Decay rate, tune as needed
                    // Use priority weight, do NOT multiply by amountGLP
                    int priorityWeight = orderPriorityMap.getOrDefault(order.id, 1);
                    rawTimeGLPPoints += priorityWeight * Math.exp(-alpha * minutesLate);
                    // This rewards on-time/early deliveries, penalizes late ones smoothly

                    // If destination node breaks an order chain (changes order or goes from order to non-order node), wait the corresponding time
                    boolean breaksOrderChain = (originNode instanceof OrderDeliverNode && !(destinationNode instanceof OrderDeliverNode)) ||
                    (originNode instanceof OrderDeliverNode && destinationNode instanceof OrderDeliverNode && ((OrderDeliverNode) originNode).order.id != ((OrderDeliverNode) destinationNode).order.id);

                    if (breaksOrderChain) {
                        currentTime = currentTime.addMinutes(SimulationProperties.timeAfterDelivery);
                    }

                }

                if (destinationNode instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) destinationNode;

                    // Refill the vehicle with GLP
                    vehicle.currentGLP += refillNode.amountGLP;

                    // Full fuel refill if not a vehicle
                    if (!refillNode.warehouse.wasVehicle) {
                        vehicle.currentFuel = vehicle.maxFuel;
                    }
                }
            }
        }

        Time timeAfterSimulation = environment.currentTime.addMinutes(environment.minutesToSimulate);
        for (PlannerOrder order : sortedOrders) {
            if (order.amountGLP > 0 && timeAfterSimulation.isAfter(order.deadline)) {
                rawMissedOrders++;
            }
            if (order.amountGLP == 0) {
                rawFulfilledOrders++;
            }
        }

        for (PlannerVehicle vehicle : vehicleMap.values()) {
            if (vehicle.currentFuel < 0.3 * vehicle.maxFuel) {
                errors.add("Vehicle " + vehicle.id + " has not enough fuel to continue.");
            }
        }

        isFeasible = errors.isEmpty();

        // --- Fitness Calculation (Redesigned & Extended) ---
        // Tunable weights for new components
        double w1 = 1.0; // expReward
        double w2 = 2.0; // sigmoidOrders
        double w3 = 1.5; // logImaginaryFuel
        double w4 = 1.5; // logImaginaryGLP
        double w5 = 3.0; // missedOrdersPenalty
        double w6 = 2.0; // vehicle utilization bonus
        double w7 = 1.0; // end-period GLP bonus

        // 1. Normalize expReward
        double maxExpReward = 0.0;
        for (PlannerOrder order : sortedOrders) {
            int priorityWeight = orderPriorityMap.getOrDefault(order.id, 1);
            maxExpReward += priorityWeight;
        }
        if (maxExpReward <= 0) maxExpReward = 1.0; // Prevent division by zero
        double expRewardNorm = rawTimeGLPPoints / maxExpReward;

        // 2. Logistic (sigmoid) for fulfilled orders (smoothly in [0,1])
        double sigmoidOrders = 1.0 / (1.0 + Math.exp(-0.5 * (rawFulfilledOrders - environment.orders.size() / 2.0)));

        // 3. Smoother sigmoid penalty for missed orders
        int nOrders = environment.orders.size();
        if (nOrders <= 0) nOrders = 1;
        double beta = 1.0; // Controls steepness; tune as needed
        double missedOrdersPenalty = 1.0 / (1.0 + Math.exp(-beta * (rawMissedOrders - nOrders / 2.0)));

        // 4. Normalize logImaginaryFuel/GLP
        double maxFuel = 0.0, maxGLP_fleet = 0.0;
        for (PlannerVehicle v : vehicleMap.values()) {
            maxFuel += v.maxFuel;
            maxGLP_fleet += v.maxGLP;
        }
        if (maxFuel <= 0) maxFuel = 1.0;
        if (maxGLP_fleet <= 0) maxGLP_fleet = 1.0;
        double logImaginaryFuelNorm = (Double.isFinite(rawImaginaryFuel) && Math.log1p(maxFuel) > 0) ? rawImaginaryFuel / Math.log1p(maxFuel) : 0.0;
        double logImaginaryGLPNorm = (Double.isFinite(rawImaginaryGLP) && Math.log1p(maxGLP_fleet) > 0) ? rawImaginaryGLP / Math.log1p(maxGLP_fleet) : 0.0;

        // 5. Vehicle utilization bonus: reward for using more vehicles (not idle/finished)
        int usedVehicles = 0;
        for (List<Node> route : routes.values()) {
            if (route.size() > 2) {
                usedVehicles++;
            }
        }
        double utilizationBonus = usedVehicles / (double) Math.max(1, environment.vehicles.size()); // [0,1]
        // Optionally, use sigmoid for smoother reward: 1/(1+exp(-4*(utilization-0.5)))

        // 6. End-period GLP bonus: reward for finishing with more GLP in vehicles
        double totalGLP = 0.0;
        double maxGLP = 0.0;
        for (PlannerVehicle v : vehicleMap.values()) {
            totalGLP += v.currentGLP;
            maxGLP += v.maxGLP;
        }
        double glpBonus = (maxGLP > 0) ? (totalGLP / maxGLP) : 0.0; // [0,1]

        // Bonus for first node close to vehicle's actual position
        double firstNodeBonus = 0.0;
        double firstNodeThreshold = 1.0; // distance units
        double firstNodeBonusValue = 0.5;
        for (PlannerVehicle v : environment.vehicles) {
            List<Node> route = routes.get(v.id);
            if (route != null && !route.isEmpty()) {
                Node firstNode = route.get(0);
                double dist = environment.getDistances().get(v.initialPosition).get(firstNode.getPosition());
                if (dist < firstNodeThreshold) {
                    firstNodeBonus += firstNodeBonusValue;
                }
            }
        }

        // 7. Fitness (weights as before, now using normalized components)
        fitness = w1 * expRewardNorm
                + w2 * sigmoidOrders
                - w3 * logImaginaryFuelNorm
                - w4 * logImaginaryGLPNorm
                - w5 * missedOrdersPenalty
                + w6 * utilizationBonus
                + w7 * glpBonus
                + firstNodeBonus;
        if (isFeasible) fitness *= 1.5; // Smoother feasible bonus

        // --- Robustness: Clamp fitness to avoid -Infinity/NaN ---
        if (!Double.isFinite(fitness) || fitness < -1e10) {
            fitness = -1e6; // Assign a large negative value, but not -Infinity
        }

        hasRunSimulation = true;
    }

    public void compress(){
        if (routes == null){
            return;
        }

        for (List<Node> route : routes.values()){
            if (route.size() < 2){
                continue;
            }

            for (int i = 0; i < route.size() - 1; i++){
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                // Compress order deliver nodes
                if (originNode instanceof OrderDeliverNode && destinationNode instanceof OrderDeliverNode){
                    if (((OrderDeliverNode) originNode).order.id == ((OrderDeliverNode) destinationNode).order.id){
                        route.remove(i + 1);
                        i--;
                        ((OrderDeliverNode) originNode).amountGLP += ((OrderDeliverNode) destinationNode).amountGLP;
                    }
                }    

                // Compress product refill nodes
                if (originNode instanceof ProductRefillNode && destinationNode instanceof ProductRefillNode){
                    if (((ProductRefillNode) originNode).warehouse.id == ((ProductRefillNode) destinationNode).warehouse.id){
                        route.remove(i + 1);
                        i--;
                        ((ProductRefillNode) originNode).amountGLP += ((ProductRefillNode) destinationNode).amountGLP;
                    }
                }
            }
        }
        // Enforce invariant after compress
        // NOTE: This requires an Environment parameter, so compress should accept it
    }

    public void enforceRouteInvariant(Environment environment) {
        if (routes == null) return;
        for (Map.Entry<Integer, List<Node>> entry : routes.entrySet()) {
            int vehicleId = entry.getKey();
            List<Node> route = entry.getValue();
            if (route == null || route.isEmpty()) continue;
            // Remove all EmptyNode and FinalNode except at ends
            route.removeIf(n -> (n instanceof EmptyNode || n instanceof FinalNode));
            // Add correct EmptyNode at start
            Node emptyNode = environment.getNodes().stream()
                .filter(n -> n instanceof EmptyNode && n.getPosition().equals(environment.vehicles.stream().filter(v -> v.id == vehicleId).findFirst().get().initialPosition))
                .findFirst().orElse(null);
            if (emptyNode != null) route.add(0, emptyNode);
            // Add correct FinalNode at end
            Node finalNode = environment.getNodes().stream()
                .filter(n -> n instanceof FinalNode)
                .findFirst().orElse(null);
            if (finalNode != null) route.add(finalNode);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Solution {\n");
        sb.append(getReport());  // Get all the common information
        
        // Add routes information
        sb.append("\n  Routes:\n");
        if (routes != null) {
            routes.forEach((vehicleId, route) -> {
                sb.append("    Vehicle ").append(vehicleId).append(": ");
                if (route.isEmpty()) {
                    sb.append("Empty route");
                } else {
                    route.forEach(node -> sb.append(node).append(" → "));
                    // Remove the last arrow
                    sb.setLength(sb.length() - 3);
                }
                sb.append("\n");
            });
        } else {
            sb.append("    No routes defined\n");
        }
        
        sb.append("}");

        sb.append("\n  Environment:\n");
        sb.append(environment);

        return sb.toString();
    }

    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        if (!hasRunSimulation) {
            return "No simulation has been run yet.";
        }

        report.append("  Simulation Status: ").append(hasRunSimulation ? "Completed" : "Not Run").append("\n");
        report.append("  Feasibility: ").append(isFeasible ? "Feasible" : "Not Feasible").append("\n");
        report.append("  Total Fitness: ").append(String.format("%.4f", fitness)).append("\n");
        report.append("  Fitness Components:\n");
        report.append(String.format("    - Time GLP Points (weight=%d): %.4f (based on %.2f minutes×GLP, normalized: %.4f)\n", 
            weightTimeGLPPoints, timeGLPPointsComponent, rawTimeGLPPoints, normalizeTimeGLPPoints(rawTimeGLPPoints)));
        report.append(String.format("    - Fulfilled Orders (weight=%d): %.4f (based on %d orders, normalized: %.4f)\n", 
            weightFulfilledOrders, fulfilledOrdersComponent, rawFulfilledOrders, normalizeOrders(rawFulfilledOrders)));
        report.append(String.format("    - Imaginary Fuel (weight=%d): %.4f (based on %.2f liters, normalized: %.4f)\n", 
            weightImaginaryFuel, imaginaryFuelComponent, rawImaginaryFuel, normalizeFuel(rawImaginaryFuel)));
        report.append(String.format("    - Imaginary GLP (weight=%d): %.4f (based on %d units, normalized: %.4f)\n", 
            weightImaginaryGLP, imaginaryGLPComponent, rawImaginaryGLP, normalizeGLP(rawImaginaryGLP)));
        report.append(String.format("    - Missed Orders (weight=%d): %.4f (based on %d orders, normalized: %.4f)\n", 
            weightMissedOrders, missedOrdersComponent, rawMissedOrders, normalizeOrders(rawMissedOrders)));
        
        if (!errors.isEmpty()) {
            report.append("\n  Errors:\n");
            errors.forEach(error -> report.append("    - ").append(error).append("\n"));
        }

        return report.toString();
    }

    public Time getStartingTime() {
        return startingTime;
    }

    public void setStartingTime(Time startingTime) {
        this.startingTime = startingTime;
    }
}