package entities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import algorithm.Node;
import utils.Position;
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
    
    // Path and node information
    public List<Position> currentPath;
    public Node currentNode;

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
        this.currentNode = null;
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
                double deltaX = to.x - from.x;
                double deltaY = to.y - from.y;
                currentPath.set(0, new Position(from.x + deltaX, from.y + deltaY));
                units = 0;
            } else if (distance <= units) {
                // Remove 'from' position from path
                currentPath.remove(0);
                units -= distance;
            }
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
            ", currentNode=" + (currentNode != null ? currentNode.toString() : "null") +
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
            clone.currentNode = this.currentNode != null ? this.currentNode.clone() : null;
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
        REPAIR("inside main warehouse, NOT AVAILABLE until x shift according to failure type"),
        MAINTENANCE("heading to or in maintenance, NOT AVAILABLE until 23:59");

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
        return state != VehicleState.MAINTENANCE && state != VehicleState.REPAIR && state != VehicleState.STUCK;
    }
}
