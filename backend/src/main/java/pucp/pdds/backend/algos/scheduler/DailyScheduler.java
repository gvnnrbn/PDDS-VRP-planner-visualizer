package pucp.pdds.backend.algos.scheduler;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class DailyScheduler implements Runnable {
    private SchedulerState state;
    private final Lock stateLock = new ReentrantLock();
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean isRunning;
    private static final DateTimeFormatter SIM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DataProvider dataProvider;

    public void setState(SchedulerState state) {
        this.state = state;
    }

    @Autowired
    public DailyScheduler(SimpMessagingTemplate messagingTemplate, DataProvider dataProvider) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
        this.isRunning = true;
    }

    @Override
    public void run() {
        while(isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                refetchData();
                stateLock.lock();
                // PLAN LOGIC
                pucp.pdds.backend.algos.algorithm.Environment environment = new pucp.pdds.backend.algos.algorithm.Environment(
                    state.getActiveVehicles(), 
                    state.getActiveOrders(), 
                    state.getWarehouses(), 
                    state.getActiveBlockages(), 
                    state.getFailures(), 
                    state.getActiveMaintenances(), 
                    state.getCurrTime(), 
                    state.minutesToSimulate
                );
                debugPrint("Planning interval " + state.getCurrTime() + " started at " + state.getCurrTime() + " with " + state.getActiveVehicles().size() + " vehicles and " + state.getActiveOrders().size() + " orders");
                stateLock.unlock();
                Algorithm algorithm = new Algorithm(true);
                Solution sol = algorithm.run(environment, state.minutesToSimulate);
                debugPrint(sol.toString());
                // PLAN LOGIC

                state.initializeVehicles();

                for (int iteration = 0; iteration < state.minutesToSimulate && isRunning && !Thread.currentThread().isInterrupted(); iteration++) {
                    stateLock.lock();
                    state.advance(sol);
                    onAfterExecution(iteration, sol);
                    stateLock.unlock();

                    stateLock.lock();
                    pushChanges();
                    stateLock.unlock();

                    LocalDateTime realTime = state.getCurrTime().toLocalDateTime();
                    long sleepMillis = Duration.between(LocalDateTime.now(), realTime).toMillis();

                    try {
                        if (sleepMillis > 0) {
                            Thread.sleep(sleepMillis);
                        }
                    } catch (InterruptedException e) {
                        isRunning = false;
                        Thread.currentThread().interrupt();
                        sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                        return;
                    }
                }

                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }
            } catch (Exception e) {
                sendError("Unexpected error during simulation: " + e.getMessage());
                isRunning = false;
                return;
            }
        }

        if (!isRunning) {
            sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void refetchData() {
        stateLock.lock();
        try {
            dataProvider.refetchData(state);
        } finally {
            stateLock.unlock();
        }
    }

    public void pushChanges() {
        stateLock.lock();
        try {
            dataProvider.pushChanges(state);
        } finally {
            stateLock.unlock();
        }
    }

    private void debugPrint(String message) {
        System.out.println(state.getCurrTime() + " | " + message);
    }

    private void onAfterExecution(int iteration, Solution sol) {
        java.util.Map<String, Object> response = buildSimulationUpdateResponse(state, sol);
        sendResponse("SIMULATION_UPDATE", response);

            // SimulationVisualizer.draw(state.getActiveVehicles(), state.getActiveBlockages(), state.getCurrTime(), state.minutesToSimulate, state.getWarehouses(), sol);
    }

    private java.util.Map<String, Object> buildSimulationUpdateResponse(SchedulerState state, Solution sol) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("minuto", formatSimTime(state.getCurrTime()));
        var almacenes = DataChunk.convertWarehousesToDataChunk(state.getWarehouses());
        var vehiculos = DataChunk.convertVehiclesToDataChunk(state.getVehicles(), sol.routes);
        var pedidos = DataChunk.convertOrdersToDataChunk(state.getPastOrders(), state.getVehicles(), sol.routes, state.getCurrTime());
        var incidencias = DataChunk.convertIncidentsToDataChunk(state.getFailures());
        var mantenimientos = DataChunk.convertMaintenancesToDataChunk(state.getActiveMaintenances());
        var bloqueos = DataChunk.convertBlockagesToDataChunk(state.getActiveBlockages());

        formatAlmacenes(almacenes);
        formatVehiculos(vehiculos);
        formatPedidos(pedidos);
        formatIncidencias(incidencias);
        formatMantenimientos(mantenimientos);
        formatBloqueos(bloqueos);

        response.put("almacenes", almacenes);
        response.put("vehiculos", vehiculos);
        response.put("pedidos", pedidos);
        response.put("incidencias", incidencias);
        response.put("mantenimientos", mantenimientos);
        response.put("bloqueos", bloqueos);
        return response;
    }

    // Formatters for each list
    private void formatAlmacenes(java.util.List<?> almacenes) {
    }
    private void formatVehiculos(java.util.List<?> vehiculos) {
        for (Object v : vehiculos) {
            try {
                var etaField = v.getClass().getField("eta");
                Object eta = etaField.get(v);
                if (eta != null && !eta.toString().equals("-")) {
                    etaField.set(v, formatSimTime(eta));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatPedidos(java.util.List<?> pedidos) {
        for (Object p : pedidos) {
            try {
                var fechaLimiteField = p.getClass().getField("fechaLimite");
                Object fechaLimite = fechaLimiteField.get(p);
                if (fechaLimite != null) {
                    fechaLimiteField.set(p, formatSimTime(fechaLimite));
                }
                var vehiculosAtendiendoField = p.getClass().getField("vehiculosAtendiendo");
                java.util.List<?> vehiculosAtendiendo = (java.util.List<?>) vehiculosAtendiendoField.get(p);
                for (Object va : vehiculosAtendiendo) {
                    try {
                        var etaField = va.getClass().getField("eta");
                        Object eta = etaField.get(va);
                        if (eta != null) {
                            etaField.set(va, formatSimTime(eta));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatIncidencias(java.util.List<?> incidencias) {
        for (Object i : incidencias) {
            try {
                var fechaInicioField = i.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(i);
                if (fechaInicio != null) {
                    fechaInicioField.set(i, formatSimTime(fechaInicio));
                }
                var fechaFinField = i.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(i);
                if (fechaFin != null) {
                    fechaFinField.set(i, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatMantenimientos(java.util.List<?> mantenimientos) {
        for (Object m : mantenimientos) {
            try {
                var fechaInicioField = m.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(m);
                if (fechaInicio != null) {
                    fechaInicioField.set(m, formatSimTime(fechaInicio));
                }
                var fechaFinField = m.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(m);
                if (fechaFin != null) {
                    fechaFinField.set(m, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatBloqueos(java.util.List<?> bloqueos) {
        for (Object b : bloqueos) {
            try {
                var fechaInicioField = b.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(b);
                if (fechaInicio != null) {
                    fechaInicioField.set(b, formatSimTime(fechaInicio));
                }
                var fechaFinField = b.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(b);
                if (fechaFin != null) {
                    fechaFinField.set(b, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }

    // Format a Time or LocalDateTime to the required string format
    private String formatSimTime(Object timeObj) {
        if (timeObj == null) return "";
        if (timeObj instanceof pucp.pdds.backend.algos.utils.Time t) {
            return String.format("%02d/%02d/%04d %02d:%02d", t.getDay(), t.getMonth(), t.getYear(), t.getHour(), t.getMinute());
        }
        if (timeObj instanceof java.time.LocalDateTime ldt) {
            return ldt.format(SIM_FORMATTER);
        }
        return timeObj.toString();
    }


    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/daily", response);
    }

    private void sendError(String message) {
        sendResponse("ERROR", message);
    }

    private long calculateMillisToNextMinute() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMinute = now.plusMinutes(1).withSecond(0).withNano(0);
        return Duration.between(now, nextMinute).toMillis();
    }
}