package domain;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;

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
    private Map<Position, Map<Position, Integer>> distances;
    private boolean areDistancesGenerated = false;

    public List<Node> getNodes() {
        if (!areNodesGenerated) {
            generateNodes();
        }
        return nodes;
    }

    public Map<Position, Map<Position, Integer>> getDistances() {
        if (!areDistancesGenerated) {
            generateDistances();
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
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Time(0, 0, 0, 0));
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
    public static double calculateFuelCost(Node from, Node to, Map<Position, Map<Position, Integer>> distances,
            Vehicle vehicle) {
        int distance = distances.get(from.getPosition()).get(to.getPosition());
        double fuelCost = distance * (vehicle.weight() + vehicle.currentGLP() * 0.5) / 180;
        return fuelCost;
    }

    public void generateDistances() {
        Map<Position, Map<Position, Integer>> distances = new HashMap<>();
        List<Position> positions = new ArrayList<>();
        Set<Position> uniquePositions = new HashSet<>();
        for (Node node : getNodes()) {
            uniquePositions.add(node.getPosition());
        }
        positions.addAll(uniquePositions);

        // Initialize the distance map for all positions
        for (Position position : positions) {
            distances.put(position, new HashMap<>());
            // Set diagonal to 0
            distances.get(position).put(position, 0);
        }

        // Only calculate distances for the upper triangle
        for (int i = 0; i < positions.size(); i++) {
            Position position = positions.get(i);
            for (int j = i + 1; j < positions.size(); j++) {
                Position otherPosition = positions.get(j);
                int distance;
                if (isManhattanAvailable(position, otherPosition)) {
                    distance = calculateManhattanDistance(position, otherPosition);
                } else {
                    distance = calculateAStarDistance(position, otherPosition);
                }
                // Set distance in both directions
                distances.get(position).put(otherPosition, distance);
                distances.get(otherPosition).put(position, distance);
            }
        }

        this.distances = distances;
        areDistancesGenerated = true;
    }

    private boolean isManhattanAvailable(Position from, Position to) {
        // Create intermediate points for the L-shaped Manhattan path
        Position intermediate1 = new Position(from.x(), to.y());
        Position intermediate2 = new Position(to.x(), from.y());

        // Check both possible L-shaped paths
        boolean path1Available = !isRouteBlocked(from, intermediate1) && !isRouteBlocked(intermediate1, to);
        boolean path2Available = !isRouteBlocked(from, intermediate2) && !isRouteBlocked(intermediate2, to);

        return path1Available || path2Available;
    }

    private boolean isRouteBlocked(Position a, Position b) {
        for (Blockage blockage : blockages) {
            if (blockage.blocksRoute(a, b)) {
                return true;
            }
        }
        return false;
    }

    public static int calculateManhattanDistance(Position from, Position to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y());
    }

    private int calculateAStarDistance(Position from, Position to) {
        // Priority queue for open nodes, sorted by f-score (g + h)
        PriorityQueue<AstarNode> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        // Set to track visited nodes
        Set<Position> closedSet = new HashSet<>();
        // Map to store g-scores (cost from start to current)
        Map<Position, Integer> gScore = new HashMap<>();
        // Map to store parent nodes for path reconstruction
        Map<Position, Position> cameFrom = new HashMap<>();

        // Initialize g-score for start position
        gScore.put(from, 0);
        openSet.add(new AstarNode(from, 0, calculateManhattanDistance(from, to)));

        while (!openSet.isEmpty()) {
            AstarNode current = openSet.poll();

            if (current.position.equals(to)) {
                return gScore.get(to);
            }

            closedSet.add(current.position);

            // Generate neighbors (up, down, left, right)
            for (Position neighbor : getAstarNeighbors(current.position)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                // Calculate tentative g-score
                int tentativeGScore = gScore.get(current.position) + 1;

                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current.position);
                    gScore.put(neighbor, tentativeGScore);
                    int fScore = tentativeGScore + calculateManhattanDistance(neighbor, to);
                    openSet.add(new AstarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }

        // If we get here, no path was found
        return Integer.MAX_VALUE;
    }

    private List<Position> getAstarNeighbors(Position current) {
        List<Position> neighbors = new ArrayList<>();
        int[][] directions = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } }; // up, right, down, left

        for (int[] dir : directions) {
            int newX = current.x() + dir[0];
            int newY = current.y() + dir[1];

            // Check if the new position is within grid boundaries
            if (newX >= 0 && newX < gridLength && newY >= 0 && newY < gridWidth) {
                Position neighbor = new Position(newX, newY);
                if (!isRouteBlocked(current, neighbor)) {
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }

    // Helper class for A* algorithm
    private static class AstarNode {
        Position position;
        int f; // g + heuristic

        AstarNode(Position position, int g, int f) {
            this.position = position;
            this.f = f;
        }
    }

    public void reportGeneratedNodes() {
        if (!areNodesGenerated) {
            System.out.println("Nodes have not been generated yet.");
            return;
        }

        System.out.println("\n=== Node Generation Report ===");
        System.out.println("Total nodes generated: " + nodes.size());

        int emptyNodes = 0;
        int orderNodes = 0;
        int refillNodes = 0;
        int finalNodes = 0;

        for (Node node : nodes) {
            if (node instanceof EmptyNode)
                emptyNodes++;
            else if (node instanceof OrderDeliverNode)
                orderNodes++;
            else if (node instanceof ProductRefillNode)
                refillNodes++;
            else if (node instanceof FinalNode)
                finalNodes++;
        }

        System.out.println("Node types breakdown:");
        System.out.println("- Empty nodes (vehicle start positions): " + emptyNodes);
        System.out.println("- Order delivery nodes: " + orderNodes);
        System.out.println("- Product refill nodes: " + refillNodes);
        System.out.println("- Final nodes: " + finalNodes);

        // Print total GLP to be delivered
        int totalGLPToDeliver = 0;
        for (Node node : nodes) {
            if (node instanceof OrderDeliverNode) {
                totalGLPToDeliver += ((OrderDeliverNode) node).amountGLP;
            }
        }
        System.out.println("\nTotal GLP to be delivered: " + totalGLPToDeliver + " m³");

        // Print total GLP to be refilled
        int totalGLPToRefill = 0;
        for (Node node : nodes) {
            if (node instanceof ProductRefillNode) {
                totalGLPToRefill += ((ProductRefillNode) node).amountGLP;
            }
        }
        System.out.println("Total GLP to be refilled: " + totalGLPToRefill + " m³");
    }
}
