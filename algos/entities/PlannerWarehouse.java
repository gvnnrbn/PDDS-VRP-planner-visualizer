package entities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.Position;

public class PlannerWarehouse implements Cloneable {
    public int id;
    public Position position;
    public int maxGLP;
    public int currentGLP;
    public boolean isMain;
    public boolean wasVehicle;

    public PlannerWarehouse(int id, Position position, int maxGLP, int currentGLP, boolean isMain, boolean wasVehicle) {
        this.id = id;
        this.position = position;
        this.maxGLP = maxGLP;
        this.currentGLP = currentGLP;
        this.isMain = isMain;
        this.wasVehicle = wasVehicle;
    }

    public static List<PlannerWarehouse> parseWarehouses(String filePath) {
        List<PlannerWarehouse> warehouses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID for warehouses
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3 || parts.length > 4) continue;
                
                // Parse warehouse details
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int maxGLP = Integer.parseInt(parts[2]);
                boolean isMain = parts.length == 4 && parts[3].equals("main");
                
                // Create warehouse with parsed values
                PlannerWarehouse warehouse = new PlannerWarehouse(
                    id,
                    new Position(x, y),
                    maxGLP,
                    maxGLP, // Start with full GLP
                    isMain,
                    false
                );
                warehouses.add(warehouse);
                id++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return warehouses;
    }

    @Override
    public String toString() {
        return "PlannerWarehouse{" +
            "id=" + id +
            ", position=" + position +
            ", maxGLP=" + maxGLP +
            ", currentGLP=" + currentGLP +
            ", isMain=" + isMain +
            ", wasVehicle=" + wasVehicle +
            '}';
    }

    @Override
    public PlannerWarehouse clone() {
        try {
            PlannerWarehouse clone = new PlannerWarehouse(
                this.id,
                this.position.clone(),
                this.maxGLP,
                this.currentGLP,
                this.isMain,
                this.wasVehicle
            );
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }
}
