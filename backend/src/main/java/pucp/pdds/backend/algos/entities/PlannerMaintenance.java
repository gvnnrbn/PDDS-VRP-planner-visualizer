package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.model.Mantenimiento;

public class PlannerMaintenance implements Cloneable {
    public int id;
    public String vehiclePlaque;
    public Time startDate;
    public Time endDate; 

    public PlannerMaintenance(int id, String vehiclePlaque, Time startDate, Time endDate) {
        this.id = id;
        this.vehiclePlaque = vehiclePlaque;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static PlannerMaintenance fromEntity(Mantenimiento mantenimiento) {
        Time startDate = new Time(
            mantenimiento.getStartTime().getYear(),
            mantenimiento.getStartTime().getMonthValue(),
            mantenimiento.getStartTime().getDayOfMonth(),
            mantenimiento.getStartTime().getHour(),
            mantenimiento.getStartTime().getMinute()
        );
        
        Time endDate = new Time(
            mantenimiento.getEndTime().getYear(),
            mantenimiento.getEndTime().getMonthValue(),
            mantenimiento.getEndTime().getDayOfMonth(),
            mantenimiento.getEndTime().getHour(),
            mantenimiento.getEndTime().getMinute()
        );
        
        String vehiclePlaque = mantenimiento.getVehiculo() != null ? 
            mantenimiento.getVehiculo().getPlaca() : "UNKNOWN";
        
        return new PlannerMaintenance(
            mantenimiento.getId().intValue(),
            vehiclePlaque,
            startDate,
            endDate
        );
    }

    public boolean isActive(Time currentTime) {
        return currentTime.isAfterOrAt(startDate) && currentTime.isBefore(endDate);
    }

    @Override
    public String toString() {
        return "PlannerMaintenance{" +
            "id=" + id +
            ", vehiclePlaque='" + vehiclePlaque + '\'' +
            ", startDate=" + startDate +
            ", endDate=" + endDate +
            '}';
    }

    @Override
    public PlannerMaintenance clone() {
        try {
            PlannerMaintenance clone = new PlannerMaintenance(
                this.id,
                this.vehiclePlaque,
                this.startDate.clone(),
                this.endDate.clone()
            );
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }
}
