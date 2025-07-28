package pucp.pdds.backend.dto;

import pucp.pdds.backend.algos.entities.PlannerFailure.FailureType;
import pucp.pdds.backend.algos.entities.PlannerFailure.Shift;

public class UpdateFailuresMessage {
    private FailureType type;
    private String vehiclePlaque;
    private Shift shiftOccurredOn;
    
    // Default constructor
    public UpdateFailuresMessage() {}
    
    public UpdateFailuresMessage(FailureType type, String vehiclePlaque, Shift shiftOccurredOn) {
        this.type = type;
        this.vehiclePlaque = vehiclePlaque;
        this.shiftOccurredOn = shiftOccurredOn;
    }
    
    public FailureType getType() {
        return type;
    }
    
    public void setType(FailureType type) {
        this.type = type;
    }
    
    public String getVehiclePlaque() {
        return vehiclePlaque;
    }
    
    public void setVehiclePlaque(String vehiclePlaque) {
        this.vehiclePlaque = vehiclePlaque;
    }
    
    public Shift getShiftOccurredOn() {
        return shiftOccurredOn;
    }
    
    public void setShiftOccurredOn(Shift shiftOccurredOn) {
        this.shiftOccurredOn = shiftOccurredOn;
    }
    
    @Override
    public String toString() {
        return "UpdateFailuresMessage{" +
            "type=" + type +
            ", vehiclePlaque='" + vehiclePlaque + '\'' +
            ", shiftOccurredOn=" + shiftOccurredOn +
            '}';
    }
}