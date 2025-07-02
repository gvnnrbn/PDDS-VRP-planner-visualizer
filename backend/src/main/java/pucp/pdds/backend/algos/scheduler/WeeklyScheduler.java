package pucp.pdds.backend.algos.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;
import java.time.format.DateTimeFormatter;

@Service
public class WeeklyScheduler implements Runnable {
    private SchedulerState state;
    private Lock stateLock = new ReentrantLock();
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean isRunning;
    private static final DateTimeFormatter SIM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    private SynchronousQueue<SchedulerState> stateQueue = new SynchronousQueue<>();
    private SynchronousQueue<Solution> solutionQueue = new SynchronousQueue<>();

    private Thread algorithmThread;

    public void setState(SchedulerState state) {
        this.state = state;
    }

    @Autowired
    public WeeklyScheduler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.isRunning = true;
    }

    @Override
    public void run() {
        algorithmThread = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    while (true) {
                        SchedulerState state = stateQueue.take();
                        Environment environment = new Environment(
                            state.getActiveVehicles(), 
                            state.getActiveOrders(), 
                            state.getWarehouses(), 
                            state.getActiveBlockages(), 
                            state.getFailures(), 
                            state.getActiveMaintenances(), 
                            state.getCurrTime(), 
                            state.minutesToSimulate
                        );
                        Algorithm algorithm = new Algorithm(true);
                        Solution sol = algorithm.run(environment, state.minutesToSimulate);
                        solutionQueue.put(sol);
                    }
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }
            }
        });

        algorithmThread.start();

        Time endSimulationTime = state.getCurrTime().addTime(new Time(0,0,7,0,0));

        while(state.getCurrTime().isBefore(endSimulationTime) && isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Solution sol;
                try {
                    sol = solutionQueue.take();
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }

                stateLock.lock();
                SchedulerState clonedState = state.clone();
                stateLock.unlock();

                for (int i = 0; i < clonedState.minutesToSimulate; i++) {
                    clonedState.advance(sol, false);
                }

                try {
                    stateQueue.put(clonedState);
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }

                stateLock.lock();
                state.initializeVehicles();
                stateLock.unlock();

                debugPrint("Executing solution meant for " + sol.getStartingTime());

                for (int iteration = 0; iteration < state.minutesToSimulate && isRunning && !Thread.currentThread().isInterrupted(); iteration++) {
                    stateLock.lock();
                    state.advance(sol, true);
                    onAfterExecution(iteration, sol);
                    stateLock.unlock();

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        isRunning = false;
                        Thread.currentThread().interrupt();
                        algorithmThread.interrupt();
                        sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                        return;
                    }
                }

                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    algorithmThread.interrupt();
                    return;
                }
            } catch (Exception e) {
                sendError("Unexpected error during simulation: " + e.getMessage());
                e.printStackTrace();
                algorithmThread.interrupt();
                isRunning = false;
                return;
            }
        }

        if (!isRunning) {
            sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
            algorithmThread.interrupt();
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void updateFailures(UpdateFailuresMessage message) {
        stateLock.lock();
        int lastId = state.getFailures().size() > 0 ? state.getFailures().getLast().id : 0;

        PlannerFailure failure = new PlannerFailure(
            lastId + 1, message.getType(), message.getShiftOccurredOn(),
            message.getVehiclePlaque(), null);
        List<PlannerFailure> failures = new ArrayList<>(state.getFailures());
        failures.add(failure);
        state.setFailures(failures);
        stateLock.unlock();
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
        var indicadores = DataChunk.convertIndicatorsToDataChunk(state.getActiveIndicators());

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
        response.put("indicadores", indicadores);
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
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }

    private void sendError(String message) {
        sendResponse("ERROR", message);
    }
}