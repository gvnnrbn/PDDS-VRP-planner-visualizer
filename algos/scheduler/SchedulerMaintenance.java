package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.Time;

public class SchedulerMaintenance {
    int id;
    String vehicleId;
    Time date;

    SchedulerMaintenance(int id, String vehicleId, Time date) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
    }

    public static List<SchedulerMaintenance> parseMaintenances(String filePath) {
        List<SchedulerMaintenance> maintenances = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                
                int date = Integer.parseInt(parts[0].trim());
                String vehicleId = parts[1].trim();

                SchedulerMaintenance maintenance = new SchedulerMaintenance(id++, vehicleId, new Time(date/10000,date%10000/100, date%100, 0, 0));
                maintenances.add(maintenance);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return maintenances;
    }
}
