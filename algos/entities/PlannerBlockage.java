package entities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import utils.Time;
import utils.Position;

public class PlannerBlockage implements Cloneable {
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

    // Assumes a and b are colinear
    public boolean blocksRoute(Position a, Position b) {
        if (vertices.isEmpty()) {
            return false;
        }
        
        // Verify points are colinear
        if (a.x != b.x && a.y != b.y) {
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
        if (a.x == b.x && c.x == d.x) {
            return a.x == c.x && 
                   Math.max(Math.min(a.y, b.y), Math.min(c.y, d.y)) < 
                   Math.min(Math.max(a.y, b.y), Math.max(c.y, d.y));
        }

        // Vertical route and horizontal blockage
        if (a.x == b.x && c.x != d.x) {
            return Math.min(a.y, b.y) < c.y && c.y < Math.max(a.y, b.y) &&
                   Math.min(c.x, d.x) < a.x && a.x < Math.max(c.x, d.x);
        }

        // Horizontal route and vertical blockage
        if (a.x != b.x && c.x == d.x) {
            return Math.min(a.x, b.x) < c.x && c.x < Math.max(a.x, b.x) &&
                   Math.min(c.y, d.y) < a.y && a.y < Math.max(c.y, d.y);
        }

        // Horizontal route and horizontal blockage
        if (a.x != b.x && c.x != d.x) {
            return a.y == c.y && 
                   Math.max(Math.min(a.x, b.x), Math.min(c.x, d.x)) < 
                   Math.min(Math.max(a.x, b.x), Math.max(c.x, d.x));
        }

        return false;
    }

    public static List<PlannerBlockage> parseBlockages(String filePath) {
        List<PlannerBlockage> blockages = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(":");
                if (parts.length != 2) continue;

                String timeRange = parts[0]; // Ej: 01d00h17m-01d23h59m
                String coordsStr = parts[1]; // Ej: 05,20,05,35

                String[] timeBounds = timeRange.split("-");
                if (timeBounds.length != 2) continue;

                Time startTime = parseCustomTime(timeBounds[0]);
                Time endTime = parseCustomTime(timeBounds[1]);

                if (startTime == null || endTime == null) continue;

                String[] coordinates = coordsStr.split(",");
                if (coordinates.length % 2 != 0) continue;

                List<Position> vertices = new ArrayList<>();
                for (int i = 0; i < coordinates.length; i += 2) {
                    int x = Integer.parseInt(coordinates[i].trim());
                    int y = Integer.parseInt(coordinates[i + 1].trim());
                    vertices.add(new Position(x, y));
                }

                blockages.add(new PlannerBlockage(id++, startTime, endTime, vertices));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return blockages;
    }

    private static Time parseCustomTime(String timeStr) {
        try {
            // Ej: 01d00h17m
            int dIndex = timeStr.indexOf('d');
            int hIndex = timeStr.indexOf('h');
            int mIndex = timeStr.indexOf('m');
    
            if (dIndex == -1 || hIndex == -1 || mIndex == -1) return null;
    
            int day = Integer.parseInt(timeStr.substring(0, dIndex));
            int hour = Integer.parseInt(timeStr.substring(dIndex + 1, hIndex));
            int minute = Integer.parseInt(timeStr.substring(hIndex + 1, mIndex));
    
            return new Time(0, 0, day, hour, minute); // fix: year and month must keep up with simulation
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isActive(Time currentTime) {
        return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
    }

    @Override
    public String toString() {
        return "PlannerBlockage{" +
            "id=" + id +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", vertices=" + vertices +
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
