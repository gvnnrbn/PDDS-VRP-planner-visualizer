package pucp.pdds.backend.algos.scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class DatabaseDataProvider implements DataProvider {
    private final VehiculoRepository vehiculoRepository;
    private final PedidoRepository pedidoRepository;
    private final AlmacenRepository almacenRepository;
    private final BloqueoRepository bloqueoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final Time initialTime;

    @Autowired
    public DatabaseDataProvider(
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
    public List<PlannerOrder> getCurrentOrders(Time time) {
        LocalDateTime start = time.toLocalDateTime().minusHours(2);
        LocalDateTime end = time.toLocalDateTime();
        return pedidoRepository.findByFechaRegistroBetween(start, end).stream().map(PlannerOrder::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerOrder> getOrdersForWeek(Time startDate) {
        LocalDateTime start = startDate.toLocalDateTime();
        LocalDateTime end = start.plusDays(7);
        return pedidoRepository.findByFechaRegistroBetween(start, end).stream().map(PlannerOrder::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerBlockage> getBlockages() {
        return bloqueoRepository.findAll().stream().map(PlannerBlockage::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<PlannerBlockage> getCurrentBlockages(Time time) {
        return bloqueoRepository.findCurrent(time.toLocalDateTime()).stream().map(PlannerBlockage::fromEntity).collect(Collectors.toList());
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

    @Override
    public void refetchData(SchedulerState state, Time startTime) {
        System.out.println("[SHOW] Refetching data");
        LocalDateTime fetchingInterval[] = {
            state.getCurrTime().toLocalDateTime(),
            state.getCurrTime().toLocalDateTime()
        };

        // WORKING FINE
        List<PlannerOrder> newOrders = pedidoRepository.
            findByFechaRegistroBetween(fetchingInterval[0], fetchingInterval[1])
            .stream()
            .map(PlannerOrder::fromEntity)
            .filter(o-> !state.getOrders().stream().anyMatch(o2->o2.id == o.id))
            .collect(Collectors.toList());
        System.out.println("[SHOW] New orders: " + newOrders.size());
        for (PlannerOrder order : newOrders) {
            System.out.println("[SHOW] Order: " + order.id);
        }

        List<PlannerOrder> newAllOrders = new ArrayList<>(state.getOrders());
        newAllOrders.addAll(newOrders);
        state.setOrders(newAllOrders);

        // NOT WORKING
        List<PlannerBlockage> newBlockages = bloqueoRepository.
            findByStartTimeBetween(fetchingInterval[0], fetchingInterval[1])
            .stream()
            .map(PlannerBlockage::fromEntity)
            .filter(b-> !state.getBlockages().stream().anyMatch(b2->b2.id == b.id))
            .collect(Collectors.toList());
        System.out.println("[SHOW] New blockages: " + newBlockages.size());
        for (PlannerBlockage blockage : newBlockages) {
            System.out.println("[SHOW] Blockage: " + blockage.id);
        }

        List<PlannerBlockage> newAllBlockages = new ArrayList<>(state.getBlockages());
        newAllBlockages.addAll(newBlockages);
        state.setBlockages(newAllBlockages);

        // NOT TRIED
        List<PlannerFailure> newFailures = incidenciaRepository.
            findAll()
            .stream().map(PlannerFailure::fromEntity)
            .filter(f-> !state.getFailures().stream().anyMatch(f2->f2.id == f.id))
            .collect(Collectors.toList());

        List<PlannerFailure> newAllFailures = new ArrayList<>(state.getFailures());
        newAllFailures.addAll(newFailures);
        state.setFailures(newAllFailures);
    }
}
