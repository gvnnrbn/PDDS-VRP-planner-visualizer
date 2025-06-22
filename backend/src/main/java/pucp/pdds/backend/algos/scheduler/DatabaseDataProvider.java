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
    public List<PlannerOrder> getOrdersForWeek(java.time.LocalDateTime startDate) {
        java.time.LocalDateTime endDate = startDate.plusDays(7);
        return pedidoRepository.findByFechaRegistroBetween(startDate, endDate).stream().map(PlannerOrder::fromEntity).collect(Collectors.toList());
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

    @Override
    public void refetchData(SchedulerState state) {
        // Fetch new orders
        var newOrders = pedidoRepository.findAll()
            .stream()
            .filter(order -> state.getOrders().stream().noneMatch(o -> o.id == order.getId()))
            .map(PlannerOrder::fromEntity)
            .toList();
        if (!newOrders.isEmpty()) {
            state.getOrders().addAll(newOrders);
        }

        // Fetch new blockages
        var newBlockages = bloqueoRepository.findAll()
            .stream()
            .filter(blockage -> state.getBlockages().stream().noneMatch(b -> b.id == blockage.getId()))
            .map(PlannerBlockage::fromEntity)
            .toList();
        if (!newBlockages.isEmpty()) {
            state.getBlockages().addAll(newBlockages);
        }

        // Fetch new failures
        var newFailures = incidenciaRepository.findAll()
            .stream()
            .filter(failure -> state.getFailures().stream().noneMatch(f -> f.id == failure.getId()))
            .map(PlannerFailure::fromEntity)
            .toList();
        if (!newFailures.isEmpty()) {
            state.getFailures().addAll(newFailures);
        }

        // Fetch new maintenances
        var newMaintenances = mantenimientoRepository.findAll()
            .stream()
            .filter(maintenance -> state.getMaintenances().stream().noneMatch(m -> m.id == maintenance.getId()))
            .map(PlannerMaintenance::fromEntity)
            .toList();
        if (!newMaintenances.isEmpty()) {
            state.getMaintenances().addAll(newMaintenances);
        }

        // Fetch new warehouses
        var newWarehouses = almacenRepository.findAll()
            .stream()
            .filter(warehouse -> state.getWarehouses().stream().noneMatch(w -> w.id == warehouse.getId()))
            .map(PlannerWarehouse::fromEntity)
            .toList();
        if (!newWarehouses.isEmpty()) {
            state.getWarehouses().addAll(newWarehouses);
        }
    }

    @Override
    public void pushChanges(SchedulerState state) {
        // Update vehicles
        state.getVehicles().forEach(plannerVehicle -> {
            vehiculoRepository.findById(Long.valueOf(plannerVehicle.id)).ifPresent(vehicleModel -> {
                vehicleModel.setPosicionY((float)plannerVehicle.position.y);
                vehicleModel.setPosicionX((float)plannerVehicle.position.x);
                vehicleModel.setCurrCombustible((float)plannerVehicle.currentFuel);
                vehicleModel.setCurrGlp((float)plannerVehicle.currentGLP);
                vehiculoRepository.save(vehicleModel);
            });
        });

        // Update orders
        state.getOrders().forEach(plannerOrder -> {
            pedidoRepository.findById(Long.valueOf(plannerOrder.id)).ifPresent(orderModel -> {
                orderModel.setCantidadGLP(plannerOrder.amountGLP);
                if (plannerOrder.deliverTime != null) {
                    orderModel.setFechaEntrega(plannerOrder.deliverTime.toLocalDateTime());
                }
                pedidoRepository.save(orderModel);
            });
        });

        // Update failures
        state.getFailures().forEach(plannerFailure -> {
            incidenciaRepository.findById(Long.valueOf(plannerFailure.id)).ifPresent(failureModel -> {
                if (plannerFailure.timeOccuredOn != null) {
                    failureModel.setOcurrido(true);
                    incidenciaRepository.save(failureModel);
                }
            });
        });
    }
}
