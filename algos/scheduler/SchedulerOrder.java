package scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import domain.Position;
import utils.Time;

public class SchedulerOrder {
    public int id;
    public Time arrivalTime;
    public Position position;
    public int amountGLP;
    public String clientId;
    public Time deadline;
    
    
    public SchedulerOrder(int id, Time arrivalTime, Position position, int amountGLP, String clientId, Time deadline) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.position = position;
        this.amountGLP = amountGLP;
        this.clientId = clientId;
        this.deadline = deadline;
    }

    public static List<SchedulerOrder> parseOrders(String filePath) {
        List<SchedulerOrder> orders = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
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
    
                Time creationTime = new Time(0,0, day, hour, minute);  // Suponemos mes = 1
    
                // Parsear datos del pedido
                String[] orderParts = parts[1].split(",");
                if (orderParts.length != 5) continue;
    
                int x = Integer.parseInt(orderParts[0].trim());
                int y = Integer.parseInt(orderParts[1].trim());
    
                String clientId = orderParts[2].trim();

                int amountGLP = Integer.parseInt(orderParts[3].replace("m3", "").trim());
    
                int deadlineHours = Integer.parseInt(orderParts[4].replace("h", "").trim());
    
                Time deadline = creationTime.addMinutes(deadlineHours * 60);

                SchedulerOrder order = new SchedulerOrder(id++, creationTime,new Position(x, y), amountGLP, clientId, deadline);
                orders.add(order);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return orders;
    }
}
