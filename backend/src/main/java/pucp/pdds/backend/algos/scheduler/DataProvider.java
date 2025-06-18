package pucp.pdds.backend.algos.scheduler;

import java.util.List;
import org.springframework.stereotype.Component;

import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.utils.Time;

@Component
public interface DataProvider {
    List<PlannerVehicle> getVehicles();
    List<PlannerOrder> getOrders();
    List<PlannerBlockage> getBlockages();
    List<PlannerWarehouse> getWarehouses();
    List<PlannerFailure> getFailures();
    List<PlannerMaintenance> getMaintenances();
    Time getInitialTime();
    void export(DataChunk dataChunk, int sequence);
}
