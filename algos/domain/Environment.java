package domain;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Environment {
    public static final int chunkSize = 5; // Max number of m3 of GLP that can be transported or refilled in one chunk
    public static final int speed = 10; // km/h
    public static final int timeAfterDelivery = 15; // minutes

    public static final int minutesLeftMultiplier = 1; // multiplier for the fitness function

    public Time currentTime;

    public List<Vehicle> vehicles;
    public List<Order> orders;
    public List<Warehouse> warehouses;
    public List<Blockage> blockages;

    public Environment(List<Vehicle> vehicles, List<Order> orders, List<Warehouse> warehouses, Time currentTime) {
        this.vehicles = vehicles;
        this.orders = orders;
        this.warehouses = warehouses;
        this.currentTime = currentTime;
    }

    public Environment() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Time(0, 0, 0,0));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Environment{\n");
        for (Vehicle vehicle : vehicles) {
            sb.append("  ").append(vehicle).append("\n");
        }
        for (Order order : orders) {
            sb.append("  ").append(order).append("\n");
        }
        for (Warehouse warehouse : warehouses) {
            sb.append("  ").append(warehouse).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
    

    public List<Node> generateNodes() {
        List<Node> nodes = new ArrayList<>();
        int nodeSerial = 0;

        for (Vehicle vehicle : vehicles) {
            nodes.add(new EmptyNode(nodeSerial++, vehicle.initialPosition()));
        }

        for (Order order : orders) {
            int remainingGLP = order.amountGLP();
            while (remainingGLP > 0) {
                if (remainingGLP > chunkSize) {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, chunkSize));
                    remainingGLP -= chunkSize;
                } else {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, remainingGLP));
                    remainingGLP = 0;
                }
            }
        }

        for (Warehouse warehouse : warehouses) {
            int remainingGLP = warehouse.currentGLP();
            while (remainingGLP > 0) {
                if (remainingGLP > chunkSize) {
                    nodes.add(new ProductRefillNode(nodeSerial++, warehouse, chunkSize));
                    remainingGLP -= chunkSize;
                } else {
                    nodes.add(new ProductRefillNode(nodeSerial++, warehouse, remainingGLP));
                    remainingGLP = 0;
                }
            }
        }

        for (Warehouse warehouse : warehouses) {
            nodes.add(new FuelRefillNode(nodeSerial++, warehouse));
        }

        return nodes;
    }

    public Map<Integer, Map<Integer, Integer>> generateDistances(List<Node> nodes) {
        // Calculate for each node pair the distances, 0 for same position, manhattan for no blockages and A* for blockages
        throw new UnsupportedOperationException("Not implemented");
    }

    public static double calculateFuelCost(Node from, Node to, Map<Integer, Map<Integer, Integer>> distances, Vehicle vehicle) {
        int distance = distances.get(from.id).get(to.id);
        double fuelCost = distance * (vehicle.weight() + vehicle.currentGLP() * 0.5) / 180;
        return fuelCost;
    }
}
