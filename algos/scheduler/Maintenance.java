package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Time;

public class Maintenance {
    int id;
    String vehicleId;
    int year;
    Time date;

    Maintenance(int id, String vehicleId, int year, Time date) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.year = year;
        this.date = date;
    }

    public static List<Maintenance> parseMaintenances(String filePath) {
        List<Maintenance> maintenances = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                
                int date = Integer.parseInt(parts[0].trim());
                String vehicleId = parts[1].trim();

                Maintenance maintenance = new Maintenance(id++, vehicleId, date/10000, new Time(date%10000/100, date%100, 0, 0));
                maintenances.add(maintenance);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return maintenances;
    }
}
