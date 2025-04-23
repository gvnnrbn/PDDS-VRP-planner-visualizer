package domain;

import java.util.List;

public record Blockage(List<Position> vertices) {
    // Assumes a and b are colinear
    public boolean blocksRoute(Position a, Position b) {
        if (vertices.isEmpty()) {
            return false;
        }
        
        // Verify points are colinear
        if (a.x() != b.x() && a.y() != b.y()) {
            throw new IllegalArgumentException("Route points must be colinear (horizontal or vertical)");
        }
        
        // Check if any segment of the blockage intersects with the route segment
        for (int i = 0; i < vertices.size() - 1; i++) {
            Position v1 = vertices.get(i);
            Position v2 = vertices.get(i + 1);
            
            if (doSegmentsIntersect(a, b, v1, v2)) {
                return true;
            }
        }
        return false;
    }

    private boolean doSegmentsIntersect(Position a, Position b, Position c, Position d) {
        // Vertical route and vertical blockage
        if (a.x() == b.x() && c.x() == d.x()) {
            return a.x() == c.x() && 
                   Math.max(Math.min(a.y(), b.y()), Math.min(c.y(), d.y())) < 
                   Math.min(Math.max(a.y(), b.y()), Math.max(c.y(), d.y()));
        }

        // Vertical route and horizontal blockage
        if (a.x() == b.x() && c.x() != d.x()) {
            return Math.min(a.y(), b.y()) < c.y() && c.y() < Math.max(a.y(), b.y()) &&
                   Math.min(c.x(), d.x()) < a.x() && a.x() < Math.max(c.x(), d.x());
        }

        // Horizontal route and vertical blockage
        if (a.x() != b.x() && c.x() == d.x()) {
            return Math.min(a.x(), b.x()) < c.x() && c.x() < Math.max(a.x(), b.x()) &&
                   Math.min(c.y(), d.y()) < a.y() && a.y() < Math.max(c.y(), d.y());
        }

        // Horizontal route and horizontal blockage
        if (a.x() != b.x() && c.x() != d.x()) {
            return a.y() == c.y() && 
                   Math.max(Math.min(a.x(), b.x()), Math.min(c.x(), d.x())) < 
                   Math.min(Math.max(a.x(), b.x()), Math.max(c.x(), d.x()));
        }

        return false;
    }
}
