package domain_environment;
import java.util.List;

/*
 * List of vertices that form an open polygon
 */
public class Blockage {
    public List<Position> positions;

    // from -> to is a linear path
    public boolean isBlocked(Position from, Position to) {
        // Check if the path intersects with any line segment of the blockage
        // It can still end at one vertex of the blockage
        for (int i = 0; i < positions.size() - 1; i++) {
            Position p1 = positions.get(i);
            Position p2 = positions.get(i + 1);

            // Line segment intersection check using parametric equations
            int x1 = from.x;
            int y1 = from.y;
            int x2 = to.x;
            int y2 = to.y;
            int x3 = p1.x;
            int y3 = p1.y;
            int x4 = p2.x;
            int y4 = p2.y;

            // Calculate denominator for intersection formulas
            float denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            
            // Skip if lines are parallel or nearly parallel (accounting for float precision)
            if (Math.abs(denominator) < 0.000001f) {
                continue;
            }

            // Calculate intersection parameters
            float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator;
            float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator;

            // Check if intersection point lies on both line segments
            if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                return true; // Path intersects with blockage
            }
        }
        return false; // No intersection found, path is not blocked
    }
}
