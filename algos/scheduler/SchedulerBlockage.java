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
    
                String[] parts = line.split("-");
                if (parts.length != 2) continue;
    
                // Parse start time (ej: 01d00h01m)
                String[] timeParts = parts[0].split("[dhm]");
                if (timeParts.length < 3) continue;
    
                int day = Integer.parseInt(timeParts[0]);
                int hour = Integer.parseInt(timeParts[1]);
                int minute = Integer.parseInt(timeParts[2]);                
                Time startTime = new Time(0,0, day, hour, minute);

                String[] parts2 = line.split(":");
                if (parts2.length != 2) continue;

                // Parse end time (ej: 01d00h01m)
                String[] timeParts2 = parts2[0].split("[dhm]");
                if (timeParts2.length < 3) continue;
    
                day = Integer.parseInt(timeParts2[0]);
                hour = Integer.parseInt(timeParts2[1]);
                minute = Integer.parseInt(timeParts2[2]);
                
                Time endTime = new Time(0,0, day, hour, minute);

                // Parse vertices
                String[] coordinates = parts[1].split(",");
                List<Position> vertices = new ArrayList<>();
                // Process coordinates in pairs (x,y)
                for (int i = 0; i < coordinates.length; i += 2) {
                    int x = Integer.parseInt(coordinates[i]);
                    int y = Integer.parseInt(coordinates[i + 1]);
                    vertices.add(new Position(x, y));
                }

                SchedulerBlockage blockage = new SchedulerBlockage(id++, startTime, endTime, vertices);
                blockages.add(blockage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return blockages;
    }
}
