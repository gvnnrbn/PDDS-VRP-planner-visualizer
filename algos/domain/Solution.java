package domain;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class Solution {
    public Map<Integer, List<Node>> routes; // routes[vehicleId] -> nodes

    private boolean hasRunSimulation = false;
    private boolean isFeasible = true; // Depends on the simulation
    private int fitness = 0;

    public Solution() {
        routes = new HashMap<>();
    }

    public int fitness(Environment environment, Map<Integer, Map<Integer, Integer>> distances) {
        if (!hasRunSimulation) {
            simulate(environment, distances);
        }

        return fitness;
    }

    public boolean isFeasible(Environment environment, Map<Integer, Map<Integer, Integer>> distances) {
        if (!hasRunSimulation) {
            simulate(environment, distances);
        }
        return isFeasible;
    }

    private void simulate(Environment environment, Map<Integer, Map<Integer, Integer>> distances) {
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
                int distance = distances.get(originNode.id).get(destinationNode.id);
                int timeSpent = (int) Math.ceil((double) distance / Environment.speed) * 60; // Convert hours to minutes
                currentTime = currentTime.addMinutes(timeSpent);

                // Calculate and check fuel cost
                double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, distances, vehicle);
                if (vehicle.currentFuel() < fuelCost) {
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
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Check if the vehicle has enough GLP to deliver the order
                    if (vehicle.currentGLP() < GLPToDeliver) {
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Deliver the order amount
                    if (order.amountGLP() == GLPToDeliver) {
                        orderMap.remove(order.id());
                        int minutesLeft = currentTime.minutesUntil(order.deadline());
                        fitness += minutesLeft * Environment.minutesLeftMultiplier;
                    } else {
                        orderMap.put(order.id(), new Order(order.id(), order.amountGLP() - GLPToDeliver, order.position(), order.deadline()));
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
                        isFeasible = false;
                        hasRunSimulation = true;
                        return;
                    }

                    // Update the warehouse state
                    warehouse = new Warehouse(warehouse.id(), warehouse.position(), warehouse.currentGLP() - refillNode.amountGLP, warehouse.maxGLP());
                    warehouseMap.put(warehouse.id(), warehouse);

                    // Refill the vehicle
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.currentFuel() - fuelCost, vehicle.maxGLP(), vehicle.currentGLP() + refillNode.amountGLP, vehicle.initialPosition());
                }

                // When the vehicle arrives at a FuelRefillNode
                if (destinationNode instanceof FuelRefillNode) {
                    // Refuel the vehicle
                    vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.maxFuel(), vehicle.maxGLP(), vehicle.currentGLP(), vehicle.initialPosition());
                }
            }
        }

        hasRunSimulation = true;
    }
}