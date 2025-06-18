package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pucp.pdds.backend.algos.scheduler.Scheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerState;
import pucp.pdds.backend.algos.scheduler.DataProvider;
import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class SimulationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final DataProvider dataProvider;
    private final SchedulerState schedulerState;
    private final Environment environment;

    private Scheduler currentSimulation;
    private Thread simulationThread;
    private final Object simulationLock = new Object();

    @Autowired
    public SimulationService(SimpMessagingTemplate messagingTemplate, 
                           DataProvider dataProvider,
                           SchedulerState schedulerState,
                           Environment environment) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
        this.schedulerState = schedulerState;
        this.environment = environment;
    }

    public void startSimulation(InitMessage message) {
        synchronized (simulationLock) {
            stopCurrentSimulation();

            try {
                logger.info("Starting simulation - loading fresh data from database...");
                sendResponse("SIMULATION_LOADING", "Loading data from database...");
                
                // Initialize scheduler state with fresh data from database
                var vehicles = dataProvider.getVehicles();
                var orders = dataProvider.getOrders();
                var blockages = dataProvider.getBlockages();
                var warehouses = dataProvider.getWarehouses();
                var failures = dataProvider.getFailures();
                var maintenances = dataProvider.getMaintenances();
                
                logger.info("Loaded {} vehicles, {} orders, {} blockages, {} warehouses, {} failures, {} maintenances", 
                    vehicles.size(), orders.size(), blockages.size(), warehouses.size(), failures.size(), maintenances.size());
                
                schedulerState.setVehicles(vehicles.stream().map(v -> v.clone()).toList());
                schedulerState.setOrders(new java.util.ArrayList<>(orders.stream().map(o -> o.clone()).toList()));
                schedulerState.setBlockages(blockages.stream().map(b -> b.clone()).toList());
                schedulerState.setWarehouses(warehouses.stream().map(w -> w.clone()).toList());
                schedulerState.setFailures(new java.util.ArrayList<>(failures.stream().map(f -> f.clone()).toList()));
                schedulerState.setMaintenances(maintenances.stream().map(m -> m.clone()).toList());
                schedulerState.setCurrTime(dataProvider.getInitialTime().clone());

                currentSimulation = new Scheduler(schedulerState, messagingTemplate);
                simulationThread = new Thread(currentSimulation, "simulation-thread");
                simulationThread.start();

                logger.info("Simulation started successfully");
                sendResponse("SIMULATION_STARTED", "Simulation initialized with fresh data from database");
            } catch (Exception e) {
                logger.error("Error starting simulation", e);
                sendResponse("SIMULATION_ERROR", "Error starting simulation: " + e.getMessage());
            }
        }
    }
    
    public void updateFailures(UpdateFailuresMessage message) {
        synchronized (simulationLock) {
            if (currentSimulation != null) {
                try {
                    currentSimulation.updateFailures(message);
                    sendResponse("STATE_UPDATED", "State updated successfully");
                } catch (Exception e) {
                    sendResponse("ERROR", "Failed to update state: " + e.getMessage());
                }
            } else {
                sendResponse("ERROR", "No active simulation");
            }
        }
    }

    public void stopSimulation() {
        synchronized (simulationLock) {
            stopCurrentSimulation();
            sendResponse("SIMULATION_STOPPED", "Simulation stopped");
        }
    }

    private void stopCurrentSimulation() {
        if (currentSimulation != null) {
            currentSimulation.stop();
            if (simulationThread != null && simulationThread.isAlive()) {
                simulationThread.interrupt();
                try {
                    simulationThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            currentSimulation = null;
            simulationThread = null;
        }
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }
}
