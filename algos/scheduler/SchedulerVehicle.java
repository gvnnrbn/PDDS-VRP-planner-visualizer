package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Node;
import domain.Position;
import utils.PathBuilder;

public class SchedulerVehicle {
    public int id;
    public String plaque; // TA01
    public String type;
    public EnumVehicleState state;
    public SchedulerFailure failure;
    public SchedulerMaintenance maintenance;
    public int waitTransition;
    // Vehicle class attributes:
    public int weight;
    public int maxFuel;
    public double currentFuel;
    public int maxGLP;
    public int currentGLP;
    public Position position;

    public List<Position> currentPath;
    public Node currentNode;
    
    public SchedulerVehicle(int id, String plaque, String type,EnumVehicleState state, int weight, int maxFuel, 
    double currentFuel, int maxGLP, int currentGLP, Position position) {
        this.id = id;
        this.plaque = plaque;
        this.type = type;
        this.state = state;
        this.weight = weight;
        this.maxFuel = maxFuel;
        this.currentFuel = currentFuel;
        this.maxGLP = maxGLP;
        this.currentGLP = currentGLP;
        this.position = position;
        this.waitTransition = 0;
    }

    public static List<SchedulerVehicle> parseVehicles(String filePath, Position position) {
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
                    SchedulerVehicle vehicle = new SchedulerVehicle(i,type+""+i,type,EnumVehicleState.IDLE, 
                    weight, (int) fuel, fuel, maxGLP, currentGLP, new Position(position.x(), position.y()));
                            
                    vehicles.add(vehicle);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vehicles;
    }
    
    public void advancePath(double units) {
        if (currentPath == null || currentPath.isEmpty() || currentPath.size() < 2) {
            currentPath = null;
            return;
        }

        Position from = currentPath.get(0);
        Position to = currentPath.get(1);

        while (units > 0 && currentPath.size() > 1) {
            double distance = PathBuilder.calculateDistance(List.of(from,to));
            if (distance > units) { 
                // Move 'from' position the corresponding amount of units
                double deltaX = to.x() - from.x();
                double deltaY = to.y() - from.y();
                currentPath.set(0, new Position(from.x() + deltaX, from.y() + deltaY));
                units = 0;
            } else if (distance <= units) {
                // Remove 'from' position from path
                currentPath.remove(0);
                units -= distance;
            }
        }
    }
}
