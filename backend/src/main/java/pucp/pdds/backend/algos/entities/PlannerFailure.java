package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Time;

public class PlannerFailure implements Cloneable{
    public int id;
    public FailureType type;
    public Shift shiftOccurredOn;
    public String vehiclePlaque;

    public Time timeOccuredOn;

    public PlannerFailure(int id, FailureType type, Shift shiftOccurredOn, String vehiclePlaque, Time timeOccuredOn) {
        this.id = id;
        this.vehiclePlaque = vehiclePlaque;
        this.type = type;
        this.shiftOccurredOn = shiftOccurredOn;
        this.timeOccuredOn = timeOccuredOn;
    }

    public boolean hasBeenAssigned() {
        return timeOccuredOn != null;
    }

    @Override
    public String toString() {
        return "PlannerFailure{" +
            "id=" + id +
            ", type=" + type +
            ", shiftOccurredOn=" + shiftOccurredOn +
            ", vehiclePlaque='" + vehiclePlaque + '\'' +
            '}';
    }

    @Override
    public PlannerFailure clone() {
        try {
            PlannerFailure clone = new PlannerFailure(
                this.id,
                this.type,
                this.shiftOccurredOn,
                this.vehiclePlaque,
                this.timeOccuredOn
            );
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }

    public enum FailureType {
        Ti1,
        Ti2,
        Ti3;

        public int getMinutesStuck() {
            switch (this) {
                case Ti1:
                    return 120;
                case Ti2:
                    return 120;
                case Ti3:
                    return 240;
                default:
                    return 0;
            }
        }
    }

    public enum Shift {
        T1, // 00:00-08:00
        T2, // 08:00-16:00
        T3; // 16:00-24:00
    }
}