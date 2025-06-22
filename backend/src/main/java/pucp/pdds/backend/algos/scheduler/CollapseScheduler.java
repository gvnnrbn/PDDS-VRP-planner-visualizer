package pucp.pdds.backend.algos.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.SimulationStateDTO;

public class CollapseScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CollapseScheduler.class);
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean running = true;
    private SchedulerState state;

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
        
        while (running) {
            // TODO: Implement the core logic for the "simulation to collapse".
            // This loop should advance time, check for collapse conditions (e.g., no more vehicles,
            // no more GLP, unable to fulfill orders, etc.), and send updates.
            
            // Placeholder: Send a heartbeat message every 5 seconds.
            try {
                Thread.sleep(1000); // Reduced sleep time to 1 second for more fluid visualization
                if (state != null) {
                    // Create DTO with current state
                    SimulationStateDTO stateDTO = new SimulationStateDTO(
                        state.getCurrTime().toString(),
                        state.getVehicles(),
                        state.getOrders(),
                        state.getBlockages(),
                        state.getWarehouses()
                    );
                    
                    // Send the full state for visualization
                    sendResponse("SIMULATION_UPDATE", stateDTO);

                    // Advance simulation time by 1 minute
                    Time newTime = state.getCurrTime().addMinutes(1);
                    state.setCurrTime(newTime);
                }

            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Collapse simulation thread finished.");
    }

    private void sendResponse(String type, Object data) {
        if (this.messagingTemplate != null) {
            SimulationResponse response = new SimulationResponse(type, data);
            messagingTemplate.convertAndSend("/topic/simulation", response);
        }
    }
} 