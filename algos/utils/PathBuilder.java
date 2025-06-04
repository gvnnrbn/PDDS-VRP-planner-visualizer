package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import entities.PlannerBlockage;

public class PathBuilder {
    private static final double EPSILON = 1e-6;

    // Returns null if no path is found, empty list if from == to, otherwise returns the path
    public static List<Position> buildPath(Position from, Position to, List<PlannerBlockage> blockages) {
        // Check if either point is inside a blockage (not at endpoints)
        if (isInsideBlockage(from, blockages) || isInsideBlockage(to, blockages)) {
            return null;
        }

        if (from.equals(to)) {
            return new ArrayList<>();
        }

        List<Position> path = new ArrayList<>();
        Position fromPos = from;
        Position toPos = to;

        // Handle non-integer start position
        if (!from.isInteger()) {
            Position roundedFrom = from.round();
            // Check if direct path to rounded position is blocked
            if (isPathBlocked(from, roundedFrom, blockages)) {
                return null;
            }
            path.add(from);
            fromPos = roundedFrom;
        }

        // Handle non-integer end position
        if (!to.isInteger()) {
            Position roundedTo = to.round();
            // Check if direct path from rounded position is blocked
            if (isPathBlocked(roundedTo, to, blockages)) {
                return null;
            }
            toPos = roundedTo;
        }

        // Try Manhattan path first with integer positions
        List<Position> mainPath = buildManhattanPath(fromPos, toPos, blockages);
        if (mainPath != null) {
            // Verify the path doesn't cross any blockages
            if (isPathValid(mainPath, blockages)) {
                path.addAll(mainPath);
                // Add final non-integer position if needed
                if (!to.isInteger()) {
                    path.add(to);
                }
                return compressPath(path);
            }
        }

        // Try A* as a fallback with integer positions
        mainPath = buildAstarPath(fromPos, toPos, blockages);
        if (mainPath != null) {
            // Verify the path doesn't cross any blockages
            if (isPathValid(mainPath, blockages)) {
                path.addAll(mainPath);
                // Add final non-integer position if needed
                if (!to.isInteger()) {
                    path.add(to);
                }
                return compressPath(path);
            }
        }

        return null;
    }

    private static boolean isPathValid(List<Position> path, List<PlannerBlockage> blockages) {
        if (path == null || path.size() < 2) return true;
        
        // Check each segment of the path
        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            
            // Check if this segment is blocked
            if (isPathBlocked(current, next, blockages)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInsideBlockage(Position pos, List<PlannerBlockage> blockages) {
        for (PlannerBlockage blockage : blockages) {
            for (int i = 0; i < blockage.vertices.size() - 1; i++) {
                Position v1 = blockage.vertices.get(i);
                Position v2 = blockage.vertices.get(i + 1);
                
                // Check if the point lies on the blockage line (but not at endpoints)
                if (isPointOnLine(pos, v1, v2) && !isPointEqual(pos, v1) && !isPointEqual(pos, v2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPointEqual(Position p1, Position p2) {
        return Math.abs(p1.x - p2.x) < EPSILON && Math.abs(p1.y - p2.y) < EPSILON;
    }

    public static double calculateDistance(List<Position> path) {
        if (path == null) {
            return Double.POSITIVE_INFINITY;
        }

        if (path.isEmpty()) {
            return 0;
        }

        double distance = 0;
        // Assume each path fragment is colinear
        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            distance += Math.abs(current.x - next.x) + Math.abs(current.y - next.y);
        }
        return distance;
    }

    public static Map<Position, Map<Position, Double>> generateDistances(List<Position> positions, List<PlannerBlockage> blockages) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private static boolean isPathBlocked(Position from, Position to, List<PlannerBlockage> blockages) {
        if (from.equals(to)) {
            return false;
        }

        // First check if either point is on a vertex
        for (PlannerBlockage blockage : blockages) {
            List<Position> vertices = blockage.vertices;
            if (vertices.size() < 2) continue;
            
            boolean fromOnVertex = false;
            boolean toOnVertex = false;
            Position fromVertex = null;
            Position toVertex = null;
            
            for (Position vertex : vertices) {
                if (isPointEqual(from, vertex)) {
                    fromOnVertex = true;
                    fromVertex = vertex;
                }
                if (isPointEqual(to, vertex)) {
                    toOnVertex = true;
                    toVertex = vertex;
                }
            }
            
            // If both points are on vertices of the same blockage
            if (fromOnVertex && toOnVertex) {
                // Check if these vertices are connected by a blockage line
                for (int i = 0; i < vertices.size() - 1; i++) {
                    Position v1 = vertices.get(i);
                    Position v2 = vertices.get(i + 1);
                    if ((isPointEqual(fromVertex, v1) && isPointEqual(toVertex, v2)) ||
                        (isPointEqual(fromVertex, v2) && isPointEqual(toVertex, v1))) {
                        return true; // Movement along blockage line between vertices
                    }
                }
            }
            
            // If one point is on a vertex, check if the movement is along a blockage line
            if (fromOnVertex || toOnVertex) {
                for (int i = 0; i < vertices.size() - 1; i++) {
                    Position v1 = vertices.get(i);
                    Position v2 = vertices.get(i + 1);
                    if (isPointOnLine(from, v1, v2) && isPointOnLine(to, v1, v2)) {
                        return true; // Moving along the blockage line
                    }
                }
            }
        }

        // Check if any point along the path is inside a blockage
        if (Math.abs(from.x - to.x) < EPSILON) {
            // Vertical movement
            double minY = Math.min(from.y, to.y);
            double maxY = Math.max(from.y, to.y);
            for (double y = minY; y <= maxY; y += 1.0) {
                Position pos = new Position(from.x, y);
                if (isInsideBlockage(pos, blockages)) {
                    return true;
                }
            }
        } else if (Math.abs(from.y - to.y) < EPSILON) {
            // Horizontal movement
            double minX = Math.min(from.x, to.x);
            double maxX = Math.max(from.x, to.x);
            for (double x = minX; x <= maxX; x += 1.0) {
                Position pos = new Position(x, from.y);
                if (isInsideBlockage(pos, blockages)) {
                    return true;
                }
            }
        }

        // Check if the path crosses or moves along any blockage
        for (PlannerBlockage blockage : blockages) {
            if (blockage.blocksRoute(from, to)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPointOnLine(Position p, Position lineStart, Position lineEnd) {
        // Check if point p lies on the line segment between lineStart and lineEnd
        if (Math.abs(lineStart.x - lineEnd.x) < EPSILON) {
            // Vertical line
            return Math.abs(p.x - lineStart.x) < EPSILON &&
                   p.y >= Math.min(lineStart.y, lineEnd.y) - EPSILON &&
                   p.y <= Math.max(lineStart.y, lineEnd.y) + EPSILON;
        } else {
            // Horizontal line
            return Math.abs(p.y - lineStart.y) < EPSILON &&
                   p.x >= Math.min(lineStart.x, lineEnd.x) - EPSILON &&
                   p.x <= Math.max(lineStart.x, lineEnd.x) + EPSILON;
        }
    }

    // Convention is that empty is for from = to, null is for no possible path
    private static List<Position> buildManhattanPath(Position from, Position to, List<PlannerBlockage> blockages) {
        if (from.equals(to)) {
            return new ArrayList<>();
        }

        // Check if points are within grid boundaries
        if (!isWithinBounds(from) || !isWithinBounds(to)) {
            return null;
        }

        List<Position> path = new ArrayList<>();
        path.add(from);

        // Try first L-shape: Move in Y direction first, then X
        Position pivot1 = new Position(from.x, to.y);
        if (isWithinBounds(pivot1) && !isPathBlocked(from, pivot1, blockages) && !isPathBlocked(pivot1, to, blockages)) {
            path.add(pivot1);
            path.add(to);
            return path;
        }

        // Try second L-shape: Move in X direction first, then Y
        path.clear();
        path.add(from);
        Position pivot2 = new Position(to.x, from.y);
        if (isWithinBounds(pivot2) && !isPathBlocked(from, pivot2, blockages) && !isPathBlocked(pivot2, to, blockages)) {
            path.add(pivot2);
            path.add(to);
            return path;
        }

        return null;
    }

    private static boolean isWithinBounds(Position pos) {
        return pos.x >= 0 && pos.x <= SimulationProperties.gridLength &&
               pos.y >= 0 && pos.y <= SimulationProperties.gridWidth;
    }

    // Convention is that empty is for from = to, null is for no possible path
    private static List<Position> buildAstarPath(Position from, Position to, List<PlannerBlockage> blockages) {
        return AStarPathfinder.findPath(from, to, blockages);
    }

    private static List<Position> compressPath(List<Position> path) {
        if (path.size() < 3) {
            return path;
        }

        List<Position> compressedPath = new ArrayList<>();
        compressedPath.add(path.get(0));

        Position prev = path.get(0);
        Position current;
        Position next;

        for (int i = 1; i < path.size() - 1; i++) {
            current = path.get(i);
            next = path.get(i + 1);

            // Skip if current point is same as previous or next point
            if (Math.abs(current.x - prev.x) < EPSILON && Math.abs(current.y - prev.y) < EPSILON ||
                Math.abs(current.x - next.x) < EPSILON && Math.abs(current.y - next.y) < EPSILON) {
                continue;
            }

            // Check if current point represents a direction change
            boolean isDirectionChange = 
                !((Math.abs((current.x - prev.x) - (next.x - current.x)) < EPSILON && 
                   Math.abs((current.y - prev.y) - (next.y - current.y)) < EPSILON) || // Same direction
                  (Math.abs(current.x - prev.x) < EPSILON && Math.abs(current.x - next.x) < EPSILON) || // Vertical line
                  (Math.abs(current.y - prev.y) < EPSILON && Math.abs(current.y - next.y) < EPSILON));  // Horizontal line

            if (isDirectionChange) {
                compressedPath.add(current);
                prev = current;
            }
        }

        Position lastPoint = path.get(path.size() - 1);
        Position lastCompressed = compressedPath.get(compressedPath.size() - 1);
        
        // Only add last point if it's different from the last compressed point
        if (Math.abs(lastPoint.x - lastCompressed.x) > EPSILON || 
            Math.abs(lastPoint.y - lastCompressed.y) > EPSILON) {
            compressedPath.add(lastPoint);
        }

        return compressedPath;
    }
}