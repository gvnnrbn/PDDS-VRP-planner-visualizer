package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Time;

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
