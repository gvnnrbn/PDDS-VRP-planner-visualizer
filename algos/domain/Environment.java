package domain;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import utils.PathBuilder;
import utils.Time;

public class Environment {
    public static int deliverChunkSize = 10; // Max number of m3 of GLP that can be transported or refilled in one chunk
    public static int refillChunkSize = 5; // Max number of m3 of GLP that can be refilled in one chunk
    public static int speed = 50; // km/h
    public static int timeAfterDelivery = 15; // minutes
    public static int timeAfterRefill = 10; // minutes

    public static int minutesLeftMultiplier = 1; // multiplier for the fitness function
    public static int lateDeliveryPenalty = 1; // penalty for the fitness function

    public static int maxFitnessForCompletedOrders = 10000;
    public static int maxFitnessForMaximumTimePoints = 20000;

    // 1 grid unit = 1 km
    public static int gridLength = 70;
    public static int gridWidth = 50;

    public static final int maxFitness = 10_000;
    public static final int constraintViolationPenalty = 100;

    public Time currentTime;

    public List<Vehicle> vehicles;
    public List<Order> orders;
    public List<Warehouse> warehouses;
    public List<Blockage> blockages;

    private List<Node> nodes;
    private boolean areNodesGenerated = false;
    private Map<Position, Map<Position, Double>> distances;
    private boolean areDistancesGenerated = false;

    public List<Node> getNodes() {
        if (!areNodesGenerated) {
            generateNodes();
        }
        return nodes;
    }

    public Map<Position, Map<Position, Double>> getDistances() {
        if (!areDistancesGenerated) {
            distances = PathBuilder.generateDistances(getNodes().stream().map(Node::getPosition).collect(Collectors.toList()), blockages, gridLength, gridWidth);
            areDistancesGenerated = true;
        }
        return distances;
    }

    public Environment(List<Vehicle> vehicles, List<Order> orders, List<Warehouse> warehouses, List<Blockage> blockages,
            Time currentTime) {
        this.vehicles = vehicles;
        this.orders = orders;
        this.warehouses = warehouses;
        this.blockages = blockages;
        this.currentTime = currentTime;
    }

    public Environment() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Time(0,0, 0, 0, 0));
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
        for (Blockage blockage : blockages) {
            sb.append("  ").append(blockage).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public void generateNodes() {
        List<Warehouse> warehousesCopy = new ArrayList<>(warehouses);

        List<Node> nodes = new ArrayList<>();
        int nodeSerial = 0;

        for (Vehicle vehicle : vehicles) {
            nodes.add(new EmptyNode(nodeSerial++, vehicle.initialPosition()));
        }

        for (Order order : orders) {
            int remainingGLP = order.amountGLP();
            while (remainingGLP > 0) {
                if (remainingGLP > deliverChunkSize) {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, deliverChunkSize));
                    remainingGLP -= deliverChunkSize;
                } else {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, remainingGLP));
                    remainingGLP = 0;
                }
            }
        }

        // Calculate the total amount of GLP that needs to be transported
        int totalGLP = 0;
        for (Order order : orders) {
            totalGLP += order.amountGLP();
        }

        // Calculate the total amount of GLP currently in the vehicles
        int totalGLPInVehicles = 0;
        for (Vehicle vehicle : vehicles) {
            totalGLPInVehicles += vehicle.currentGLP();
        }

        int totalGLPToRefill = totalGLP - totalGLPInVehicles;
        int totalAssignableGLP = (int) (totalGLPToRefill * 1.5);

        // Round robin to assign GLP from the warehouses
        int currentWarehouseIndex = 0;

        while (totalAssignableGLP > 0) {
            Warehouse currentWarehouse = warehousesCopy.get(currentWarehouseIndex);
            int warehouseGLP = currentWarehouse.currentGLP();

            if (warehouseGLP > 0) {
                int assignableGLP = Math.min(warehouseGLP, refillChunkSize);
                assignableGLP = Math.min(assignableGLP, totalAssignableGLP);

                // Create refill nodes in smaller chunks to allow for more frequent refueling
                int refillChunkSize = Math.min(assignableGLP, Environment.refillChunkSize);
                nodes.add(new ProductRefillNode(nodeSerial++, currentWarehouse, refillChunkSize));
                warehouseGLP -= refillChunkSize;
                totalAssignableGLP -= refillChunkSize;
            }

            currentWarehouseIndex = (currentWarehouseIndex + 1) % warehousesCopy.size();
        }

        // Add final nodes
        Warehouse mainWarehouse = null;
        for (Warehouse warehouse : warehouses) {
            if (warehouse.isMain()) {
                mainWarehouse = warehouse;
                break;
            }
        }
        if (mainWarehouse == null) {
            throw new RuntimeException("No main warehouse found");
        }
        for (int i = 0; i < vehicles.size(); i++) {
            nodes.add(new FinalNode(nodeSerial++, mainWarehouse.position()));
        }

        this.nodes = nodes;
        areNodesGenerated = true;
    }

    // Dist Max = 25 * 180 / 15 = 300 Km.
    // Fuel (in galons) = Distance (in km) * [weight (in kg) + 0.5 * GLP (in m3)] /
    // 180
    public static double calculateFuelCost(Node from, Node to, Map<Position, Map<Position, Double>> distances,
            Vehicle vehicle) {
        double distance = distances.get(from.getPosition()).get(to.getPosition());
        double fuelCost = distance * (vehicle.weight() + vehicle.currentGLP() * 0.5) / 180;
        return fuelCost;
    }
}
