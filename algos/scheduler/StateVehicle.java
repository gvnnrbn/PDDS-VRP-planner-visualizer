package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Position;
import domain.Vehicle;

public class StateVehicle {
    String id; // TA01
    Vehicle algorithmVehicle;
    String type;
    boolean hasRoute; // hide in map if false (it's in warehouse)
    boolean hasFailure; // marks vehicle the moment a failure happens
    Failure failure;
    boolean hasMaintenance; // marks vehicle on its way to maintenance
    Maintenance maintenance;
    
    StateVehicle(String id, Vehicle algorithmVehicle, String type, boolean hasRoute, boolean hasFailure,
            Failure failure, boolean hasMaintenance, Maintenance maintenance) {
        this.id = id;
        this.algorithmVehicle = algorithmVehicle;
        this.type = type;
        this.hasRoute = hasRoute;
        this.hasFailure = hasFailure;
        this.failure = failure;
        this.hasMaintenance = hasMaintenance;
        this.maintenance = maintenance;
    }

    public static List<StateVehicle> parseVehicles(String filePath) {
        List<StateVehicle> vehicles = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID for vehicles
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 6) continue;
                
                String type = parts[0].trim();
                int weight = (int) Double.parseDouble(parts[0].trim());
                int maxGLP = (int) Double.parseDouble(parts[2].trim());
                double currentGLP = Double.parseDouble(parts[3].trim());
                int fuel = (int) Double.parseDouble(parts[4].trim());
                int amountOfUnits = Integer.parseInt(parts[5].trim());
                
                // Create vehicle with all required fields
                for (int i = 0; i < amountOfUnits; i++) {
                    StateVehicle vehicle = new StateVehicle(
                            type + "0" + id,
                            new Vehicle(id, weight, fuel, fuel, maxGLP, (int) currentGLP, new Position(0, 0)),
                            type,
                            false,
                            false,
                            null,
                            false,
                            null);
                    vehicles.add(vehicle);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vehicles;
    }
    
}
