package pucp.pdds.backend.algos.algorithm;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.Time;

public class Solution implements Cloneable {
    // --- Hyperparameters for Fitness Function ---
    // These constants must be tuned for the specific problem characteristics.
    private static final double V_BASE_GLP_DELIVERED = 10.0; // Base value per unit of GLP delivered on time
    private static final double V_EARLINESS_BONUS = 5.0;     // Bonus multiplier for early deliveries

    private static final double W_TARDINESS = 25.0;          // Penalty per minute of delay per unit of GLP
    private static final double W_UNDELIVERED_GLP = 100.0;     // Penalty per unit of undelivered GLP
    private static final double W_GLP_DEFICIT = 50.0;        // Penalty for not having enough GLP for a delivery
    private static final double W_FUEL_DEFICIT = 200.0;        // Penalty for not having enough fuel for a trip
    private static final double W_FINAL_FUEL_LEVEL = 150.0;  // Penalty for finishing with low fuel

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

    private List<String> errors = new ArrayList<>();

    public Solution(Environment environment) {
        routes = new HashMap<>();
        this.environment = environment;
    }

    @Override
    public Solution clone() {
        Solution clone = new Solution(this.environment);
        clone.routes = this.routes.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
            .map(Node::clone).collect(Collectors.toList())));
        clone.hasRunSimulation = false;
        clone.isFeasible = true;
        clone.fitness = 0;
        clone.startingTime = this.startingTime;
        return clone;
    }

    public double fitness() {
        if (!hasRunSimulation) {
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

        // --- Initialization for Fitness Calculation ---
        double deliveredValue = 0.0;
        double totalPenalty = 0.0;
        this.errors.clear();

        Map<Integer, PlannerOrder> orderMap = environment.orders.stream()
            .collect(Collectors.toMap(order -> order.id, PlannerOrder::clone));

        Map<Integer, PlannerVehicle> vehicleMap = environment.vehicles.stream()
            .collect(Collectors.toMap(vehicle -> vehicle.id, PlannerVehicle::clone));
        
        for (PlannerVehicle vehicle : vehicleMap.values()) {
            List<Node> route = routes.get(vehicle.id);
            if (route == null || route.isEmpty()) continue;

            Time currentTime = environment.currentTime;

            if (vehicle.waitTransition > 0){
                currentTime = currentTime.addMinutes(vehicle.waitTransition);
                vehicle.waitTransition = 0;
            }

            for (int i = 0; i < route.size() - 1; i++) {
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                double distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
                int timeSpent = (int) Math.ceil(distance / SimulationProperties.speed) * 60;
                currentTime = currentTime.addMinutes(timeSpent);

                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
                
                // --- Fuel Deficit Penalty ---
                if (vehicle.currentFuel < fuelCost) {
                    double fuelDeficit = fuelCost - vehicle.currentFuel;
                    totalPenalty += W_FUEL_DEFICIT * fuelDeficit;
                    errors.add("Vehicle " + vehicle.id + " fuel deficit: " + String.format("%.2f", fuelDeficit));
                }
                vehicle.currentFuel -= fuelCost;

                if (destinationNode instanceof OrderDeliverNode) {
                    OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                    PlannerOrder order = orderMap.get(deliverNode.order.id);
                    int GLPToDeliver = deliverNode.amountGLP;

                    // --- GLP Deficit Penalty ---
                    if (vehicle.currentGLP < GLPToDeliver) {
                        double glpDeficit = GLPToDeliver - vehicle.currentGLP;
                        totalPenalty += W_GLP_DEFICIT * glpDeficit;
                        errors.add("Vehicle " + vehicle.id + " GLP deficit for order " + order.id + ": " + glpDeficit);
                    }

                    vehicle.currentGLP -= GLPToDeliver;
                    order.amountGLP -= GLPToDeliver;

                    // --- Tardiness Penalty vs. Earliness Bonus ---
                    if (currentTime.isAfter(order.deadline)) {
                        long minutesLate = currentTime.minutesSince(order.deadline);
                        double tardinessPenalty = W_TARDINESS * minutesLate * GLPToDeliver;
                        
                        // Apply scaling factor for timesForgiven orders
                        double scalingFactor = Math.pow(2, order.timesForgiven);
                        tardinessPenalty *= scalingFactor;
                        
                        totalPenalty += tardinessPenalty;
                        errors.add("Order " + order.id + " delivered " + minutesLate + " minutes late. (Scaling: x" + String.format("%.1f", scalingFactor) + ")");
                    } else {
                        double timeHorizon = environment.minutesToSimulate; // Normalization factor
                        long minutesEarly = order.deadline.minutesSince(currentTime);
                        double deliveryValue = GLPToDeliver * (V_BASE_GLP_DELIVERED + V_EARLINESS_BONUS * (minutesEarly / timeHorizon));
                        
                        // Apply scaling factor for timesForgiven orders
                        double scalingFactor = Math.pow(2, order.timesForgiven);
                        deliveryValue *= scalingFactor;
                        
                        deliveredValue += deliveryValue;
                    }

                    boolean breaksOrderChain = !(originNode instanceof OrderDeliverNode) ||
                        (((OrderDeliverNode) originNode).order.id != deliverNode.order.id);

                    if (breaksOrderChain) {
                        currentTime = currentTime.addMinutes(SimulationProperties.timeAfterDelivery);
                    }
                }

                if (destinationNode instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) destinationNode;
                    vehicle.currentGLP += refillNode.amountGLP;
                    if (!refillNode.warehouse.wasVehicle) {
                        vehicle.currentFuel = vehicle.maxFuel;
                    }
                }
            }
        }

        // --- Post-simulation Penalties ---

        // Penalty for undelivered GLP
        for (PlannerOrder order : orderMap.values()) {
            if (order.amountGLP > 0) {
                double undeliveredPenalty = W_UNDELIVERED_GLP * order.amountGLP;
                
                // Apply scaling factor for timesForgiven orders
                double scalingFactor = Math.pow(2, order.timesForgiven);
                undeliveredPenalty *= scalingFactor;
                
                totalPenalty += undeliveredPenalty;
                errors.add("Order " + order.id + " has undelivered GLP: " + order.amountGLP + " (Scaling: x" + String.format("%.1f", scalingFactor) + ")");
            }
        }

        // Penalty for low final fuel
        for (PlannerVehicle vehicle : vehicleMap.values()) {
            double requiredFinalFuel = 0.3 * vehicle.maxFuel;
            if (vehicle.currentFuel < requiredFinalFuel) {
                double finalFuelDeficit = requiredFinalFuel - vehicle.currentFuel;
                totalPenalty += W_FINAL_FUEL_LEVEL * finalFuelDeficit;
                errors.add("Vehicle " + vehicle.id + " finished with low fuel. Deficit: " + String.format("%.2f", finalFuelDeficit));
            }
        }

        isFeasible = errors.isEmpty();
        this.fitness = deliveredValue - totalPenalty;

        // Clamp fitness to avoid numerical instability in certain algorithms
        if (!Double.isFinite(fitness)) {
            this.fitness = -1.0e12; 
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