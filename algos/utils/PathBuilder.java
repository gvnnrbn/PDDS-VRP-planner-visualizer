package utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import domain.Blockage;
import domain.Position;

public class PathBuilder {
    private final List<Blockage> blockages;
    private final int gridLength;
    private final int gridWidth;

    public PathBuilder(List<Blockage> blockages, int gridLength, int gridWidth) {
        this.blockages = blockages;
        this.gridLength = gridLength;
        this.gridWidth = gridWidth;
    }

    public static List<Position> buildPath(Position from, Position to, List<Blockage> blockages, 
                                         int gridLength, int gridWidth) {
        PathBuilder builder = new PathBuilder(blockages, gridLength, gridWidth);

        List<Position> path = new ArrayList<>();

        Position fromNotInteger = null;
        Position toNotInteger = null;

        if (from.x() != Math.floor(from.x()) || from.y() != Math.floor(from.y())) {
            fromNotInteger = from;
            from = new Position(Math.floor(from.x()), Math.floor(from.y()));
        }

        if (to.x() != Math.floor(to.x()) || to.y() != Math.floor(to.y())) {
            toNotInteger = to;
            to = new Position(Math.floor(to.x()), Math.floor(to.y()));
        }

        path = builder.findPathInteger(from, to);

        if (fromNotInteger != null) {
            path.addFirst(fromNotInteger);
        }

        if (toNotInteger != null) {
            path.addLast(toNotInteger);
        }

        return path;
    }

    private List<Position> findPathInteger(Position fromInteger, Position toInteger) {
        if (fromInteger.equals(toInteger)) {
            return new ArrayList<>();
        }

        // If Manhattan path is available, use it directly
        if (isManhattanAvailable(fromInteger, toInteger)) {
            return buildManhattanPath(fromInteger, toInteger);
        }
        
        // Otherwise, find A* path
        return findAStarPath(fromInteger, toInteger);
    }

    private boolean isManhattanAvailable(Position from, Position to) {
        // Check if either L-shaped path is available
        Position intermediate1 = new Position(from.x(), to.y());
        Position intermediate2 = new Position(to.x(), from.y());
        
        boolean path1Clear = !isRouteBlocked(from, intermediate1) && 
                            !isRouteBlocked(intermediate1, to);
                            
        boolean path2Clear = !isRouteBlocked(from, intermediate2) && 
                            !isRouteBlocked(intermediate2, to);
                            
        return path1Clear || path2Clear;
    }

    private List<Position> buildManhattanPath(Position from, Position to) {
        List<Position> path = new ArrayList<>();
        path.add(from);
        
        // First move horizontally, then vertically
        Position horizontal = new Position(to.x(), from.y());
        if (!from.equals(horizontal) && !isRouteBlocked(from, horizontal)) {
            if (!horizontal.equals(to)) {
                path.add(horizontal);
            }
            path.add(to);
            return path;
        }
        
        // If first path not available, try vertical then horizontal
        Position vertical = new Position(from.x(), to.y());
        path.add(vertical);
        path.add(to);
        return path;
    }

    private List<Position> findAStarPath(Position from, Position to) {
        // Priority queue for open nodes, sorted by f-score (g + h)
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        // Set to track visited nodes
        Set<Position> closedSet = new HashSet<>();
        // Map to store g-scores (cost from start to current)
        Map<Position, Double> gScore = new HashMap<>();
        // Map to store parent nodes for path reconstruction
        Map<Position, Position> cameFrom = new HashMap<>();

        // Initialize g-score for start position
        gScore.put(from, 0.0);
        openSet.add(new AStarNode(from, 0.0, calculateManhattanDistance(from, to)));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.position.equals(to)) {
                return reconstructPath(cameFrom, current.position);
            }

            closedSet.add(current.position);

            for (Position neighbor : getNeighbors(current.position)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                // Calculate tentative g-score (distance between current and neighbor is always 1)
                double tentativeGScore = gScore.get(current.position) + 1.0;

                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current.position);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + calculateManhattanDistance(neighbor, to);
                    openSet.add(new AStarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }

        // If we get here, no path was found
        return new ArrayList<>(); // Return empty path if no path found
    }

    private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
        List<Position> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current); // Add to beginning to maintain order
        }
        
        return path;
    }

    private List<Position> getNeighbors(Position current) {
        List<Position> neighbors = new ArrayList<>();
        double[][] directions = { { 0.0, 1.0 }, { 1.0, 0.0 }, { 0.0, -1.0 }, { -1.0, 0.0 } }; // up, right, down, left

        for (double[] dir : directions) {
            double newX = current.x() + dir[0];
            double newY = current.y() + dir[1];

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

    private boolean isRouteBlocked(Position a, Position b) {
        for (Blockage blockage : blockages) {
            if (blockage.blocksRoute(a, b)) {
                return true;
            }
        }
        return false;
    }

    private static double calculateManhattanDistance(Position from, Position to) {
        return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y());
    }

    // Helper class for A* algorithm
    private static class AStarNode {
        final Position position;
        final double f; // g + heuristic

        AStarNode(Position position, double g, double f) {
            this.position = position;
            this.f = f;
        }
    }
}