package pucp.pdds.backend.algos.entities;

import java.util.ArrayList;
import java.util.List;

import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.algorithm.OrderDeliverNode;
import pucp.pdds.backend.algos.algorithm.ProductRefillNode;
import pucp.pdds.backend.algos.data.Indicator;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.model.Vehiculo;

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

    public static PlannerVehicle fromEntity(Vehiculo vehiculo) {
        VehicleState state = vehiculo.isDisponible() ? VehicleState.IDLE : VehicleState.REPAIR;
        Position position = new Position(vehiculo.getPosicionX(), vehiculo.getPosicionY());
        
        return new PlannerVehicle(
            vehiculo.getId().intValue(),
            vehiculo.getPlaca(),
            vehiculo.getTipo().name(),
            state,
            vehiculo.getPeso(),
            (int) vehiculo.getMaxCombustible(),
            vehiculo.getCurrCombustible(),
            (int) vehiculo.getMaxGlp(),
            (int) vehiculo.getCurrGlp(),
            position
        );
    }

    // Path advancement method
    public void advancePath(double units, Indicator indicators) {
        if (currentPath == null || currentPath.isEmpty() || currentPath.size() < 2) {
            currentPath = null;
            return;
        }

        if (this.currentFuel <= 0) {
            this.state = VehicleState.STUCK;
            return;
        }

        while (units > 0 && currentPath.size() > 1 && this.currentFuel > 0) {
            Position from = currentPath.get(0);
            Position to = currentPath.get(1);

            double segmentDistance = PathBuilder.calculateDistance(List.of(from, to));
            if (segmentDistance <= 0) {
                currentPath.remove(0);
                continue;
            }

            double fuelCostForSegment = segmentDistance * (this.weight / 1000 + this.currentGLP * 0.5) / 180;
            double fuelPerUnit = fuelCostForSegment / segmentDistance;

            double maxDistWithFuel = (fuelPerUnit > 0) ? this.currentFuel / fuelPerUnit : Double.POSITIVE_INFINITY;
            double distanceToMove = Math.min(units, Math.min(segmentDistance, maxDistWithFuel));

            double fuelUsed = distanceToMove * fuelPerUnit;
            this.currentFuel -= fuelUsed;
            switch (this.type) {
                case "TA":
                    indicators.fuelCounterTA += fuelUsed;
                    break;
                case "TB":
                    indicators.fuelCounterTB += fuelUsed;
                    break;
                case "TC":
                    indicators.fuelCounterTC += fuelUsed;
                    break;
                case "TD":
                    indicators.fuelCounterTD += fuelUsed;
                    break;
                default:
                    break;
            }
            indicators.fuelCounterTotal += fuelUsed;
            units -= distanceToMove;

            if (distanceToMove >= segmentDistance) {
                this.position = to;
                currentPath.remove(0);
            } else {
                double ratio = distanceToMove / segmentDistance;
                double deltaX = (to.x - from.x) * ratio;
                double deltaY = (to.y - from.y) * ratio;
                Position newPosition = new Position(from.x + deltaX, from.y + deltaY);
                this.position = newPosition;
                currentPath.set(0, newPosition);
            }
        }

        if (this.currentFuel <= 0) {
            this.currentFuel = 0;
            this.state = VehicleState.STUCK;
        }
    }

    public void processNode(
            Node node, 
            PlannerVehicle vehicle, 
            List<PlannerOrder> orders, 
            List<PlannerWarehouse> warehouses, 
            Time currentTime,
            boolean shouldLog,
            Indicator indicators
    ) {
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
            if (warehouse.position.x == 12 && warehouse.position.y == 8) {
                indicators.glpFilledMain += refillNode.amountGLP;
            } else if (warehouse.position.x == 42 && warehouse.position.y == 42) {
                indicators.glpFilledNorth += refillNode.amountGLP;
            } else if (warehouse.position.x == 63 && warehouse.position.y == 3) {
                indicators.glpFilledEast += refillNode.amountGLP;
            }
            indicators.glpFilledTotal += refillNode.amountGLP;
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

            if (shouldLog) {
                System.out.println("Order " + order.id + " currently has " + order.amountGLP + " GLP left to deliver");
            }

            if (shouldLog) {
                System.out.println("[REAL] UPDATING ORDER WITH HASH: " + order.hashCode());
            } else {
                System.out.println("[FAST] UPDATING ORDER WITH HASH: " + order.hashCode());
            }

            order.amountGLP -= deliverNode.amountGLP;
            vehicle.currentGLP -= deliverNode.amountGLP;

            if (order.amountGLP == 0) {
                order.deliverTime = currentTime;
                indicators.completedOrders ++;
                double deliveryTotalMinutes = (double) order.releaseTime.minutesUntil(currentTime);
                indicators.deliveryTimes.add(deliveryTotalMinutes);
            }

            if (order.amountGLP < 0) {
                System.out.println("ERROR WHILE UPDATING ORDER WITH HASH: " + order.hashCode());
                throw new RuntimeException("Order " + order.id + " has " + order.amountGLP + " GLP left to deliver");
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
            if (this.currentPath != null) {
                clone.currentPath = new ArrayList<>();
                for (Position p : this.currentPath) {
                    clone.currentPath.add(p.clone());
                }
            } else {
                clone.currentPath = null;
            }
            clone.nextNodeIndex = this.nextNodeIndex;
            clone.currentFailure = this.currentFailure != null ? this.currentFailure.clone() : null;
            clone.currentMaintenance = this.currentMaintenance != null ? this.currentMaintenance.clone() : null;
            clone.initialPosition = this.initialPosition.clone();
            clone.failure = this.failure != null ? this.failure.clone() : null;
            clone.minutesUntilFailure = this.minutesUntilFailure;
            clone.reincorporationTime = this.reincorporationTime != null ? this.reincorporationTime.clone() : null;
            
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
    }

    public boolean isActive(Time currentTime) {
        return this.state == VehicleState.IDLE || this.state == VehicleState.FINISHED || this.state == VehicleState.ONTHEWAY;
    }
}
