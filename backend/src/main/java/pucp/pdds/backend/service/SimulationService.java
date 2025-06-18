package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.scheduler.Scheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerState;
import pucp.pdds.backend.algos.scheduler.DataProvider;
import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class SimulationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final DataProvider dataProvider;
    private final SchedulerState schedulerState;

    private Scheduler currentSimulation;
    private Thread simulationThread;
    private final Object simulationLock = new Object();

    @Autowired
    public SimulationService(SimpMessagingTemplate messagingTemplate, 
                           DataProvider dataProvider,
                           SchedulerState schedulerState) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
        this.schedulerState = schedulerState;
    }

    public void startSimulation(InitMessage message) {
        synchronized (simulationLock) {
            stopCurrentSimulation();

            try {
                // Initialize scheduler state with data
                schedulerState.setVehicles(dataProvider.getVehicles());
                schedulerState.setOrders(dataProvider.getOrders());
                schedulerState.setBlockages(dataProvider.getBlockages());
                schedulerState.setWarehouses(dataProvider.getWarehouses());
                schedulerState.setFailures(dataProvider.getFailures());
                schedulerState.setMaintenances(dataProvider.getMaintenances());
                schedulerState.setCurrTime(dataProvider.getInitialTime());

                currentSimulation = new Scheduler(schedulerState, messagingTemplate);
                simulationThread = new Thread(currentSimulation, "simulation-thread");
                simulationThread.start();

                sendResponse("SIMULATION_STARTED", "Simulation initialized");
            } catch (Exception e) {
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

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }
}
