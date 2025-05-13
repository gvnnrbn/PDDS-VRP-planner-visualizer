package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import domain.Blockage;
import domain.Position;

public class PathBuilder {
    public static List<Position> buildPath(Position from, Position to, List<Blockage> blockages, 
                                         int gridLength, int gridWidth) {
        List<Position> path = new ArrayList<>();

        if (from.x() == to.x() && from.y() == to.y()) {
            return path;
        }

        Position adjustedFrom = new Position(Math.floor(from.x()), Math.floor(from.y()));
        Position adjustedTo = new Position(Math.ceil(to.x()), Math.ceil(to.y()));

        if (isManhattanAvailable(adjustedFrom, adjustedTo, gridLength, gridWidth)) {
            path = buildManhattanPath(adjustedFrom, adjustedTo, blockages, gridLength, gridWidth);
        } else {
            path = buildAstarPath(adjustedFrom, adjustedTo, blockages, gridLength, gridWidth);
        }

        if (from.x() != adjustedFrom.x() || from.y() != adjustedFrom.y()) {
            path.addFirst(from);
        }

        if (to.x() != adjustedTo.x() || to.y() != adjustedTo.y()) {
            path.addLast(to);
        }

        return path;
    }

    public static double calculateDistance(List<Position> path) {
        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            distance += Math.abs(path.get(i).x() - path.get(i + 1).x()) + Math.abs(path.get(i).y() - path.get(i + 1).y());
        }
        return distance;
    }

    public static Map<Position, Map<Position, Double>> generateDistances(List<Position> positions, List<Blockage> blockages, int gridLength, int gridWidth) {
        Map<Position, Map<Position, Double>> distances = new HashMap<>();

        Set<Position> uniquePositions = new HashSet<>();
        for (Position position : positions) {
            uniquePositions.add(position);
        }
        positions.addAll(uniquePositions);

        for (Position position : positions) {
            distances.put(position, new HashMap<>());
            distances.get(position).put(position, 0.0);
        }

        for (int i = 0; i < positions.size(); i++) {
            Position position = positions.get(i);
            for (int j = i + 1; j < positions.size(); j++) {
                Position otherPosition = positions.get(j);

                List<Position> path = buildPath(position, otherPosition, blockages, gridLength, gridWidth);
                double distance = calculateDistance(path);

                // Set distance in both directions
                distances.get(position).put(otherPosition, distance);
                distances.get(otherPosition).put(position, distance);
            }
        }

        return distances;
    }

    private static double calculateManhattanDistance(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private static boolean isManhattanAvailable(Position from, Position to, int gridLength, int gridWidth) {
        double distance = calculateManhattanDistance(from, to);
        return distance <= gridLength && distance <= gridWidth;
    }

    private static boolean isPathBlocked(Position from, Position to, List<Blockage> blockages) {
        if (blockages == null || blockages.isEmpty()) {
            return false;
        }
        for (Blockage blockage : blockages) {
            if (blockage != null && blockage.blocksRoute(from, to)) {
                return true;
            }
        }
        return false;
    }

    private static List<Position> buildManhattanPath(Position from, Position to, List<Blockage> blockages, int gridLength, int gridWidth) {
        List<Position> path = new ArrayList<>();
        path.add(from);
        
        // First try: Move horizontally then vertically
        Position horizontalThenVertical = new Position(to.x(), from.y());
        if (!isPathBlocked(from, horizontalThenVertical, blockages) && 
            !isPathBlocked(horizontalThenVertical, to, blockages)) {
            if (!from.equals(horizontalThenVertical)) {
                path.add(horizontalThenVertical);
            }
            path.add(to);
            return path;
        }
        
        // Second try: Move vertically then horizontally
        Position verticalThenHorizontal = new Position(from.x(), to.y());
        if (!isPathBlocked(from, verticalThenHorizontal, blockages) && 
            !isPathBlocked(verticalThenHorizontal, to, blockages)) {
            if (!from.equals(verticalThenHorizontal)) {
                path.add(verticalThenHorizontal);
            }
            path.add(to);
            return path;
        }

        return new ArrayList<>();
    }

    private static class AstarNode implements Comparable<AstarNode> {
        final Position position;
        final double f; // f = g + h (total cost)
        @SuppressWarnings("unused") // g is used in the compareTo method
        final double g; // cost from start to current node

        AstarNode(Position position, double g, double h) {
            this.position = position;
            this.g = g;
            this.f = g + h;
        }

        @Override
        public int compareTo(AstarNode other) {
            return Double.compare(this.f, other.f);
        }
    }

    private static List<Position> getAstarNeighbors(Position pos, int gridLength, int gridWidth) {
        List<Position> neighbors = new ArrayList<>();
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}}; // up, right, down, left
        
        for (int[] dir : directions) {
            int newX = (int)pos.x() + dir[0];
            int newY = (int)pos.y() + dir[1];
            
            // Check if the new position is within grid boundaries
            if (newX >= 0 && newX < gridLength && newY >= 0 && newY < gridWidth) {
                neighbors.add(new Position(newX, newY));
            }
        }
        
        return neighbors;
    }

    private static List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
        List<Position> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current); // Add to beginning to maintain order
        }
        
        return path;
    }

    private static List<Position> buildAstarPath(Position from, Position to, List<Blockage> blockages, int gridLength, int gridWidth) {
        // Priority queue for open nodes, sorted by f-score (g + h)
        PriorityQueue<AstarNode> openSet = new PriorityQueue<>();
        // Set to track visited nodes
        Set<Position> closedSet = new HashSet<>();
        // Map to store g-scores (cost from start to current)
        Map<Position, Double> gScore = new HashMap<>();
        // Map to store parent nodes for path reconstruction
        Map<Position, Position> cameFrom = new HashMap<>();

        // Initialize g-score for start position
        gScore.put(from, 0.0);
        openSet.add(new AstarNode(from, 0.0, calculateManhattanDistance(from, to)));

        while (!openSet.isEmpty()) {
            AstarNode current = openSet.poll();

            if (current.position.equals(to)) {
                return reconstructPath(cameFrom, to);
            }

            closedSet.add(current.position);

            // Generate neighbors (up, down, left, right)
            for (Position neighbor : getAstarNeighbors(current.position, gridLength, gridWidth)) {
                if (closedSet.contains(neighbor) || isPathBlocked(current.position, neighbor, blockages)) {
                    continue;
                }

                // Calculate tentative g-score (each move costs 1)
                double tentativeGScore = gScore.get(current.position) + 1.0;

                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current.position);
                    gScore.put(neighbor, tentativeGScore);
                    double hScore = calculateManhattanDistance(neighbor, to);
                    openSet.add(new AstarNode(neighbor, tentativeGScore, hScore));
                }
            }
        }

        return new ArrayList<>();
    }
}