package pucp.pdds.backend.algos.utils;

import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure.FailureType;
import pucp.pdds.backend.algos.entities.PlannerFailure.Shift;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.List;

public class CSVDataParser {
    private static int currentYear = 2025; 
    private static int currentMonth = 1;

    public static List<PlannerOrder> parseOrders(String filePath) {
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
                    Time creationTime = new Time(currentYear, currentMonth, day, hour, minute);
    
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
    

    public static List<PlannerVehicle> parseVehicles(String filePath) {
        List<PlannerVehicle> vehicles = new ArrayList<>();
        int id = 1; // Single ID counter for all vehicle types
        
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
                        // Generate plaque using type and sequential number, adding leading zero only for single digits
                        String plaque = type + (i + 1 < 10 ? "0" + (i + 1) : String.valueOf(i + 1));
                        PlannerVehicle vehicle = new PlannerVehicle(
                            id++,
                            plaque,
                            type,
                            PlannerVehicle.VehicleState.IDLE,
                            (int) grossWeight,
                            25,  // Increased fuel capacity to 25 gallons
                            25,  // Current fuel to 25 gallons
                            maxGLP,
                            maxGLP,  // Start with empty GLP to force refilling
                            new Position(12, 8)  // Start at main warehouse position
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

    public static List<PlannerBlockage> parseBlockages(String filePath) {
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
                    Time startTime = new Time(currentYear, currentMonth, startDay, startHour, startMinute);

                    // Parse end time
                    String[] endTimeComponents = timeParts[1].split("[dhm]");
                    if (endTimeComponents.length != 3) {
                        System.err.println("Invalid end time format: " + timeParts[1]);
                        continue;
                    }
                    int endDay = Integer.parseInt(endTimeComponents[0]);
                    int endHour = Integer.parseInt(endTimeComponents[1]);
                    int endMinute = Integer.parseInt(endTimeComponents[2]);
                    Time endTime = new Time(currentYear, currentMonth, endDay, endHour, endMinute);

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

    public static List<PlannerWarehouse> parseWarehouses(String filePath) {
        List<PlannerWarehouse> warehouses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Use explicit ID counter instead of list size
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header line
                if (line.contains("Tipo")) continue;

                // Split line by commas
                String[] parts = line.split(",");
                if (parts.length != 3 && parts.length != 4) {
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
                    boolean isMain = parts.length == 4 && parts[3].trim().equalsIgnoreCase("main");

                    // Validate coordinates
                    if (x < 0 || y < 0) {
                        System.err.println("Invalid coordinates for warehouse " + id + ": (" + x + "," + y + ")");
                        continue;
                    }

                    // Validate GLP capacity
                    if (maxGLP <= 0) {
                        System.err.println("Invalid GLP capacity for warehouse " + id + ": " + maxGLP);
                        continue;
                    }

                    // Create warehouse
                    PlannerWarehouse warehouse = new PlannerWarehouse(
                        id++,  // Use explicit ID counter
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

            // Validate that there is exactly one main warehouse
            long mainWarehouseCount = warehouses.stream().filter(w -> w.isMain).count();
            if (mainWarehouseCount != 1) {
                System.err.println("Warning: Found " + mainWarehouseCount + " main warehouses, expected exactly 1");
            }

        } catch (IOException e) {
            System.err.println("Error reading warehouses file: " + e.getMessage());
        }
        
        return warehouses;
    }

    public static List<PlannerFailure> parseFailures(String filePath) {
        List<PlannerFailure> failures = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID for failures
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("_");
                if (parts.length != 3) {
                    System.err.println("Invalid failure format: " + line);
                    continue;
                }

                try {
                    // Parse shift (T1=00:00-08:00, T2=08:00-16:00, T3=16:00-24:00)
                    String shiftStr = parts[0].trim();
                    if (!shiftStr.startsWith("T")) {
                        System.err.println("Invalid shift format: " + shiftStr);
                        continue;
                    }
                    int shift = Integer.parseInt(shiftStr.substring(1));
                    Shift shiftOccurredOn = switch (shift) {
                        case 1 -> Shift.T1;
                        case 2 -> Shift.T2;
                        case 3 -> Shift.T3;
                        default -> throw new IllegalArgumentException("Invalid shift: " + shift);
                    };

                    // Parse vehicle plaque
                    String vehiclePlaque = parts[1].trim();

                    // Parse failure type (TI1, TI2, TI3)
                    String typeStr = parts[2].trim();
                    if (!typeStr.startsWith("TI")) {
                        System.err.println("Invalid failure type format: " + typeStr);
                        continue;
                    }
                    int typeInt = Integer.parseInt(typeStr.substring(2));
                    FailureType type = switch (typeInt) {
                        case 1 -> FailureType.Ti1;
                        case 2 -> FailureType.Ti2;
                        case 3 -> FailureType.Ti3;
                        default -> throw new IllegalArgumentException("Invalid failure type: " + typeInt);
                    };

                    // Create failure with null times (they will be set when registered)
                    PlannerFailure failure = new PlannerFailure(
                        id++,
                        type,
                        shiftOccurredOn,
                        vehiclePlaque,
                        null
                    );
                    failures.add(failure);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid failure format: " + line + " - " + e.getMessage());
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading failures file: " + e.getMessage());
        }
        
        return failures;
    }

    public static List<PlannerMaintenance> parseMaintenances(String filePath) {
        List<PlannerMaintenance> maintenances = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    System.err.println("Invalid maintenance format in line: " + line);
                    continue;
                }
                
                try {
                    int startDate = Integer.parseInt(parts[0].trim());
                    String vehiclePlaque = parts[1].trim();
                    // 20250503:TD06
                    Time start = new Time(startDate/10000, startDate%10000/100, startDate%100, 0, 0);
                    Time end = start.addTime(new Time(0, 0, 0, 23, 59));
                    PlannerMaintenance maintenance = new PlannerMaintenance(id++, vehiclePlaque, start, end);
                    maintenances.add(maintenance);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in line: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading maintenances file: " + e.getMessage());
        }
        
        return maintenances;
    }
}
