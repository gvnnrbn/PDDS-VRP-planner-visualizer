package entities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.Position;
import utils.Time;

public class PlannerOrder implements Cloneable {
    public int id;
    public Time arrivalTime;
    public Position position;
    public int amountGLP;
    public String clientId;
    public Time deadline;
    public Time deliverTime;
    public Time releaseTime;

    public PlannerOrder(int id, Time arrivalTime, Position position, int amountGLP, String clientId, Time deadline) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.position = position;
        this.amountGLP = amountGLP;
        this.clientId = clientId;
        this.deadline = deadline;
        this.deliverTime = null;
        this.releaseTime = arrivalTime;
    }

    public static List<PlannerOrder> parseOrders(String filePath) {
        List<PlannerOrder> orders = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 1; // Starting ID 
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
    
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
    
                // Parse arrival time (ej: 01d00h01m)
                String[] timeParts = parts[0].split("[dhm]");
                if (timeParts.length < 3) continue;
    
                int day = Integer.parseInt(timeParts[0]);
                int hour = Integer.parseInt(timeParts[1]);
                int minute = Integer.parseInt(timeParts[2]);
    
                Time arrivalTime = new Time(1,1, day, hour, minute);
    
                // Parse order details
                String[] orderParts = parts[1].split(",");
                if (orderParts.length != 5) continue;
    
                int x = Integer.parseInt(orderParts[0].trim());
                int y = Integer.parseInt(orderParts[1].trim());
    
                String clientId = orderParts[2].trim();

                int amountGLP = Integer.parseInt(orderParts[3].replace("m3", "").trim());
    
                int deadlineHours = Integer.parseInt(orderParts[4].replace("h", "").trim());
    
                Time deadline = arrivalTime.addMinutes(deadlineHours * 60);

                PlannerOrder order = new PlannerOrder(id++, arrivalTime, new Position(x, y), amountGLP, clientId, deadline);
                orders.add(order);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return orders;
    }

    public boolean isDelivered() {
        return deliverTime != null;
    }

    public boolean isLate(Time currentTime) {
        return currentTime.isAfter(deadline);
    }

    @Override
    public String toString() {
        return "PlannerOrder{" +
            "id=" + id +
            ", arrivalTime=" + arrivalTime +
            ", position=" + position +
            ", amountGLP=" + amountGLP +
            ", clientId='" + clientId + '\'' +
            ", deadline=" + deadline +
            ", deliverTime=" + deliverTime +
            ", releaseTime=" + releaseTime +
            '}';
    }

    @Override
    public PlannerOrder clone() {
        try {
            PlannerOrder clone = new PlannerOrder(
                this.id,
                this.arrivalTime.clone(),
                this.position.clone(),
                this.amountGLP,
                this.clientId,
                this.deadline.clone()
            );
            clone.releaseTime = this.releaseTime.clone();
            clone.deliverTime = this.deliverTime != null ? this.deliverTime.clone() : null;
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }
}
