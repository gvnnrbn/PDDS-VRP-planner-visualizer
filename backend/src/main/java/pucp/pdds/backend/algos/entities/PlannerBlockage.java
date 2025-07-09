package pucp.pdds.backend.algos.entities;

import java.util.ArrayList;
import java.util.List;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.model.Bloqueo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

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

    public static PlannerBlockage fromEntity(Bloqueo bloqueo) {
        Time startTime = new Time(
            bloqueo.getStartTime().getYear(),
            bloqueo.getStartTime().getMonthValue(),
            bloqueo.getStartTime().getDayOfMonth(),
            bloqueo.getStartTime().getHour(),
            bloqueo.getStartTime().getMinute()
        );
        
        Time endTime = new Time(
            bloqueo.getEndTime().getYear(),
            bloqueo.getEndTime().getMonthValue(),
            bloqueo.getEndTime().getDayOfMonth(),
            bloqueo.getEndTime().getHour(),
            bloqueo.getEndTime().getMinute()
        );
        
        List<Position> vertices = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> verticesData = mapper.readValue(
                bloqueo.getVerticesJson(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> vertexData : verticesData) {
                double x = ((Number) vertexData.get("x")).doubleValue();
                double y = ((Number) vertexData.get("y")).doubleValue();
                vertices.add(new Position(x, y));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vertices JSON: " + e.getMessage());
        }
        
        return new PlannerBlockage(
            bloqueo.getId().intValue(),
            startTime,
            endTime,
            vertices
        );
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

        // Check if either point is exactly on a vertex
        boolean aOnVertex = false;
        boolean bOnVertex = false;
        Position aVertex = null;
        Position bVertex = null;
        
        for (Position vertex : vertices) {
            if (isPointEqual(a, vertex)) {
                aOnVertex = true;
                aVertex = vertex;
            }
            if (isPointEqual(b, vertex)) {
                bOnVertex = true;
                bVertex = vertex;
            }
        }

        // If both points are on vertices, check if they're connected by a blockage line
        if (aOnVertex && bOnVertex) {
            for (int i = 0; i < vertices.size() - 1; i++) {
                Position v1 = vertices.get(i);
                Position v2 = vertices.get(i + 1);
                if ((isPointEqual(v1, aVertex) && isPointEqual(v2, bVertex)) ||
                    (isPointEqual(v1, bVertex) && isPointEqual(v2, aVertex))) {
                    return true;
                }
            }
        }

        // Check if the line segment intersects any blockage line
        for (int i = 0; i < vertices.size() - 1; i++) {
            Position v1 = vertices.get(i);
            Position v2 = vertices.get(i + 1);
            if (doLinesIntersect(a, b, v1, v2)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPointEqual(Position p1, Position p2) {
        return Math.abs(p1.x - p2.x) < EPSILON && Math.abs(p1.y - p2.y) < EPSILON;
    }

    private boolean doLinesIntersect(Position p1, Position p2, Position p3, Position p4) {
        // Check if lines are colinear
        if (Math.abs(p1.x - p2.x) < EPSILON && Math.abs(p3.x - p4.x) < EPSILON) {
            // Both lines are vertical
            if (Math.abs(p1.x - p3.x) > EPSILON) return false;
            return !(Math.max(p1.y, p2.y) < Math.min(p3.y, p4.y) || 
                    Math.min(p1.y, p2.y) > Math.max(p3.y, p4.y));
        }
        if (Math.abs(p1.y - p2.y) < EPSILON && Math.abs(p3.y - p4.y) < EPSILON) {
            // Both lines are horizontal
            if (Math.abs(p1.y - p3.y) > EPSILON) return false;
            return !(Math.max(p1.x, p2.x) < Math.min(p3.x, p4.x) || 
                    Math.min(p1.x, p2.x) > Math.max(p3.x, p4.x));
        }

        // One line is vertical, one is horizontal
        if (Math.abs(p1.x - p2.x) < EPSILON) {
            // First line is vertical
            double x = p1.x;
            double y = p3.y;
            return x >= Math.min(p3.x, p4.x) && x <= Math.max(p3.x, p4.x) &&
                   y >= Math.min(p1.y, p2.y) && y <= Math.max(p1.y, p2.y);
        } else {
            // First line is horizontal
            double x = p3.x;
            double y = p1.y;
            return x >= Math.min(p1.x, p2.x) && x <= Math.max(p1.x, p2.x) &&
                   y >= Math.min(p3.y, p4.y) && y <= Math.max(p3.y, p4.y);
        }
    }

    public boolean isActive(Time currTime) {
        return currTime.isAfterOrAt(startTime) && currTime.isBeforeOrAt(endTime);
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
            '}';
    }

    @Override
    public PlannerBlockage clone() {
        List<Position> clonedVertices = new ArrayList<>();
        for (Position vertex : vertices) {
            clonedVertices.add(vertex.clone());
        }
        return new PlannerBlockage(id, startTime.clone(), endTime.clone(), clonedVertices);
    }
}
