package entities;

import java.util.ArrayList;
import java.util.List;

import algorithm.Node;
import algorithm.OrderDeliverNode;
import algorithm.ProductRefillNode;
import utils.Position;
import utils.SimulationProperties;
import utils.Time;
import utils.PathBuilder;

public class PlannerVehicle implements Cloneable {
    // Basic vehicle attributes
    public int id;
    public String plaque;
    public String type;
    public VehicleState state;
    public PlannerFailure currentFailure;
    public PlannerMaintenance currentMaintenance;
    public int waitTransition;
    public double weight;
    public int maxFuel;
    public double currentFuel;
    public int maxGLP;
    public int currentGLP;
    public Position position;
    public Position initialPosition;
    public PlannerFailure failure;
    public int minutesUntilFailure;
    public Time reincorporationTime;
    
    // Path and node information
    public List<Position> currentPath;
    public int nextNodeIndex;

    public PlannerVehicle(int id, String plaque, String type, VehicleState state, 
                         double weight, int maxFuel, double currentFuel, 
                         int maxGLP, int currentGLP, Position position) {
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
        this.initialPosition = position;
        this.waitTransition = 0;
        this.currentPath = null;
        this.nextNodeIndex = 1;
    }

    // Path advancement method
    public void advancePath(double units) {
        if (currentPath == null || currentPath.isEmpty() || currentPath.size() < 2) {
            currentPath = null;
            return;
        }

        Position from = currentPath.get(0);
        Position to = currentPath.get(1);

        while (units > 0 && currentPath.size() > 1) {
            double distance = PathBuilder.calculateDistance(List.of(from, to));
            if (distance > units) {
                // Move 'from' position the corresponding amount of units
                double ratio = units / distance;
                double deltaX = (to.x - from.x) * ratio;
                double deltaY = (to.y - from.y) * ratio;
                Position newPosition = new Position(from.x + deltaX, from.y + deltaY);
                currentPath.set(0, newPosition);
                this.position = newPosition;  // Update vehicle position
                units = 0;
            } else if (distance <= units) {
                // Remove 'from' position from path
                currentPath.remove(0);
                this.position = to;  // Update vehicle position
                units -= distance;
                
                // Update from and to for next iteration
                if (currentPath.size() > 1) {
                    from = currentPath.get(0);
                    to = currentPath.get(1);
                }
            }
        }
    }

    public void processNode(Node node, PlannerVehicle vehicle, List<PlannerOrder> orders, List<PlannerWarehouse> warehouses, Time currentTime) {
        if (node instanceof ProductRefillNode) {
            ProductRefillNode refillNode = (ProductRefillNode) node;
            PlannerWarehouse warehouse = warehouses.stream()
                .filter(w -> w.id == refillNode.warehouse.id)
                .findFirst()
                .orElse(null);
            if (warehouse == null) {
                throw new RuntimeException("Warehouse with id " + refillNode.warehouse.id + " not found");
            }

            warehouse.currentGLP -= refillNode.amountGLP;
            vehicle.currentGLP += refillNode.amountGLP;
            vehicle.currentFuel = vehicle.maxFuel;
            this.waitTransition = SimulationProperties.timeAfterRefill;
        }
        if (node instanceof OrderDeliverNode) {
            OrderDeliverNode deliverNode = (OrderDeliverNode) node;
            PlannerOrder order = orders.stream()
                .filter(o -> o.id == deliverNode.order.id)
                .findFirst()
                .orElse(null);
            if (order == null) {
                throw new RuntimeException("Order with id " + deliverNode.order.id + " not found");
            }

            order.amountGLP -= deliverNode.amountGLP;
            vehicle.currentGLP += deliverNode.amountGLP;

            if (order.amountGLP == 0) {
                order.deliverTime = currentTime;
            }

            this.waitTransition = SimulationProperties.timeAfterDelivery;
        }
    }

    @Override
    public String toString() {
        return "PlannerVehicle{" +
            "id=" + id +
            ", plaque='" + plaque + "'" +
            ", type='" + type + "'" +
            ", state=" + state +
            ", currentFailure=" + (currentFailure != null ? currentFailure.toString() : "null") +
            ", currentMaintenance=" + (currentMaintenance != null ? currentMaintenance.toString() : "null") +
            ", waitTransition=" + waitTransition +
            ", weight=" + weight + "kg" +
            ", maxFuel=" + maxFuel + "L" +
            ", currentFuel=" + currentFuel + "L" +
            ", maxGLP=" + maxGLP + "m3" +
            ", currentGLP=" + currentGLP + "m3" +
            ", position=" + position.toString() +
            ", initialPosition=" + initialPosition.toString() +
            ", currentPath=" + (currentPath != null ? currentPath.size() + " positions" : "null") +
            ", nextNodeIndex=" + nextNodeIndex +
            '}';
    }

    @Override
    public PlannerVehicle clone() {
        try {
            PlannerVehicle clone = new PlannerVehicle(
                this.id,
                this.plaque,
                this.type,
                this.state,
                this.weight,
                this.maxFuel,
                this.currentFuel,
                this.maxGLP,
                this.currentGLP,
                this.position.clone()
            );
            
            // Clone mutable fields
            clone.waitTransition = this.waitTransition;
            clone.currentPath = this.currentPath != null ? new ArrayList<>(this.currentPath) : null;
            clone.nextNodeIndex = this.nextNodeIndex;
            clone.currentFailure = this.currentFailure != null ? this.currentFailure.clone() : null;
            clone.currentMaintenance = this.currentMaintenance != null ? this.currentMaintenance.clone() : null;
            clone.initialPosition = this.initialPosition.clone();
            
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }

    public enum VehicleState {
        IDLE("no routes planned, AVAILABLE"),
        ONTHEWAY("has routes planned, AVAILABLE"),
        STUCK("failure occurred, AVAILABLE AS WAREHOUSE"),
        RETURNING_TO_BASE("heading to base, NOT AVAILABLE"),
        REPAIR("inside main warehouse, NOT AVAILABLE until x shift according to failure type"),
        MAINTENANCE("heading to or in maintenance, NOT AVAILABLE until 23:59"),
        FINISHED("finished route for this planning interval");

        private final String description;

        VehicleState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name() + "{" + description + "}";
        }
    }

    public boolean isActive(Time currentTime) {
        return this.state == VehicleState.IDLE || this.state == VehicleState.FINISHED || this.state == VehicleState.ONTHEWAY;
    }
}
