package utils;

import domain.Blockage;
import domain.Environment;
import domain.Order;
import domain.Position;
import domain.Time;
import domain.Vehicle;
import domain.Warehouse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: Domain classes are incomplete for real use (proper domain classes are needed)
// This class is only for testing and algorithm experiments
public class EnvironmentParser {
    private Time currentTime;

    public EnvironmentParser(Time currentTime) {
        this.currentTime = currentTime;
    }

    public Environment parseEnvironment(String vehiclesFilePath, String ordersFilePath, String blockagesFilePath, String warehousesFilePath) {
        List<Vehicle> vehicles = parseVehicles(vehiclesFilePath);
        List<Order> orders = parseOrders(ordersFilePath);
        List<Blockage> blockages = parseBlockages(blockagesFilePath);
        List<Warehouse> warehouses = parseWarehouses(warehousesFilePath);

        return new Environment(vehicles, orders, warehouses, blockages, currentTime);
    }

    public List<Order> parseOrders(String filePath) {
        List<Order> orders = new ArrayList<>();
        int orderId = 1;
    
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
    
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
    
                // Parsear timestamp de creación (ej: 01d00h01m)
                String[] timeParts = parts[0].split("[dhm]");
                if (timeParts.length < 3) continue;
    
                int day = Integer.parseInt(timeParts[0]);
                int hour = Integer.parseInt(timeParts[1]);
                int minute = Integer.parseInt(timeParts[2]);
    
                Time creationTime = new Time(1, day, hour, minute);  // Suponemos mes = 1
    
                // Parsear datos del pedido
                String[] orderParts = parts[1].split(",");
                if (orderParts.length != 5) continue;
    
                int x = Integer.parseInt(orderParts[0].trim());
                int y = Integer.parseInt(orderParts[1].trim());
    
                String amountStr = orderParts[3].replace("m3", "").trim();
                int amountGLP = Integer.parseInt(amountStr);
    
                String deadlineStr = orderParts[4].replace("h", "").trim();
                int deadlineHours = Integer.parseInt(deadlineStr);
    
                Time deadline = creationTime.addMinutes(deadlineHours * 60);
                Position position = new Position(x, y);
    
                Order order = new Order(orderId++, amountGLP, position, deadline);
                orders.add(order);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return orders;
    }
    

    public List<Vehicle> parseVehicles(String filePath) {
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID for vehicles
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 6) continue;
                
                // Parse vehicle details with trim() to remove whitespace
                int weight = (int) Double.parseDouble(parts[1].trim());
                int maxGLP = (int) Double.parseDouble(parts[2].trim());
                double currentGLP = Double.parseDouble(parts[3].trim());
                int fuel = (int) Double.parseDouble(parts[4].trim());
                int amountOfUnits = Integer.parseInt(parts[5].trim());
                
                // Create vehicle with all required fields
                for (int i = 0; i < amountOfUnits; i++) {
                    Vehicle vehicle = new Vehicle(
                        id++,                    // vehicle ID
                        weight,                  // weight
                        fuel,                    // maxFuel
                        fuel,                    // currentFuel
                        maxGLP,                  // maxGLP
                        (int) currentGLP,        // currentGLP
                        new Position(0, 0)       // initialPosition (default value)
                    );
                    vehicles.add(vehicle);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return vehicles;
    }

    public List<Blockage> parseBlockages(String filePath) {
        List<Blockage> blockages = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                
                // Parse vertices
                String[] coordinates = parts[1].split(",");
                List<Position> vertices = new ArrayList<>();
                
                // Process coordinates in pairs (x,y)
                for (int i = 0; i < coordinates.length; i += 2) {
                    int x = Integer.parseInt(coordinates[i]);
                    int y = Integer.parseInt(coordinates[i + 1]);
                    vertices.add(new Position(x, y));
                }
                
                // Create blockage with vertices
                Blockage blockage = new Blockage(vertices);
                blockages.add(blockage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return blockages;
    }

    public List<Warehouse> parseWarehouses(String filePath) {
        List<Warehouse> warehouses = new ArrayList<>();
        
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
                Warehouse warehouse = new Warehouse(
                    id++,                    // warehouse ID
                    new Position(x, y),      // position
                    maxGLP,                  // currentGLP (starts full)
                    maxGLP,                  // maxGLP
                    isMain                   // isMain
                );
                warehouses.add(warehouse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return warehouses;
    }
}
