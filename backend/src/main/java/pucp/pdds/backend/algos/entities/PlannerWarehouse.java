package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Position;

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

    @Override
    public String toString() {
        return "PlannerWarehouse{" +
            "id=" + id +
            ", position=" + position.toString() +
            ", maxGLP=" + maxGLP + "m3" +
            ", currentGLP=" + currentGLP + "m3" +
            ", isMain=" + isMain +
            ", wasVehicle=" + wasVehicle +
            ", isEmpty=" + (currentGLP == 0) +
            ", isFull=" + (currentGLP == maxGLP) +
            ", capacityPercentage=" + String.format("%.2f", ((double) currentGLP / maxGLP) * 100) + "%" +
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
