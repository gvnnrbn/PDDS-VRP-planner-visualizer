package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Position;

public class SchedulerVehicle {
    String id; // TA01
    String type;
    EnumVehicleState state;
    // Vehicle class attributes:
    int weight;
    int maxFuel;
    double currentFuel;
    int maxGLP;
    int currentGLP;
    Position initialPosition;
    
    SchedulerVehicle(String id, String type,EnumVehicleState state, int weight, int maxFuel, 
    double currentFuel, int maxGLP, int currentGLP, Position initialPosition) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.weight = weight;
        this.maxFuel = maxFuel;
        this.currentFuel = currentFuel;
        this.maxGLP = maxGLP;
        this.currentGLP = currentGLP;
        this.initialPosition = initialPosition;
        
    }

    public static List<SchedulerVehicle> parseVehicles(String filePath, Position initialPosition) {
        List<SchedulerVehicle> vehicles = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 6) continue;
                
                String type = parts[0].trim();
                int weight = (int) Double.parseDouble(parts[1].trim());
                int maxGLP = (int) Double.parseDouble(parts[2].trim());
                int currentGLP = (int) Double.parseDouble(parts[3].trim());
                double fuel = Double.parseDouble(parts[4].trim());
                int amountOfUnits = Integer.parseInt(parts[5].trim());
                
                // Create vehicle with all required fields
                for (int i = 0; i < amountOfUnits; i++) {
                    SchedulerVehicle vehicle = new SchedulerVehicle(type+""+i,type,EnumVehicleState.IDLE, 
                    weight, (int) fuel, fuel, maxGLP, currentGLP, new Position(initialPosition.x(), initialPosition.y()));
                            
                    vehicles.add(vehicle);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vehicles;
    }
    
}
