package pucp.pdds.backend.algos.scheduler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.repository.*;

@Service
public class FileBasedDataProvider implements DataProvider {
    private final List<PlannerVehicle> vehicles;
    private final List<PlannerOrder> orders;
    private final List<PlannerBlockage> blockages;
    private final List<PlannerWarehouse> warehouses;
    private final List<PlannerFailure> failures;
    private final List<PlannerMaintenance> maintenances;
    private final Time initialTime;

    @Autowired
    public FileBasedDataProvider(
        VehiculoRepository vehiculoRepository,
        PedidoRepository pedidoRepository,
        AlmacenRepository almacenRepository,
        BloqueoRepository bloqueoRepository,
        IncidenciaRepository incidenciaRepository,
        MantenimientoRepository mantenimientoRepository
    ) {
        this.vehicles = vehiculoRepository.findAll().stream().map(PlannerVehicle::fromEntity).collect(Collectors.toList());
        this.orders = pedidoRepository.findAll().stream().map(PlannerOrder::fromEntity).collect(Collectors.toList());
        this.blockages = bloqueoRepository.findAll().stream().map(PlannerBlockage::fromEntity).collect(Collectors.toList());
        this.warehouses = almacenRepository.findAll().stream().map(PlannerWarehouse::fromEntity).collect(Collectors.toList());
        this.failures = incidenciaRepository.findAll().stream().map(PlannerFailure::fromEntity).collect(Collectors.toList());
        this.maintenances = mantenimientoRepository.findAll().stream().map(PlannerMaintenance::fromEntity).collect(Collectors.toList());
        this.initialTime = new Time(2025, 1, 1, 0, 0);
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
}
