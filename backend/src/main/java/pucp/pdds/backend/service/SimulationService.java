package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.scheduler.Scheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerAgent;
import pucp.pdds.backend.algos.scheduler.SchedulerAgentTextFiles;
import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class SimulationService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Scheduler currentSimulation;
    private Thread simulationThread;
    private Object simulationLock = new Object();

    public void startSimulation(InitMessage message) {
        synchronized (simulationLock) {
            stopCurrentSimulation();

            try {
                SchedulerAgent agent = createSchedulerAgent(message);
                currentSimulation = new Scheduler(agent, messagingTemplate);
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
            try{
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            simulationThread = null;
            currentSimulation = null;
        }
        currentSimulation = null;
        simulationThread = null;
    }

    private SchedulerAgent createSchedulerAgent(InitMessage message) {
        return new SchedulerAgentTextFiles(message.getInitialTime());
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }
}
