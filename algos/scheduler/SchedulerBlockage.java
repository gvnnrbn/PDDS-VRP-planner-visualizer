package scheduler;

import domain.Position;
import utils.Time;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SchedulerBlockage {
    public int id;
    public Time startTime;
    public Time endTime;
    public List<Position> vertices; // check

    public SchedulerBlockage(int id, Time startTime, Time endTime, List<Position> vertices) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.vertices = vertices;
    }

    public static List<SchedulerBlockage> parseBlockages(String filePath) {
        List<SchedulerBlockage> blockages = new ArrayList<>();
        
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

                blockages.add(new SchedulerBlockage(id++, startTime, endTime, vertices));
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
}
