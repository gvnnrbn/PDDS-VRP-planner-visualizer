package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.Time;

public class SchedulerMaintenance {
    public int id;
    public String vehiclePlaque;
    public Time startDate;
    public Time endDate; 

    public SchedulerMaintenance(int id, String vehiclePlaque, Time startDate, Time endDate) {
        this.id = id;
        this.vehiclePlaque = vehiclePlaque;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static List<SchedulerMaintenance> parseMaintenances(String filePath) {
        List<SchedulerMaintenance> maintenances = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                
                int startDate = Integer.parseInt(parts[0].trim());
                String vehiclePlaque = parts[1].trim();
                Time start = new Time(startDate/10000,startDate%10000/100, startDate%100, 0, 0);
                Time end = start.addTime(new Time(0, 0, 1, 0, 0));
                SchedulerMaintenance maintenance = new SchedulerMaintenance(id++, vehiclePlaque, start, end);
                maintenances.add(maintenance);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return maintenances;
    }
}
