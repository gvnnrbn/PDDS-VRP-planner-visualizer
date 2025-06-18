package pucp.pdds.backend.algos.scheduler;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.utils.SimulationVisualizer;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class Scheduler implements Runnable {
    private final SchedulerState state;
    private final SimpMessagingTemplate messagingTemplate;
    private final Environment springEnv;

    @Autowired
    public Scheduler(SchedulerState state, SimpMessagingTemplate messagingTemplate, Environment springEnv) {
        this.state = state;
        this.messagingTemplate = messagingTemplate;
        this.springEnv = springEnv;
    }

    @Override
    public void run() {
        initializeState();

        Time endSimulationTime = state.getCurrTime().addTime(new Time(0,0,7,0,0));

        while(state.getCurrTime().isBefore(endSimulationTime)) {
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
            Algorithm algorithm = new Algorithm(true);
            Solution sol = algorithm.run(environment, state.minutesToSimulate);
            debugPrint(sol.toString());
            // PLAN LOGIC

            if (!sol.isFeasible(environment)) {
                sendError("Can't continue delivering, couldn't find a feasible plan for next " + state.minutesToSimulate + " minutes");
                break;
            }

            state.initializeVehicles();

            for (int iteration = 0; iteration < state.minutesToSimulate; iteration++) {
                state.advance(sol);
                onAfterExecution(iteration, sol);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void updateFailures(UpdateFailuresMessage message) {
        int lastId = state.getFailures().size() > 0 ? state.getFailures().getLast().id : 0;

        PlannerFailure failure = new PlannerFailure(
            lastId + 1, message.getType(), message.getShiftOccurredOn(),
            message.getVehiclePlaque(), null);
        state.getFailures().add(failure);
    }

    private void initializeState() {
        // State is now initialized in SimulationService
    }

    private void debugPrint(String message) {
        System.out.println(state.getCurrTime() + " | " + message);
    }

    private void onAfterExecution(int iteration, Solution sol) {
        DataChunk.SimulacionMinuto simulacionMinuto = new DataChunk.SimulacionMinuto(iteration);
        simulacionMinuto.setVehiculos(DataChunk.convertVehiclesToDataChunk(state.getActiveVehicles(), sol.routes));
        simulacionMinuto.setAlmacenes(DataChunk.convertWarehousesToDataChunk(state.getWarehouses()));
        simulacionMinuto.setPedidos(DataChunk.convertOrdersToDataChunk(state.getActiveOrders(), state.getActiveVehicles(), sol.routes, state.getCurrTime()));
        simulacionMinuto.setIncidencias(DataChunk.convertIncidentsToDataChunk(state.getFailures(), state.getActiveMaintenances()));

        sendResponse("SIMULATION_UPDATE", simulacionMinuto);

        // Only run visualization in dev mode
        if (isDevProfile()) {
            SimulationVisualizer.draw(state.getActiveVehicles(), state.getActiveBlockages(), state.getCurrTime(), state.minutesToSimulate, state.getWarehouses(), sol);
        }
    }

    private boolean isDevProfile() {
        for (String profile : springEnv.getActiveProfiles()) {
            if (profile.equals("dev")) {
                return true;
            }
        }
        return false;
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }

    private void sendError(String message) {
        sendResponse("ERROR", message);
    }
}