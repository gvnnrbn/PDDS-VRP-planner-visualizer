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
    public static int chunkSize = 5; // Max number of m3 of GLP that can be transported or refilled in one chunk
    public static int speed = 10; // km/h
    public static int timeAfterDelivery = 15; // minutes

    public static int minutesLeftMultiplier = 1; // multiplier for the fitness function

    public static int gridLength = 30;
    public static int gridWidth = 30;

    public Time currentTime;

    public List<Vehicle> vehicles;
    public List<Order> orders;
    public List<Warehouse> warehouses;
    public List<Blockage> blockages;

    private List<Node> nodes;
    private boolean areNodesGenerated = false;
    private Map<Integer, Map<Integer, Integer>> distances;
    private boolean areDistancesGenerated = false;

    public List<Node> getNodes() {
        if (!areNodesGenerated) {
            generateNodes();
        }
        return nodes;
    }

    public Map<Integer, Map<Integer, Integer>> getDistances() {
        if (!areDistancesGenerated) {
            generateDistances();
        }
        return distances;
    }

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
    

    public void generateNodes() {
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

            if (warehouse.isMain()) {
                // Generate a single infinite ProductRefillNode for the main warehouse
                nodes.add(new ProductRefillNode(nodeSerial++, warehouse, chunkSize));
                continue;
            }

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

        // Add an empty nodes for the main warehouse as final nodes
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

    public static double calculateFuelCost(Node from, Node to, Map<Integer, Map<Integer, Integer>> distances, Vehicle vehicle) {
        int distance = distances.get(from.id).get(to.id);
        double fuelCost = distance * (vehicle.weight() + vehicle.currentGLP() * 0.5) / 180;
        return fuelCost;
    }

    public void generateDistances() {
        Map<Integer, Map<Integer, Integer>> distances = new HashMap<>();

        for (Node node : nodes) {
            Map<Integer, Integer> distancesFromLocalNode = new HashMap<>();

            // Calculate distances from local node to all other nodes
            for (Node otherNode : nodes) {
                if (node.getPosition().equals(otherNode.getPosition())) {
                    // Same position
                    distancesFromLocalNode.put(otherNode.id, 0);
                } else if (isManhattanAvailable(node.getPosition(), otherNode.getPosition())) {
                    // Manhattan distance
                    distancesFromLocalNode.put(otherNode.id, calculateManhattanDistance(node.getPosition(), otherNode.getPosition()));
                } else {
                    // Use A* to find the shortest path
                    distancesFromLocalNode.put(otherNode.id, calculateAStarDistance(node.getPosition(), otherNode.getPosition()));
                } 
            }

            distances.put(node.id, distancesFromLocalNode);
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

    private int calculateManhattanDistance(Position from, Position to) {
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
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}}; // up, right, down, left

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
}

