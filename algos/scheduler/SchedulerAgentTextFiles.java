package scheduler;

import java.util.List;

import data.DataChunk;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import utils.CSVDataParser;
import utils.DataExporter;
import utils.Time;

public class SchedulerAgentTextFiles implements SchedulerAgent {
    private List<PlannerVehicle> vehicles;
    private List<PlannerOrder> orders;
    private List<PlannerBlockage> blockages;
    private List<PlannerWarehouse> warehouses;
    private List<PlannerFailure> failures;
    private List<PlannerMaintenance> maintenances;

    public SchedulerAgentTextFiles() {
        this.vehicles = CSVDataParser.parseVehicles("main/vehicles.csv");
        this.orders = CSVDataParser.parseOrders("main/orders.csv");
        this.blockages = CSVDataParser.parseBlockages("main/blockages.csv");
        this.warehouses = CSVDataParser.parseWarehouses("main/warehouses.csv");
        this.failures = CSVDataParser.parseFailures("main/failures.csv");
        this.maintenances = CSVDataParser.parseMaintenances("main/maintenances.csv");

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
    public void export(DataChunk dataChunk, Time currentTime) {
        DataExporter.exportToJson(dataChunk, currentTime);
    }
}
