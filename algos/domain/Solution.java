package domain;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// TODO: Normalize reward values with order size
public class Solution implements Cloneable {
    public Map<Integer, List<Node>> routes; // routes[vehicleId] -> nodes

    private boolean hasRunSimulation = false;
    private boolean isFeasible = true; // Depends on the simulation
    private int fitness = 0;
    
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

    public int fitness(Environment environment) {
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

    private void simulate(Environment environment) {
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

        for (Vehicle vehicle : environment.vehicles) {
            List<Node> route = routes.get(vehicle.id());
            Time currentTime = environment.currentTime;
            /*
            // Check if first node is their corresponding empty node
            Node firstNode = route.get(0);
            if (!(firstNode instanceof EmptyNode) || !(firstNode.getPosition().equals(vehicle.initialPosition()))) {
                fitness -= Environment.incorrectStartPositionPenalty;
                isFeasible = false;
                hasRunSimulation = true;
                return;
            }
            */

            for (int i = 0; i < route.size() - 1; i++) {
                Node originNode = route.get(i);
                Node destinationNode = route.get(i + 1);

                // If destination node breaks an order chain (changes order or goes from order to non-order node), wait the corresponding time
                boolean breaksOrderChain = (originNode instanceof OrderDeliverNode && !(destinationNode instanceof OrderDeliverNode)) ||
                 (originNode instanceof OrderDeliverNode && destinationNode instanceof OrderDeliverNode && ((OrderDeliverNode) originNode).order.id() != ((OrderDeliverNode) destinationNode).order.id());

                if (breaksOrderChain) {
                    currentTime = currentTime.addMinutes(Environment.timeAfterDelivery);
                }

                // Pass time after traveling from originNode to destinationNode
                int distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
                int timeSpent = (int) Math.ceil((double) distance / Environment.speed) * 60; // Convert hours to minutes
                currentTime = currentTime.addMinutes(timeSpent);

                // Calculate and check fuel cost
                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
                if (vehicle.currentFuel() < fuelCost) {
                    fitness -= Environment.insufficientFuelPenalty;
                    isFeasible = false;
                    hasRunSimulation = true;
                    return;
                }

                // When the vehicle arrives at an OrderDeliverNode
                if (destinationNode instanceof OrderDeliverNode) {
                    OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                    Order order = deliverNode.order;

                    int GLPToDeliver = deliverNode.amountGLP;

                    // Check if the order can be delivered at the current time
                    if (currentTime.isAfter(order.deadline())) {
                        int minutesLate = currentTime.minutesSince(order.deadline());
                        fitness -= minutesLate * Environment.lateDeliveryPenalty;
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Check if the vehicle has enough GLP to deliver the order
                    if (vehicle.currentGLP() < GLPToDeliver) {
                        fitness -= Environment.insufficientGLPPenalty;
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Deliver the order amount
                    if (orderMap.get(order.id()).amountGLP() == GLPToDeliver) {
                        orderMap.remove(order.id());
                        int minutesLeft = currentTime.minutesUntil(order.deadline());
                        fitness += minutesLeft * Environment.minutesLeftMultiplier;
                    } else {
                        Order updatedOrder = new Order(order.id(), orderMap.get(order.id()).amountGLP() - GLPToDeliver, order.position(), order.deadline());
                        orderMap.put(order.id(), updatedOrder);
                    }

                    // Update vehicle state
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.currentFuel() - fuelCost, vehicle.maxGLP(), vehicle.currentGLP() - GLPToDeliver, vehicle.initialPosition());
                }

                // When the vehicle arrives at a ProductRefillNode
                if (destinationNode instanceof ProductRefillNode) {
                    ProductRefillNode refillNode = (ProductRefillNode) destinationNode;
                    Warehouse warehouse = warehouseMap.get(refillNode.warehouse.id());

                    // Check if the warehouse has enough product to refill the vehicle
                    if (warehouse.currentGLP() < refillNode.amountGLP) {
                        fitness -= Environment.insufficientWarehouseGLPPenalty;
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Update the warehouse state
                    warehouse = new Warehouse(warehouse.id(), warehouse.position(), warehouse.currentGLP() - refillNode.amountGLP, warehouse.maxGLP(), warehouse.isMain());
                    warehouseMap.put(warehouse.id(), warehouse);

                    // Refill the vehicle with GLP and full fuel
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.maxFuel(), vehicle.maxGLP(), vehicle.currentGLP() + refillNode.amountGLP, vehicle.initialPosition());
                }
            }
        }

        // Check if all vehicles have arrived at the final node
        for (Vehicle vehicle : environment.vehicles) {
            List<Node> route = routes.get(vehicle.id());
            if (!(route.get(route.size() - 1) instanceof FinalNode)) {
                fitness -= Environment.missingFinalNodePenalty;
                isFeasible = false;
                hasRunSimulation = true;
                return;
            }
        }

        // Check if all orders are delivered
        if (orderMap.size() > 0) {
            fitness -= orderMap.size() * Environment.undeliveredOrderPenalty;
            isFeasible = false;
            hasRunSimulation = true;
            return;
        }

        hasRunSimulation = true;
        fitness += Environment.feasibilityBonus;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Solution{\n");
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
        sb.append("  hasRunSimulation=").append(hasRunSimulation).append(",\n");
        sb.append("  isFeasible=").append(isFeasible).append(",\n");
        sb.append("  fitness=").append(fitness).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
}
