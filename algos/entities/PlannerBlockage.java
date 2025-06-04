package entities;

import java.util.ArrayList;
import java.util.List;
import utils.Time;
import utils.Position;

public class PlannerBlockage implements Cloneable {
    private static final double EPSILON = 1e-6;
    public int id;
    public Time startTime;
    public Time endTime;
    public List<Position> vertices;

    public PlannerBlockage(int id, Time startTime, Time endTime, List<Position> vertices) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.vertices = vertices;
    }

    // Returns true if the movement from a to b is blocked
    public boolean blocksRoute(Position a, Position b) {
        if (vertices.isEmpty()) {
            return false;
        }
        
        // Verify points are colinear (movement must be grid-aligned)
        if (Math.abs(a.x - b.x) > EPSILON && Math.abs(a.y - b.y) > EPSILON) {
            throw new IllegalArgumentException("Route points must be colinear (horizontal or vertical)");
        }

        // Check if either point is exactly on a vertex (allowed)
        for (Position vertex : vertices) {
            if (isPointEqual(a, vertex) || isPointEqual(b, vertex)) {
                return false;
            }
        }
        
        // Check if the movement intersects with any blockage segment
        for (int i = 0; i < vertices.size() - 1; i++) {
            Position v1 = vertices.get(i);
            Position v2 = vertices.get(i + 1);
            
            // Check if movement is along the blockage line
            if (isPointOnLine(a, v1, v2) && isPointOnLine(b, v1, v2)) {
                return true; // Moving along the blockage line is not allowed
            }
            
            // Check if movement crosses the blockage line
            if (doSegmentsIntersect(a, b, v1, v2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointEqual(Position p1, Position p2) {
        return Math.abs(p1.x - p2.x) < EPSILON && Math.abs(p1.y - p2.y) < EPSILON;
    }

    private boolean isPointOnLine(Position p, Position lineStart, Position lineEnd) {
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

    private boolean doSegmentsIntersect(Position a, Position b, Position c, Position d) {
        // Don't consider endpoint intersections as blocking
        if (isPointEqual(a, c) || isPointEqual(a, d) || isPointEqual(b, c) || isPointEqual(b, d)) {
            return false;
        }

        // Vertical route and vertical blockage
        if (Math.abs(a.x - b.x) < EPSILON && Math.abs(c.x - d.x) < EPSILON) {
            return Math.abs(a.x - c.x) < EPSILON && 
                   Math.max(Math.min(a.y, b.y), Math.min(c.y, d.y)) < 
                   Math.min(Math.max(a.y, b.y), Math.max(c.y, d.y));
        }

        // Vertical route and horizontal blockage
        if (Math.abs(a.x - b.x) < EPSILON && Math.abs(c.y - d.y) < EPSILON) {
            return Math.min(a.y, b.y) < c.y && c.y < Math.max(a.y, b.y) &&
                   Math.min(c.x, d.x) < a.x && a.x < Math.max(c.x, d.x);
        }

        // Horizontal route and vertical blockage
        if (Math.abs(a.y - b.y) < EPSILON && Math.abs(c.x - d.x) < EPSILON) {
            return Math.min(a.x, b.x) < c.x && c.x < Math.max(a.x, b.x) &&
                   Math.min(c.y, d.y) < a.y && a.y < Math.max(c.y, d.y);
        }

        // Horizontal route and horizontal blockage
        if (Math.abs(a.y - b.y) < EPSILON && Math.abs(c.y - d.y) < EPSILON) {
            return Math.abs(a.y - c.y) < EPSILON && 
                   Math.max(Math.min(a.x, b.x), Math.min(c.x, d.x)) < 
                   Math.min(Math.max(a.x, b.x), Math.max(c.x, d.x));
        }

        return false;
    }

    public boolean isActive(Time currentTime) {
        return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
    }

    @Override
    public String toString() {
        StringBuilder verticesStr = new StringBuilder("[");
        for (int i = 0; i < vertices.size(); i++) {
            Position vertex = vertices.get(i);
            verticesStr.append("(").append(vertex.x).append(",").append(vertex.y).append(")");
            if (i < vertices.size() - 1) {
                verticesStr.append(", ");
            }
        }
        verticesStr.append("]");

        return "PlannerBlockage{" +
            "id=" + id +
            ", startTime=" + startTime.toString() +
            ", endTime=" + endTime.toString() +
            ", duration=" + endTime.minutesSince(startTime) + " minutes" +
            ", vertices=" + verticesStr +
            ", isActive=" + isActive(new Time(0, 0, 1, 0, 0)) +
            '}';
    }

    @Override
    public PlannerBlockage clone() {
        try {
            PlannerBlockage clone = new PlannerBlockage(
                this.id,
                this.startTime.clone(),
                this.endTime.clone(),
                new ArrayList<>(this.vertices.size())
            );
            
            // Clone each position in the vertices list
            for (Position position : this.vertices) {
                clone.vertices.add(position.clone());
            }
            
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }
}
