package pucp.pdds.backend.algos.scheduler;

import java.util.List;

import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;

public interface SchedulerAgent {
    public List<PlannerVehicle> getVehicles();
    public List<PlannerOrder> getOrders();
    public List<PlannerBlockage> getBlockages();
    public List<PlannerWarehouse> getWarehouses();
    public List<PlannerFailure> getFailures();
    public List<PlannerMaintenance> getMaintenances();
    public void export(DataChunk dataChunk, int sequence);
}
