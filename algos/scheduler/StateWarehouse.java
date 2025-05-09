package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Position;

public class StateWarehouse {
    int id;
    Position position;
    int maxGLP;
    int currentGLP;
    boolean isMain;

    StateWarehouse(int id, Position position, int maxGLP, int currentGLP, boolean isMain) {
        this.position = position;
        this.maxGLP = maxGLP;
        this.currentGLP = currentGLP;
        this.isMain = isMain;
    }

    public static List<StateWarehouse> parseWarehouses(String filePath) {
        List<StateWarehouse> warehouses = new ArrayList<>();
        
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
                StateWarehouse warehouse = new StateWarehouse(id,new Position(x, y), maxGLP, maxGLP, isMain);
                warehouses.add(warehouse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return warehouses;
    }

}
