package entities;

import utils.Time;

public class PlannerFailure implements Cloneable{
    public int id;
    public FailureType type;
    public Shift shiftOccurredOn;
    public String vehiclePlaque;
    public Time timeTillRepaired;
    public int minutesStuck;
    public Time endStuckTime;
    public Time endRepairTime;

    public PlannerFailure(int id, FailureType type, Shift shiftOccurredOn, String vehiclePlaque, Time endStuckTime, Time endRepairTime) {
        this.id = id;
        this.vehiclePlaque = vehiclePlaque;
        this.type = type;
        this.shiftOccurredOn = shiftOccurredOn;
        this.endStuckTime = endStuckTime;
        this.endRepairTime = endRepairTime;
        // set idle time (time as warehouse) and time till available for scheduling 
        switch (type) {
            case Ti1:
                this.minutesStuck = 120;
                this.timeTillRepaired = new Time(0,0, 0, 0, 0);
                break;
            case Ti2:
                this.minutesStuck = 120;
                switch (shiftOccurredOn) {
                    case T1 -> this.timeTillRepaired = new Time(0,0, 0, 16, 0);
                    case T2 -> this.timeTillRepaired = new Time(0,0, 1, 0, 0);
                    case T3 -> this.timeTillRepaired = new Time(0,0, 1, 8, 0);
                    default -> this.timeTillRepaired = new Time(0,0, 0, 0, 0);
                }
                break;
            case Ti3:
                this.minutesStuck = 240;
                this.timeTillRepaired = new Time(0,0, 2, 0, 0);
                break;
            default:
                break;
        }
    }

    public PlannerFailure register(Time currTime, int minutesToMainWarehouse) {
        Time endStuckTime = currTime.addMinutes(this.minutesStuck); // until this time is available as WAREHOUSE
        Time endRepairTime = endStuckTime.addMinutes(minutesToMainWarehouse).addTime(this.timeTillRepaired);
        PlannerFailure copy = new PlannerFailure(
            this.id,
            this.type,
            this.shiftOccurredOn,
            this.vehiclePlaque,
            endStuckTime,
            endRepairTime
        );
        return copy;
    }

    @Override
    public String toString() {
        return "PlannerFailure{" +
            "id=" + id +
            ", type=" + type +
            ", shiftOccurredOn=" + shiftOccurredOn +
            ", vehiclePlaque='" + vehiclePlaque + '\'' +
            ", timeTillRepaired=" + timeTillRepaired +
            ", minutesStuck=" + minutesStuck +
            ", endStuckTime=" + endStuckTime +
            ", endRepairTime=" + endRepairTime +
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
                this.endStuckTime != null ? this.endStuckTime.clone() : null,
                this.endRepairTime != null ? this.endRepairTime.clone() : null
            );
            clone.minutesStuck = this.minutesStuck;
            clone.timeTillRepaired = this.timeTillRepaired != null ? this.timeTillRepaired.clone() : null;
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }

    public enum FailureType {
        Ti1,
        Ti2,
        Ti3;
    }

    public enum Shift {
        T1, // 00:00-08:00
        T2, // 08:00-16:00
        T3; // 16:00-24:00
    }
}