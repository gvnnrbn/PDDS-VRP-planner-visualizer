package scheduler;

import java.util.List;

import data.DataChunk;
import entities.PlannerBlockage;
import entities.PlannerOrder;
import entities.PlannerVehicle;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import utils.Time;

public interface SchedulerAgent {
    public List<PlannerVehicle> getVehicles();
    public List<PlannerOrder> getOrders();
    public List<PlannerBlockage> getBlockages();
    public List<PlannerWarehouse> getWarehouses();
    public List<PlannerFailure> getFailures();
    public List<PlannerMaintenance> getMaintenances();
    public void export(DataChunk dataChunk, Time currentTime);
}
