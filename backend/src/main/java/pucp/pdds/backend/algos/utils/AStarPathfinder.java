package pucp.pdds.backend.algos.utils;

import java.util.*;
import pucp.pdds.backend.algos.entities.PlannerBlockage;

class AStarPathfinder {
    private static final double EPSILON = 1e-6;
    private static final int GRID_RESOLUTION = 1; // 1 unit grid for collision checking

    private static class Node implements Comparable<Node> {
        Position pos;
        Node parent;
        double gCost; // Cost from start to this node
        double hCost; // Estimated cost from this node to goal
        
        Node(Position pos, Node parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }
        
        double fCost() {
            return gCost + hCost;
        }
        
        @Override
        public int compareTo(Node other) {
            double diff = this.fCost() - other.fCost();
            if (Math.abs(diff) < EPSILON) {
                // If f-costs are equal, prefer the one with lower h-cost
                return Double.compare(this.hCost, other.hCost);
            }
            return Double.compare(this.fCost(), other.fCost());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) return false;
            Node other = (Node) obj;
            return Math.abs(this.pos.x - other.pos.x) < EPSILON && 
                   Math.abs(this.pos.y - other.pos.y) < EPSILON;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(Math.round(pos.x * 1000), Math.round(pos.y * 1000));
        }
    }

    static List<Position> findPath(Position start, Position end, List<PlannerBlockage> blockages) {
        // Add boundary check for start and end positions
        if (start == null || end == null || 
            !isWithinBounds(start) || !isWithinBounds(end)) {
            return null;
        }

        // Check if start or end is inside a blockage line (not at endpoints)
        for (PlannerBlockage blockage : blockages) {
            for (int i = 0; i < blockage.vertices.size() - 1; i++) {
                Position v1 = blockage.vertices.get(i);
                Position v2 = blockage.vertices.get(i + 1);
                
                // If point is on blockage line but not at endpoints, path is impossible
                if (isPointOnLine(start, v1, v2) && !isPointEqual(start, v1) && !isPointEqual(start, v2)) {
                    return null;
                }
                if (isPointOnLine(end, v1, v2) && !isPointEqual(end, v1) && !isPointEqual(end, v2)) {
                    return null;
                }
            }
        }

        if (Math.abs(start.x - end.x) < EPSILON && Math.abs(start.y - end.y) < EPSILON) {
            return new ArrayList<>();
        }

        // Initialize open and closed sets
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        
        // Create start node and add to open set
        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            // Check if we reached the goal
            if (Math.abs(current.pos.x - end.x) < EPSILON && Math.abs(current.pos.y - end.y) < EPSILON) {
                return reconstructPath(current);
            }
            
            closedSet.add(current);
            
            // Generate neighbors
            for (Position neighborPos : getNeighbors(current.pos)) {
                // Skip if this move would cross a blockage or move along it
                if (isBlocked(current.pos, neighborPos, blockages)) continue;
                
                // Skip if either current position or neighbor is inside a blockage
                if (isInsideBlockage(current.pos, blockages) || isInsideBlockage(neighborPos, blockages)) continue;
                
                Node neighbor = new Node(
                    neighborPos,
                    current,
                    current.gCost + GRID_RESOLUTION,
                    heuristic(neighborPos, end)
                );
                
                // Skip if we already processed this node
                if (closedSet.contains(neighbor)) continue;
                
                // If this is a new node or we found a better path to it
                if (!openSet.contains(neighbor) || 
                    neighbor.gCost < findNodeInSet(openSet, neighbor).gCost) {
                    openSet.add(neighbor);
                }
            }
        }
        
        // No path found
        return null;
    }
    
    private static List<Position> getNeighbors(Position pos) {
        List<Position> neighbors = new ArrayList<>();
        
        // Right
        if (pos.x + GRID_RESOLUTION <= SimulationProperties.gridLength) {
            neighbors.add(new Position(pos.x + GRID_RESOLUTION, pos.y));
        }
        
        // Left
        if (pos.x - GRID_RESOLUTION >= 0) {
            neighbors.add(new Position(pos.x - GRID_RESOLUTION, pos.y));
        }
        
        // Up
        if (pos.y + GRID_RESOLUTION <= SimulationProperties.gridWidth) {
            neighbors.add(new Position(pos.x, pos.y + GRID_RESOLUTION));
        }
        
        // Down
        if (pos.y - GRID_RESOLUTION >= 0) {
            neighbors.add(new Position(pos.x, pos.y - GRID_RESOLUTION));
        }
        
        return neighbors;
    }
    
    private static boolean isBlocked(Position from, Position to, List<PlannerBlockage> blockages) {
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

        // Check for line segment intersections
        for (PlannerBlockage blockage : blockages) {
            List<Position> vertices = blockage.vertices;
            if (vertices.size() < 2) continue;
            
            for (int i = 0; i < vertices.size() - 1; i++) {
                Position v1 = vertices.get(i);
                Position v2 = vertices.get(i + 1);
                
                // Check if movement is along the blockage line
                if (isPointOnLine(from, v1, v2) && isPointOnLine(to, v1, v2)) {
                    return true;
                }
                
                // Check for intersection
                if (linesIntersect(from, to, v1, v2)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean linesIntersect(Position a1, Position a2, Position b1, Position b2) {
        // Calculate line intersection using cross products
        double denominator = (b2.y - b1.y) * (a2.x - a1.x) - (b2.x - b1.x) * (a2.y - a1.y);
        if (Math.abs(denominator) < EPSILON) return false;
        
        double ua = ((b2.x - b1.x) * (a1.y - b1.y) - (b2.y - b1.y) * (a1.x - b1.x)) / denominator;
        double ub = ((a2.x - a1.x) * (a1.y - b1.y) - (a2.y - a1.y) * (a1.x - b1.x)) / denominator;
        
        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1;
    }
    
    private static double heuristic(Position from, Position to) {
        // Using Manhattan distance as heuristic since we can only move in cardinal directions
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }
    
    private static List<Position> reconstructPath(Node endNode) {
        List<Position> path = new ArrayList<>();
        Node current = endNode;
        
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        
        return path;
    }
    
    private static Node findNodeInSet(PriorityQueue<Node> set, Node node) {
        for (Node n : set) {
            if (n.equals(node)) return n;
        }
        return null;
    }

    private static boolean isWithinBounds(Position pos) {
        return pos.x >= 0 && pos.x <= SimulationProperties.gridLength &&
               pos.y >= 0 && pos.y <= SimulationProperties.gridWidth;
    }

    private static boolean isPointEqual(Position p1, Position p2) {
        return Math.abs(p1.x - p2.x) < EPSILON && Math.abs(p1.y - p2.y) < EPSILON;
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
} 