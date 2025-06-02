package utils;

import entities.PlannerBlockage;
import entities.PlannerOrder;
import entities.PlannerVehicle;
import entities.PlannerWarehouse;
import algorithm.Environment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentParser {
    private Time currentTime;

    public EnvironmentParser(Time currentTime) {
        this.currentTime = currentTime;
    }

    public Environment parseEnvironment(String vehiclesFilePath, String ordersFilePath, String blockagesFilePath, String warehousesFilePath) {
        List<PlannerVehicle> vehicles = parseVehicles(vehiclesFilePath);
        List<PlannerOrder> orders = parseOrders(ordersFilePath);
        List<PlannerBlockage> blockages = parseBlockages(blockagesFilePath);
        List<PlannerWarehouse> warehouses = parseWarehouses(warehousesFilePath);

        return new Environment(vehicles, orders, warehouses, blockages, currentTime);
    }

    public List<PlannerOrder> parseOrders(String filePath) {
        List<PlannerOrder> orders = new ArrayList<>();
        int orderId = 1;
    
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
    
                // Split line into timestamp and order data
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }
    
                // Parse timestamp (e.g., 11d13h31m)
                String[] timeParts = parts[0].split("[dhm]");
                if (timeParts.length != 3) {
                    System.err.println("Invalid time format: " + parts[0]);
                    continue;
                }
    
                try {
                    int day = Integer.parseInt(timeParts[0]);
                    int hour = Integer.parseInt(timeParts[1]);
                    int minute = Integer.parseInt(timeParts[2]);
                    Time creationTime = new Time(0, 1, day, hour, minute);
    
                    // Parse order data (x,y,c-idClient,m3,h)
                    String[] orderParts = parts[1].split(",");
                    if (orderParts.length != 5) {
                        System.err.println("Invalid order data format: " + parts[1]);
                        continue;
                    }
    
                    int x = Integer.parseInt(orderParts[0].trim());
                    int y = Integer.parseInt(orderParts[1].trim());
                    String clientId = orderParts[2].trim();
                    
                    // Parse amount (remove m3 suffix)
                    String amountStr = orderParts[3].replace("m3", "").trim();
                    int amountGLP = Integer.parseInt(amountStr);
                    
                    // Parse deadline hours (remove h suffix)
                    String deadlineStr = orderParts[4].replace("h", "").trim();
                    int deadlineHours = Integer.parseInt(deadlineStr);
                    Time deadline = creationTime.addMinutes(deadlineHours * 60);
    
                    Position position = new Position(x, y);
                    PlannerOrder order = new PlannerOrder(orderId++, creationTime, position, amountGLP, clientId, deadline);
                    orders.add(order);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    
        return orders;
    }
    

    public List<PlannerVehicle> parseVehicles(String filePath) {
        List<PlannerVehicle> vehicles = new ArrayList<>();
        int id = 1;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header lines
                if (line.contains("Tipo") || line.contains("Unidades")) continue;

                // Split line by commas
                String[] parts = line.split(",");
                if (parts.length != 6) {
                    System.err.println("Invalid vehicle format: " + line);
                    continue;
                }

                try {
                    // Extract vehicle type (TA, TB, TC, TD)
                    String type = parts[0];
                    
                    // Convert weights from Ton to kg (1 Ton = 1000 kg)
                    double grossWeight = Double.parseDouble(parts[1]) * 1000;
                    
                    // Load capacity in m3
                    int maxGLP = Integer.parseInt(parts[2]);
                    
                    // Number of units
                    int amountOfUnits = Integer.parseInt(parts[5]);
                    
                    // Create vehicles
                    for (int i = 0; i < amountOfUnits; i++) {
                        // Generate plaque using type and sequential number
                        String plaque = type + "-" + (100 + id);
                        PlannerVehicle vehicle = new PlannerVehicle(
                            id++,
                            plaque,
                            type,
                            PlannerVehicle.VehicleState.IDLE,
                            (int) grossWeight,
                            25,  // Fixed fuel capacity
                            25,  // Current fuel
                            maxGLP,
                            maxGLP,  // Start with full GLP
                            new Position(0, 0)
                        );
                        vehicles.add(vehicle);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading vehicles file: " + e.getMessage());
        }
        
        return vehicles;
    }

    public List<PlannerBlockage> parseBlockages(String filePath) {
        List<PlannerBlockage> blockages = new ArrayList<>();
        int id = 1;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Split line into time range and coordinates
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    System.err.println("Invalid blockage format: " + line);
                    continue;
                }

                // Parse time range (e.g., 01d06h00m-01d15h00m)
                String[] timeParts = parts[0].split("-");
                if (timeParts.length != 2) {
                    System.err.println("Invalid time range format: " + parts[0]);
                    continue;
                }

                try {
                    // Parse start time
                    String[] startTimeComponents = timeParts[0].split("[dhm]");
                    if (startTimeComponents.length != 3) {
                        System.err.println("Invalid start time format: " + timeParts[0]);
                        continue;
                    }
                    int startDay = Integer.parseInt(startTimeComponents[0]);
                    int startHour = Integer.parseInt(startTimeComponents[1]);
                    int startMinute = Integer.parseInt(startTimeComponents[2]);
                    Time startTime = new Time(0, 1, startDay, startHour, startMinute);

                    // Parse end time
                    String[] endTimeComponents = timeParts[1].split("[dhm]");
                    if (endTimeComponents.length != 3) {
                        System.err.println("Invalid end time format: " + timeParts[1]);
                        continue;
                    }
                    int endDay = Integer.parseInt(endTimeComponents[0]);
                    int endHour = Integer.parseInt(endTimeComponents[1]);
                    int endMinute = Integer.parseInt(endTimeComponents[2]);
                    Time endTime = new Time(0, 1, endDay, endHour, endMinute);

                    // Parse vertices
                    String[] coordinates = parts[1].split(",");
                    if (coordinates.length < 4 || coordinates.length % 2 != 0) {
                        System.err.println("Invalid coordinate format: " + parts[1]);
                        continue;
                    }

                    List<Position> vertices = new ArrayList<>();
                    // Process coordinates in pairs (x,y)
                    for (int i = 0; i < coordinates.length; i += 2) {
                        int x = Integer.parseInt(coordinates[i].trim());
                        int y = Integer.parseInt(coordinates[i + 1].trim());
                        vertices.add(new Position(x, y));
                    }

                    // Create blockage with vertices and times
                    PlannerBlockage blockage = new PlannerBlockage(id++, startTime, endTime, vertices);
                    blockages.add(blockage);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading blockages file: " + e.getMessage());
        }
        
        return blockages;
    }

    public List<PlannerWarehouse> parseWarehouses(String filePath) {
        List<PlannerWarehouse> warehouses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header line
                if (line.contains("Tipo")) continue;

                // Split line by commas
                String[] parts = line.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    System.err.println("Invalid warehouse format: " + line);
                    continue;
                }

                try {
                    // Parse coordinates
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    
                    // Parse GLP capacity
                    int maxGLP = Integer.parseInt(parts[2].trim());
                    
                    // Check if main warehouse
                    boolean isMain = parts.length == 4 && parts[3].equals("main");

                    // Create warehouse
                    PlannerWarehouse warehouse = new PlannerWarehouse(
                        warehouses.size() + 1,  // Use sequential ID
                        new Position(x, y),
                        maxGLP,
                        maxGLP,  // Start with full GLP
                        isMain,
                        false    // wasVehicle
                    );
                    warehouses.add(warehouse);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading warehouses file: " + e.getMessage());
        }
        
        return warehouses;
    }
}
