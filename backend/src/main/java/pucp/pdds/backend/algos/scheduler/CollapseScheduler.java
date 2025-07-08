package pucp.pdds.backend.algos.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;
import pucp.pdds.backend.dto.collapse.CollapseSimulationResponse;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CollapseScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CollapseScheduler.class);
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean running = true;
    private SchedulerState state;
    private Lock stateLock = new ReentrantLock();
    private static final DateTimeFormatter SIM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CollapseScheduler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setState(SchedulerState state) {
        this.state = state;
    }

    public void stop() {
        this.running = false;
        logger.info("Collapse simulation stop requested.");
    }

    @Override
    public void run() {
        logger.info("Collapse simulation thread started.");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                stateLock.lock();
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
                logger.info("Planning interval for collapse simulation started at " + state.getCurrTime());
                stateLock.unlock();

                Algorithm algorithm = new Algorithm(true);
                Solution sol = algorithm.run(environment, state.minutesToSimulate);

                if (!sol.isFeasible()) {
                    sendError("System collapsed. Could not find a feasible plan for the next " + state.minutesToSimulate + " minutes.");
                    running = false;
                    break;
                }

                state.initializeVehicles();

                for (int i = 0; i < state.minutesToSimulate && running && !Thread.currentThread().isInterrupted(); i++) {
                    stateLock.lock();
                    state.advance(sol, true);
                    sendSimulationUpdate(sol);
                    stateLock.unlock();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        running = false;
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Error during collapse simulation", e);
                sendError("Unexpected error during collapse simulation: " + e.getMessage());
                running = false;
            }
        }
        sendResponse("COLLAPSE_SIMULATION_STOPPED", "Collapse simulation finished.");
        logger.info("Collapse simulation thread finished.");
    }

    private void sendSimulationUpdate(Solution sol) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("minuto", formatSimTime(state.getCurrTime()));
        var almacenes = DataChunk.convertWarehousesToDataChunk(state.getWarehouses());
        var vehiculos = DataChunk.convertVehiclesToDataChunk(state.getVehicles(), sol.routes);
        var pedidos = DataChunk.convertOrdersToDataChunk(state.getPastOrders(), state.getVehicles(), sol.routes, state.getCurrTime());
        var incidencias = DataChunk.convertIncidentsToDataChunk(state.getFailures());
        var mantenimientos = DataChunk.convertMaintenancesToDataChunk(state.getActiveMaintenances());
        var bloqueos = DataChunk.convertBlockagesToDataChunk(state.getActiveBlockages());
        
        response.put("almacenes", almacenes);
        response.put("vehiculos", vehiculos);
        response.put("pedidos", pedidos);
        response.put("incidencias", incidencias);
        response.put("mantenimientos", mantenimientos);
        response.put("bloqueos", bloqueos);
        
        sendResponse("SIMULATION_UPDATE", response);
    }
    
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
        if (this.messagingTemplate != null) {
            CollapseSimulationResponse response = new CollapseSimulationResponse(type, data);
            messagingTemplate.convertAndSend("/topic/collapse-simulation", response);
        }
    }

    private void sendError(String message) {
        sendResponse("COLLAPSE_SIMULATION_ERROR", message);
    }
} 