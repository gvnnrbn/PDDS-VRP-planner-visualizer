package pucp.pdds.backend.algos.scheduler;

import java.util.List;
import org.springframework.stereotype.Service;

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

@Service
public class FileBasedDataProvider implements DataProvider {
    private final List<PlannerVehicle> vehicles;
    private final List<PlannerOrder> orders;
    private final List<PlannerBlockage> blockages;
    private final List<PlannerWarehouse> warehouses;
    private final List<PlannerFailure> failures;
    private final List<PlannerMaintenance> maintenances;
    private final Time initialTime;

    public FileBasedDataProvider() {
        this.vehicles = CSVDataParser.parseVehicles("data/vehicles.csv");
        this.orders = CSVDataParser.parseOrders("data/orders.csv");
        this.blockages = CSVDataParser.parseBlockages("data/blockages.csv");
        this.warehouses = CSVDataParser.parseWarehouses("data/warehouses.csv");
        this.failures = CSVDataParser.parseFailures("data/failures.csv");
        this.maintenances = CSVDataParser.parseMaintenances("data/maintenances.csv");
        this.initialTime = new Time(2025, 1, 1, 0, 0);

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
    public Time getInitialTime() {
        return initialTime;
    }

    @Override
    public void export(DataChunk dataChunk, int sequence) {
        DataExporter.exportToJson(dataChunk, sequence);
    }
}
