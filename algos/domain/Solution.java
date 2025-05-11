package domain;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Solution implements Cloneable {
    public Map<Integer, List<Node>> routes; // routes[vehicleId] -> nodes

    private boolean hasRunSimulation = false;
    private boolean isFeasible = true;
    private double fitness = 0;

    private List<String> errors = new ArrayList<>();

    private long totalPossibleTimePoints = 0;
    private long totalTimePoints = 0;

    private double totalFuelCost = 0;
    private double imaginaryFuelConsumed = 0;

    private long totalGLPCost = 0;
    private long imaginaryGLPConsumed = 0;

    private int deliveredOrders = 0;
    private int deliveredOrdersOnTime = 0;
    private int totalSpareTime = 0;
    private int totalOrders = 0;

    // Metrics
    private double ordersDeliveredBeforeDeadlinePercentage;
    private double averageSpareTime;

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
            simulate(environment);
        }

        return fitness;
    }

    public boolean isFeasible(Environment environment) {
        if (!hasRunSimulation) {
            simulate(environment);
        }
        return isFeasible;
    }

    public double getAverageSpareTime(Environment environment) {
        if (!hasRunSimulation) {
            simulate(environment);
        }
        return averageSpareTime;
    }

    public double getOrdersDeliveredBeforeDeadlinePercentage(Environment environment) {
        if (!hasRunSimulation) {
            simulate(environment);
        }
        return ordersDeliveredBeforeDeadlinePercentage;
    }

    private void simulate(Environment environment) {
        totalOrders = environment.orders.size();

        // Clone orders
        Map<Integer, Order> orderMap = new HashMap<>();
        for (Order order : environment.orders) {
            orderMap.put(order.id(), order);
        }

        // Clone warehouses
        Map<Integer, Warehouse> warehouseMap = new HashMap<>();
        for (Warehouse warehouse : environment.warehouses) {
            warehouseMap.put(warehouse.id(), warehouse);
        }

        // Calculate max possible time points (sum deadline_i - currentTime)
        for (OrderDeliverNode deliverNode : environment.getNodes().stream().filter(node -> node instanceof OrderDeliverNode).map(node -> (OrderDeliverNode) node).collect(Collectors.toList())) {
            totalPossibleTimePoints += environment.currentTime.minutesUntil(deliverNode.order.deadline());
        }

        for (Vehicle vehicle : environment.vehicles) {
            List<Node> route = routes.get(vehicle.id());
            Time currentTime = environment.currentTime;

            for (int i = 0; i < route.size() - 1; i++) {
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                // If destination node breaks an order chain (changes order or goes from order to non-order node), wait the corresponding time
                boolean breaksOrderChain = (originNode instanceof OrderDeliverNode && !(destinationNode instanceof OrderDeliverNode)) ||
                 (originNode instanceof OrderDeliverNode && destinationNode instanceof OrderDeliverNode && ((OrderDeliverNode) originNode).order.id() != ((OrderDeliverNode) destinationNode).order.id());

                if (breaksOrderChain) {
                    currentTime = currentTime.addMinutes(Environment.timeAfterDelivery);
                }

                // If it breaks a refill chain from a wasVehicle node
                boolean isCurrentlyARefillFromVehicle = 
                    (originNode instanceof ProductRefillNode && ((ProductRefillNode) originNode).warehouse.wasVehicle());
                boolean isNextNodeARefillFromSameVehicle = 
                    (destinationNode instanceof ProductRefillNode && ((ProductRefillNode) destinationNode).warehouse.wasVehicle())
                    && originNode.getPosition() == destinationNode.getPosition();
                boolean breaksRefillFromVehicleChain =
                    (isCurrentlyARefillFromVehicle && !isNextNodeARefillFromSameVehicle);

                if (breaksRefillFromVehicleChain) {
                    currentTime = currentTime.addMinutes(Environment.timeAfterRefill);
                }

                // Pass time after traveling from originNode to destinationNode
                int distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
                int timeSpent = (int) Math.ceil((double) distance / Environment.speed) * 60; // Convert hours to minutes
                currentTime = currentTime.addMinutes(timeSpent);

                // Calculate and check fuel cost
                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
                if (vehicle.currentFuel() < fuelCost) {
                    // Terminate the route
                    imaginaryFuelConsumed += fuelCost;
                    errors.add("Vehicle " + vehicle.id() + " has not enough fuel to travel from " + originNode.getPosition() + " to " + destinationNode.getPosition() + ".");
                    break;
                } else {
                    // Consume fuel
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.currentFuel() - fuelCost, vehicle.maxGLP(), vehicle.currentGLP(), vehicle.initialPosition());
                }
                totalFuelCost += fuelCost;

                // When the vehicle arrives at an OrderDeliverNode
                if (destinationNode instanceof OrderDeliverNode) {
                    OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                    Order order = deliverNode.order;

                    int GLPToDeliver = deliverNode.amountGLP;

                    // Check if the vehicle has enough GLP to deliver the order
                    if (vehicle.currentGLP() < GLPToDeliver) {
                        // Terminate the route
                        imaginaryGLPConsumed += GLPToDeliver;
                        errors.add("Vehicle " + vehicle.id() + " has not enough GLP to deliver order " + order.id() + ".");
                        break;
                    } else {
                        // Consume GLP
                        vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.currentFuel(), vehicle.maxGLP(), vehicle.currentGLP() - GLPToDeliver, vehicle.initialPosition());
                    }
                    totalGLPCost += GLPToDeliver;

                    if (currentTime.isBefore(order.deadline())) {
                        totalTimePoints += currentTime.minutesUntil(order.deadline());
                    } else {
                        totalTimePoints -= currentTime.minutesSince(order.deadline());
                        errors.add("Vehicle " + vehicle.id() + " has delivered order " + order.id() + " after the deadline.");
                    }

                    // Deliver the order amount
                    if (orderMap.get(order.id()).amountGLP() == GLPToDeliver) {
                        // Remove the order from the orderMap
                        if (currentTime.isBefore(order.deadline())) {
                            deliveredOrdersOnTime++;
                            totalSpareTime += currentTime.minutesUntil(order.deadline());
                        } else {
                            errors.add("Vehicle " + vehicle.id() + " has delivered order " + order.id() + " after the deadline.");
                        }
                        orderMap.remove(order.id());
                    } else {
                        // Update the order amount
                        Order updatedOrder = new Order(order.id(), orderMap.get(order.id()).amountGLP() - GLPToDeliver, order.position(), order.deadline());
                        orderMap.put(order.id(), updatedOrder);
                    }
                }

                // When the vehicle arrives at a ProductRefillNode
                if (destinationNode instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) destinationNode;

                    // Refill the vehicle with GLP and full fuel
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.maxFuel(), vehicle.maxGLP(), vehicle.currentGLP() + refillNode.amountGLP, vehicle.initialPosition());
                }
            }
        }

        // Check if all vehicles have arrived at the final node
        for (Vehicle vehicle : environment.vehicles) {
            List<Node> route = routes.get(vehicle.id());
            if (!(route.get(route.size() - 1) instanceof FinalNode)) {
                errors.add("Vehicle " + vehicle.id() + " has not arrived at the final node.");
            }
        }

        deliveredOrders = environment.orders.size() - orderMap.size();

        double timePointsProportion = (totalTimePoints * 1.0 / totalPossibleTimePoints);
        double imaginaryFuelConsumedProportion = (imaginaryFuelConsumed * 1.0 / totalFuelCost);
        double imaginaryGLPConsumedProportion = (imaginaryGLPConsumed * 1.0 / totalGLPCost);
		double ordersNotDeliveredProportion = (orderMap.size() * 1.0 / totalOrders);
		double ordersNotDeliveredOnTimeProportion = (1 - deliveredOrdersOnTime * 1.0 / totalOrders);

		fitness = Solution.weightTimePoints * timePointsProportion -
			Solution.weightImaginaryFuelConsumed * imaginaryFuelConsumedProportion -
			Solution.weightImaginaryGLPConsumed * imaginaryGLPConsumedProportion -
			Solution.weightOrdersNotDelivered * ordersNotDeliveredProportion -
			Solution.weightOrdersDeliveredOnTime * ordersNotDeliveredOnTimeProportion;

        isFeasible = deliveredOrdersOnTime == totalOrders;

        ordersDeliveredBeforeDeadlinePercentage = deliveredOrdersOnTime * 1.0 / totalOrders;
        averageSpareTime = totalSpareTime * 1.0 / totalOrders;

        hasRunSimulation = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(hasRunSimulation ? "Solution{\n" : "Solution Proposal{\n");
        sb.append("  routes={\n");
        for (Map.Entry<Integer, List<Node>> entry : routes.entrySet()) {
            sb.append("    Vehicle ").append(entry.getKey()).append(": [\n");
            for (int i = 0; i < entry.getValue().size(); i++) {
                Node node = entry.getValue().get(i);
                sb.append("      ").append(node.getClass().getSimpleName())
                  .append("(id=").append(node.id)
                  .append(", position=").append(node.getPosition());
                
                if (node instanceof OrderDeliverNode) {
                    OrderDeliverNode orderNode = (OrderDeliverNode) node;
                    sb.append(", orderId=").append(orderNode.order.id())
                      .append(", amount=").append(orderNode.amountGLP);
                } else if (node instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) node;
                    sb.append(", warehouseId=").append(refillNode.warehouse.id())
                      .append(", amount=").append(refillNode.amountGLP);
                }
                
                sb.append(")");
                if (i < entry.getValue().size() - 1) {
                    sb.append(" ->");
                }
                sb.append("\n");
            }
            sb.append("    ]\n");
        }
        sb.append("  },\n");
        sb.append("  hasRunSimulation=").append(hasRunSimulation);
        if (hasRunSimulation) {
            sb.append(",\n  isFeasible=").append(isFeasible)
              .append(",\n  fitness=").append(String.format("%.4f", fitness))
              .append(",\n  fitness_components={\n")
              .append("    timePoints=").append(totalTimePoints).append("/").append(totalPossibleTimePoints).append(" (").append(String.format("%.4f", totalTimePoints * 1.0 / totalPossibleTimePoints)).append("),\n")
              .append("    imaginaryFuelConsumed=").append(String.format("%.4f", imaginaryFuelConsumed)).append("/").append(String.format("%.4f", totalFuelCost)).append(" (").append(String.format("%.4f", imaginaryFuelConsumed * 1.0 / totalFuelCost)).append("),\n")
              .append("    imaginaryGLPConsumed=").append(imaginaryGLPConsumed).append("/").append(totalGLPCost).append(" (").append(String.format("%.4f", imaginaryGLPConsumed * 1.0 / totalGLPCost)).append("),\n")
              .append("    ordersNotDelivered=").append(totalOrders - deliveredOrders).append("/").append(totalOrders).append(" (").append(String.format("%.4f", (totalOrders - deliveredOrders) * 1.0 / totalOrders)).append("),\n")
              .append("    ordersNotDeliveredOnTime=").append(totalOrders - deliveredOrdersOnTime).append("/").append(totalOrders).append(" (").append(String.format("%.4f", (totalOrders - deliveredOrdersOnTime) * 1.0 / totalOrders)).append("),\n")
              .append("  },\n")
              .append("  metrics={\n")
              .append("    ordersDeliveredBeforeDeadlinePercentage=").append(String.format("%.4f", ordersDeliveredBeforeDeadlinePercentage)).append(",\n")
              .append("    averageSpareTime=").append(String.format("%.4f", averageSpareTime)).append(",\n")
              .append("  },\n")
              .append("  errors=[\n");
            for (String error : errors) {
                sb.append("    ").append(error).append(",\n");
            }
            if (!errors.isEmpty()) {
                sb.setLength(sb.length() - 2); // Remove the last comma and newline
            }
            sb.append("\n  ]\n}");
        }
        return sb.toString();
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
            totalTimePoints * 1.0 / totalPossibleTimePoints));
        report.append(String.format("  Imaginary Fuel Consumed: %.4f/%.4f (%.4f)\n", 
            imaginaryFuelConsumed, totalFuelCost, 
            imaginaryFuelConsumed * 1.0 / totalFuelCost));
        report.append(String.format("  Imaginary GLP Consumed: %d/%d (%.4f)\n", 
            imaginaryGLPConsumed, totalGLPCost, 
            imaginaryGLPConsumed * 1.0 / totalGLPCost));
        report.append(String.format("  Orders Not Delivered: %d/%d (%.4f)\n", 
            totalOrders - deliveredOrders, totalOrders, 
            (totalOrders - deliveredOrders) * 1.0 / totalOrders));
        report.append(String.format("  Orders Not Delivered On Time: %d/%d (%.4f)\n", 
            totalOrders - deliveredOrdersOnTime, totalOrders, 
            (totalOrders - deliveredOrdersOnTime) * 1.0 / totalOrders));

        report.append("\nMetrics:\n");
        report.append(String.format("  Orders Delivered Before Deadline: %.4f%%\n", 
            ordersDeliveredBeforeDeadlinePercentage * 100));
        report.append(String.format("  Average Spare Time: %.4f minutes\n", 
            averageSpareTime));

        if (!errors.isEmpty()) {
            report.append("\nErrors:\n");
            report.append(errors.stream().collect(Collectors.joining("\n")));
        } else {
            report.append("\nNo errors found.");
        }

        return report.toString();
    }
}
