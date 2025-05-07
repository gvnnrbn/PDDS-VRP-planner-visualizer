package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Time;

public class Failure {
    int id;
    int type;
    int shift; // 1=00:00-08:00, 2=08:00-16:00, 3=16:00-24:00
    String vehicleId;
    Time timeTillRepaired;
    int minutesIdle;

    Failure(int id, int type, int shift, String vehicleId) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
        // set idle time (time as warehouse) and time till available for scheduling 
        switch (type) {
            case 1:
                this.minutesIdle = 120;
                break;
            case 2:
                this.minutesIdle = 120;
                switch (shift) {
                    case 1 -> this.timeTillRepaired = new Time(0, 0, 16, 0);
                    case 2 -> this.timeTillRepaired = new Time(0, 1, 0, 0);
                    case 3 -> this.timeTillRepaired = new Time(0, 1, 8, 0);
                    
                    default -> this.timeTillRepaired = new Time(0, 0, 0, 0);
                }
                break;
            case 3:
                this.minutesIdle = 240;
                this.timeTillRepaired = new Time(0, 2, 0, 0);
                break;
            default:
                break;
        }
    }

    public List<Failure> parseFailures(String filePath) {
        List<Failure> failures = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID for failures
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("_");
                if (parts.length != 3) continue;
                
                int shift = Integer.parseInt(parts[0].trim());
                String vehicleId = parts[1].trim();
                int type = Integer.parseInt(parts[2].trim());

                Failure failure = new Failure(id++, type, shift, vehicleId);
                failures.add(failure);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return failures;
    }
}
