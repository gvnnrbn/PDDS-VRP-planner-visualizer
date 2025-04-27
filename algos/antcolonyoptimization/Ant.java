package antcolonyoptimization;

import domain.Environment;
import domain.FinalNode;
import domain.EmptyNode;
import domain.Node;
import domain.Order;
import domain.OrderDeliverNode;
import domain.ProductRefillNode;
import domain.Solution;
import domain.Time;
import domain.Vehicle;
import domain.Warehouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Ant {
    private Solution solution;
    private List<Node> nodes;

    public Ant(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void constructSolution(Map<Integer, Map<Integer, Double>> pheromones, double alpha, double beta, Environment environment) {
        solution = new Solution();
        
        // Copy pool of nodes for this solution
        List<Node> nodePool = new ArrayList<>(environment.getNodes());
        
        // Copy vehicules and set initial positions for their routes
        List<Vehicle> vehicles = new ArrayList<>(environment.vehicles);
        for(Vehicle vehicle : vehicles) {
            solution.routes.put(vehicle.id(), new ArrayList<>());

            EmptyNode startNode = (EmptyNode) nodePool.stream()
                .filter(node -> node instanceof EmptyNode)
                .filter(node -> ((EmptyNode) node).getPosition().equals(vehicle.initialPosition()))
                .findFirst()
                .get();

            solution.routes.get(vehicle.id()).add(startNode);
            nodePool.remove(startNode);
        }

        /* 1. testing adding VALID nodes to the same route until final node is added (27/04) */
        List<Vehicle> vehiclesToProcess = new ArrayList<>(vehicles);
        
        for(Vehicle vehicle : vehiclesToProcess) {
            List<Node> route = solution.routes.get(vehicle.id());
            List<Node> discartedNodes = new ArrayList<>();
            while(!nodePool.isEmpty()){
                Node currNode = route.getLast();
                Node nextNode = getNextNode(pheromones, currNode, alpha);
                route.add(nextNode);
                if (isFeasibleSolution(route, vehicle, environment, discartedNodes)) {
                    nodePool.remove(nextNode); 
                    if(nextNode instanceof FinalNode){ // go to next vehicle if nextNode was a final node
                        break;
                    }
                }
                else{
                    route.remove(nextNode);
                    discartedNodes.add(nextNode);
                    continue; // Try different node
                }
            }
        }
    }

    public Solution getSolution() {
        return solution;
    }

    private Node getNextNode(Map<Integer, Map<Integer, Double>> pheromones, Node currNode, double alpha) {
        if (nodes.isEmpty()) {
            return null;
        }

        double[] probabilities = new double[nodes.size()];
        double sum = 0;

        for(int i = 0; i < nodes.size(); i++) {
            Node nextNode = nodes.get(i);
            
            double pheromone = pheromones.get(currNode.id).get(nextNode.id);
            probabilities[i] = Math.pow(pheromone, alpha);
            sum += probabilities[i];
        }

        if (sum == 0) {
            return nodes.get(0);  // Fallback if all probabilities are 0
        }

        for(int i = 0; i < nodes.size(); i++) {
            probabilities[i] /= sum;
        }

        double random = Math.random() * sum;
        double cumulative = 0;

        for(int i = 0; i < nodes.size(); i++) {
            cumulative += probabilities[i];
            if(random <= cumulative) {
                return nodes.get(i);
            }
        }

        return nodes.get(nodes.size() - 1);
    }
    
    // similar to simulate() in Solution class: validates if current route is feasible
    private boolean isFeasibleSolution(List<Node>route, Vehicle vehicle, Environment environment, List<Node> discartedNodes) {
        // Check if the next node is in the discarded nodes list
        if (discartedNodes.contains(route.getLast())) {
            return false;
        }

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
            int distance = environment.getDistances().get(originNode.getPosition()).get(destinationNode.getPosition());
            int timeSpent = (int) Math.ceil((double) distance / Environment.speed) * 60; // Convert hours to minutes
            currentTime = currentTime.addMinutes(timeSpent);

            // Calculate and check fuel cost
            double fuelCost = Environment.calculateFuelCost(originNode, destinationNode, environment.getDistances(), vehicle);
            if (vehicle.currentFuel() < fuelCost) {
                return false;
            }

            // When the vehicle arrives at an OrderDeliverNode
            if (destinationNode instanceof OrderDeliverNode) {
                OrderDeliverNode deliverNode = (OrderDeliverNode) destinationNode;
                Order order = deliverNode.order;

                int GLPToDeliver = deliverNode.amountGLP;

                // Check if the order can be delivered at the current time
                if (currentTime.isAfter(order.deadline())) {
                    return false;
                }

                // Check if the vehicle has enough GLP to deliver the order
                if (vehicle.currentGLP() < GLPToDeliver) {
                    return false;
                }

                // Deliver the order amount
                if (orderMap.get(order.id()).amountGLP() == GLPToDeliver) {
                    orderMap.remove(order.id());
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
                    return false;
                }

                // Update the warehouse state
                warehouse = new Warehouse(warehouse.id(), warehouse.position(), warehouse.currentGLP() - refillNode.amountGLP, warehouse.maxGLP(), warehouse.isMain());
                warehouseMap.put(warehouse.id(), warehouse);

                // Refill the vehicle with GLP and full fuel
                vehicle = new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), vehicle.maxFuel(), vehicle.maxGLP(), vehicle.currentGLP() + refillNode.amountGLP, vehicle.initialPosition());
            }
        }

        return true;
    }
}