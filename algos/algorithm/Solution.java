package algorithm;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import entities.PlannerOrder;
import entities.PlannerVehicle;
import entities.PlannerWarehouse;
import utils.SimulationProperties;
import utils.Time;

public class Solution implements Cloneable {
    // Solution model
    public Map<Integer, List<Node>> routes; // routes[vehicleId] -> nodes

    private boolean hasRunSimulation = false;
    private boolean isFeasible = true;
    private double fitness = 0;

    private List<String> errors = new ArrayList<>();

    private long totalPossibleTimePoints = 0;
    private long totalTimePoints = 0;

    private double imaginaryFuelConsumed = 0;
    private double totalFuelCost = 0;

    private long imaginaryGLPConsumed = 0;
    private long totalGLPCost = 0;

    private int deliveredOrders = 0;
    private int deliveredOrdersOnTime = 0;
    private int totalOrders = 0;

    private static int weightTimePoints = 2;
    private static int weightImaginaryFuelConsumed = 10;
    private static int weightImaginaryGLPConsumed = 10;
    private static int weightOrdersNotDelivered = 2;
    private static int weightOrdersDeliveredOnTime = 1;

    public Solution() {
        routes = new HashMap<>();
    }

    @Override
    public Solution clone() {
        Solution clone = new Solution();

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
        return clone;
    }

    public double fitness(Environment environment) {
        if (!hasRunSimulation) {
            throw new RuntimeException("Solution has not been simulated yet.");
        }
        return fitness;
    }

    public boolean isFeasible(Environment environment) {
        if (!hasRunSimulation) {
            throw new RuntimeException("Solution has not been simulated yet.");
        }
        return isFeasible;
    }

    public void simulate(Environment environment, int minutes) {
        if (hasRunSimulation) {
            return;
        }

        totalOrders = environment.orders.size();

        // Clone orders
        Map<Integer, PlannerOrder> orderMap = new HashMap<>();
        for (PlannerOrder order : environment.orders) {
            orderMap.put(order.id, order.clone());
        }

        // Clone warehouses
        Map<Integer, PlannerWarehouse> warehouseMap = new HashMap<>();
        for (PlannerWarehouse warehouse : environment.warehouses) {
            warehouseMap.put(warehouse.id, warehouse.clone());
        }

        // Clone vehicles
        Map<Integer, PlannerVehicle> vehicleMap = new HashMap<>();
        for (PlannerVehicle vehicle : environment.vehicles) {
            vehicleMap.put(vehicle.id, vehicle.clone());
        }

        // Calculate max possible time points (sum deadline_i - currentTime)
        for (OrderDeliverNode deliverNode : environment.getNodes().stream()
            .filter(node -> node instanceof OrderDeliverNode)
            .map(node -> (OrderDeliverNode) node)
            .collect(Collectors.toList())) {
            totalPossibleTimePoints += environment.currentTime.minutesUntil(deliverNode.order.deadline);
        }

        for (PlannerVehicle vehicle : vehicleMap.values()) {
            List<Node> route = routes.get(vehicle.id);
            Time currentTime = environment.currentTime;

            if (vehicle.waitTransition > 0){
                currentTime = currentTime.addMinutes(vehicle.waitTransition);
            }


            for (int i = 0; i < route.size() - 1 && currentTime.minutesSince(environment.currentTime) < minutes; i++) {
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                // Pass time after traveling from originNode to destinationNode
                double distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
                int timeSpent = (int) Math.ceil(distance / SimulationProperties.speed) * 60; // Convert hours to minutes
                currentTime = currentTime.addMinutes(timeSpent);

                // Calculate and check fuel cost
                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
                if (vehicle.currentFuel < fuelCost) {
                    // Terminate the route
                    imaginaryFuelConsumed += fuelCost;
                    errors.add("Vehicle " + vehicle.id + " has not enough fuel to travel from " + originNode.getPosition() + " to " + destinationNode.getPosition() + ".");
                    break;
                } else {
                    // Consume fuel
                    vehicle.currentFuel -= fuelCost;
                    totalFuelCost += fuelCost;
                }

                // When the vehicle arrives at an OrderDeliverNode
                if (destinationNode instanceof OrderDeliverNode) {
                    OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                    PlannerOrder order = deliverNode.order;

                    int GLPToDeliver = deliverNode.amountGLP;

                    // Check if the vehicle has enough GLP to deliver the order
                    if (vehicle.currentGLP < GLPToDeliver) {
                        // Terminate the route
                        imaginaryGLPConsumed += GLPToDeliver;
                        errors.add("Vehicle " + vehicle.id + " has not enough GLP to deliver order " + order.id + ".");
                        break;
                    } else {
                        // Consume GLP and track the cost
                        vehicle.currentGLP -= GLPToDeliver;
                        totalGLPCost += GLPToDeliver; // Track successful GLP delivery cost
                    }

                    if (currentTime.isBefore(order.deadline)) {
                        totalTimePoints += currentTime.minutesUntil(order.deadline);
                    } else {
                        totalTimePoints -= currentTime.minutesSince(order.deadline);
                        errors.add("Vehicle " + vehicle.id + " has delivered order " + order.id + " after the deadline.");
                    }

                    if (order.amountGLP == GLPToDeliver) {
                        // Remove the order from the orderMap
                        if (currentTime.isBefore(order.deadline)) {
                            deliveredOrdersOnTime++;
                        } else {
                            errors.add("Vehicle " + vehicle.id + " has delivered order " + order.id + " after the deadline.");
                        }
                        orderMap.remove(order.id);
                    } else {
                        // Update the order amount
                        order.amountGLP -= GLPToDeliver;
                    }

                    // Pass time
                    currentTime = currentTime.addMinutes(SimulationProperties.timeAfterDelivery);
                }

                if (destinationNode instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) destinationNode;

                    // Refill the vehicle with GLP and full fuel
                    vehicle.currentGLP += refillNode.amountGLP;

                    // Pass time
                    currentTime = currentTime.addMinutes(SimulationProperties.timeAfterRefill);
                }
            }
        }

        deliveredOrders = environment.orders.size() - orderMap.size();

        double timePointsProportion = totalPossibleTimePoints > 0 ? 
            (totalTimePoints * 1.0 / totalPossibleTimePoints) : 0.0;
        double imaginaryFuelConsumedProportion = totalFuelCost > 0 ? 
            (imaginaryFuelConsumed * 1.0 / totalFuelCost) : 0.0;
        double imaginaryGLPConsumedProportion = totalGLPCost > 0 ? 
            (imaginaryGLPConsumed * 1.0 / totalGLPCost) : 0.0;
		double ordersNotDeliveredProportion = totalOrders > 0 ? 
            (orderMap.size() * 1.0 / totalOrders) : 0.0;
		double ordersNotDeliveredOnTimeProportion = totalOrders > 0 ? 
            (1 - deliveredOrdersOnTime * 1.0 / totalOrders) : 0.0;

		fitness = Solution.weightTimePoints * timePointsProportion -
			Solution.weightImaginaryFuelConsumed * imaginaryFuelConsumedProportion -
			Solution.weightImaginaryGLPConsumed * imaginaryGLPConsumedProportion -
			Solution.weightOrdersNotDelivered * ordersNotDeliveredProportion -
			Solution.weightOrdersDeliveredOnTime * ordersNotDeliveredOnTimeProportion;

        isFeasible = errors.isEmpty();

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
    }

    @Override
    public String toString() {
        return "PlannerSolution{" +
            "fitness=" + fitness +
            ", isFeasible=" + isFeasible +
            ", deliveredOrders=" + deliveredOrders +
            ", deliveredOrdersOnTime=" + deliveredOrdersOnTime +
            ", totalOrders=" + totalOrders +
            ", errors=" + errors +
            '}';
    }

    public String getReport() {
        StringBuilder report = new StringBuilder();
        
        if (!hasRunSimulation) {
            return "No simulation has been run yet.";
        }

        report.append("Feasibility: ").append(isFeasible ? "Is Feasible" : "Is Not Feasible").append("\n");
        report.append("Fitness: ").append(String.format("%.4f", fitness)).append("\n\n");
        
        report.append("Fitness Components:\n");
        report.append(String.format("  Time Points: %d/%d (%.4f)\n", 
            totalTimePoints, totalPossibleTimePoints, 
            totalPossibleTimePoints > 0 ? totalTimePoints * 1.0 / totalPossibleTimePoints : 0.0));
        report.append(String.format("  Imaginary Fuel Consumed: %.4f/%.4f (%.4f)\n", 
            imaginaryFuelConsumed, totalFuelCost, 
            totalFuelCost > 0 ? imaginaryFuelConsumed * 1.0 / totalFuelCost : 0.0));
        report.append(String.format("  Imaginary GLP Consumed: %d/%d (%.4f)\n", 
            imaginaryGLPConsumed, totalGLPCost, 
            totalGLPCost > 0 ? imaginaryGLPConsumed * 1.0 / totalGLPCost : 0.0));
        report.append(String.format("  Orders Not Delivered: %d/%d (%.4f)\n", 
            totalOrders - deliveredOrders, totalOrders, 
            totalOrders > 0 ? (totalOrders - deliveredOrders) * 1.0 / totalOrders : 0.0));
        report.append(String.format("  Orders Not Delivered On Time: %d/%d (%.4f)\n", 
            totalOrders - deliveredOrdersOnTime, totalOrders, 
            totalOrders > 0 ? (totalOrders - deliveredOrdersOnTime) * 1.0 / totalOrders : 0.0));

        if (!errors.isEmpty()) {
            report.append("\nErrors:\n");
            report.append(errors.stream().collect(Collectors.joining("\n")));
        } else {
            report.append("\nNo errors found.");
        }

        return report.toString();
    }
}