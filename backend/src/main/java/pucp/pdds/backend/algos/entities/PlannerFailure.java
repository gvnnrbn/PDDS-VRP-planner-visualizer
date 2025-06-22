package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.model.Incidencia;

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

    public static PlannerFailure fromEntity(Incidencia incidencia) {
        // Map Turno to Shift
        Shift shift = Shift.valueOf(incidencia.getTurno().name());
        
        // Create a default time for the failure (start of the shift)
        Time timeOccuredOn = new Time(
            incidencia.getFecha().getYear(),
            incidencia.getFecha().getMonthValue(),
            incidencia.getFecha().getDayOfMonth(),
            shift == Shift.T1 ? 0 : shift == Shift.T2 ? 8 : 16,
            0
        );
        
        // Determine failure type based on shift (this is a simplification)
        FailureType type = FailureType.Ti1; // Default to Ti1
        
        String vehiclePlaque = incidencia.getVehiculo() != null ? 
            incidencia.getVehiculo().getPlaca() : "UNKNOWN";
        
        return new PlannerFailure(
            incidencia.getId().intValue(),
            type,
            shift,
            vehiclePlaque,
            timeOccuredOn
        );
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
                this.timeOccuredOn != null ? this.timeOccuredOn.clone() : null
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