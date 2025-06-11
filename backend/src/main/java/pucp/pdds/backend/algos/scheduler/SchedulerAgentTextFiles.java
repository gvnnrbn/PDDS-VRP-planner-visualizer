package pucp.pdds.backend.algos.scheduler;

import java.util.List;

import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.utils.CSVDataParser;
import pucp.pdds.backend.algos.utils.DataExporter;
import pucp.pdds.backend.algos.utils.Time;

public class SchedulerAgentTextFiles implements SchedulerAgent {
    private List<PlannerVehicle> vehicles;
    private List<PlannerOrder> orders;
    private List<PlannerBlockage> blockages;
    private List<PlannerWarehouse> warehouses;
    private List<PlannerFailure> failures;
    private List<PlannerMaintenance> maintenances;

    public SchedulerAgentTextFiles() {
        this.vehicles = CSVDataParser.parseVehicles("data/vehicles.csv");
        this.orders = CSVDataParser.parseOrders("data/orders.csv");
        this.blockages = CSVDataParser.parseBlockages("data/blockages.csv");
        this.warehouses = CSVDataParser.parseWarehouses("data/warehouses.csv");
        this.failures = CSVDataParser.parseFailures("data/failures.csv");
        this.maintenances = CSVDataParser.parseMaintenances("data/maintenances.csv");

        DataExporter.clearSimulationData();
    }

    @Override
    public List<PlannerVehicle> getVehicles() {
        return vehicles;
    }

    @Override
    public List<PlannerOrder> getOrders() {
        return orders;
    }

    @Override
    public List<PlannerBlockage> getBlockages() {
        return blockages;
    }

    @Override
    public List<PlannerWarehouse> getWarehouses() {
        return warehouses;
    }

    @Override
    public List<PlannerFailure> getFailures() {
        return failures;
    }

    @Override
    public List<PlannerMaintenance> getMaintenances() {
        return maintenances;
    }

    @Override
    public void export(DataChunk dataChunk, int sequence) {
        DataExporter.exportToJson(dataChunk, sequence);
    }
}
