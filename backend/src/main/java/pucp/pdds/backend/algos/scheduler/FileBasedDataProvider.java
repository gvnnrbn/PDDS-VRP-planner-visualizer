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
    private final VehiculoRepository vehiculoRepository;
    private final PedidoRepository pedidoRepository;
    private final AlmacenRepository almacenRepository;
    private final BloqueoRepository bloqueoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final MantenimientoRepository mantenimientoRepository;
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
        this.vehiculoRepository = vehiculoRepository;
        this.pedidoRepository = pedidoRepository;
        this.almacenRepository = almacenRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.initialTime = new Time(2025, 1, 1, 0, 0);
    }

    @Override
    public List<PlannerVehicle> getVehicles() {
        return vehiculoRepository.findAll().stream().map(PlannerVehicle::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerOrder> getOrders() {
        return pedidoRepository.findAll().stream().map(PlannerOrder::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerBlockage> getBlockages() {
        return bloqueoRepository.findAll().stream().map(PlannerBlockage::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerWarehouse> getWarehouses() {
        return almacenRepository.findAll().stream().map(PlannerWarehouse::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerFailure> getFailures() {
        return incidenciaRepository.findAll().stream().map(PlannerFailure::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerMaintenance> getMaintenances() {
        return mantenimientoRepository.findAll().stream().map(PlannerMaintenance::fromEntity).collect(Collectors.toList());
    }

    @Override
    public Time getInitialTime() {
        return initialTime;
    }
}
