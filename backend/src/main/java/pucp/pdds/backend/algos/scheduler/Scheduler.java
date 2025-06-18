package pucp.pdds.backend.algos.scheduler;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.utils.SimulationVisualizer;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

public class Scheduler implements Runnable {
    private final SchedulerAgent agent;
    private final SimpMessagingTemplate messagingTemplate;

    public Scheduler(SchedulerAgent agent, SimpMessagingTemplate messagingTemplate) {
        this.agent = agent;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void run() {
        SchedulerState state = SchedulerState.getInstance();

        initializeState(state);

        Time endSimulationTime = state.currTime.addTime(new Time(0,0,7,0,0));

        while(state.currTime.isBefore(endSimulationTime)) {
            // PLAN LOGIC
            Environment environment = new Environment(state.getActiveVehicles(), state.getActiveOrders(), state.warehouses, state.getActiveBlockages(), state.failures, state.getActiveMaintenances(), state.currTime, state.minutesToSimulate);
            debugPrint("Planning interval " + state.currTime + " started at " + state.currTime + " with " + state.getActiveVehicles().size() + " vehicles and " + state.getActiveOrders().size() + " orders");
            Algorithm algorithm = new Algorithm(true);
            Solution sol = algorithm.run(environment, state.minutesToSimulate);
            debugPrint(sol.toString());
            // PLAN LOGIC

            if (!sol.isFeasible(environment)) {
                sendError("Can't continue delivering, couldn't find a feasible plan for next " + state.minutesToSimulate + " minutes");
                break;
            }

            SchedulerState.lock.lock();
            state.initializeVehicles();
            SchedulerState.lock.unlock();

            for (int iteration = 0; iteration < state.minutesToSimulate; iteration++) {

                SchedulerState.lock.lock();
                state.advance(sol);
                onAfterExecution(iteration, state, sol);
                SchedulerState.lock.unlock();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        }
    }

    public void updateFailures(UpdateFailuresMessage message) {
        SchedulerState.lock.lock();

        SchedulerState state = SchedulerState.getInstance();
        int lastId = state.failures.size() > 0 ? state.failures.getLast().id : 0;

        PlannerFailure failure = new PlannerFailure(
            lastId + 1, message.getType(), message.getShiftOccurredOn(),
            message.getVehiclePlaque(), null);
        state.failures.add(failure);
        
        SchedulerState.lock.unlock();
    }

    private void initializeState(SchedulerState state) {
        state.warehouses = agent.getWarehouses();
        state.vehicles = agent.getVehicles();
        state.orders = agent.getOrders();
        state.blockages = agent.getBlockages();
        state.failures = agent.getFailures();
        state.maintenances = agent.getMaintenances();
        state.currTime = agent.getInitialTime();
    }

    private void debugPrint(String message) {
        System.out.println(SchedulerState.getInstance().currTime + " | " + message);
    }

    private void onAfterExecution(int iteration, SchedulerState state, Solution sol) {
        DataChunk.SimulacionMinuto simulacionMinuto = new DataChunk.SimulacionMinuto(iteration);
        simulacionMinuto.setVehiculos(DataChunk.convertVehiclesToDataChunk(state.getActiveVehicles(), sol.routes));
        simulacionMinuto.setAlmacenes(DataChunk.convertWarehousesToDataChunk(state.warehouses));
        simulacionMinuto.setPedidos(DataChunk.convertOrdersToDataChunk(state.getActiveOrders(), state.getActiveVehicles(), sol.routes, state.currTime));
        simulacionMinuto.setIncidencias(DataChunk.convertIncidentsToDataChunk(state.failures, state.getActiveMaintenances()));

        sendResponse("SIMULATION_UPDATE", simulacionMinuto);

        SimulationVisualizer.draw(state.getActiveVehicles(), state.getActiveBlockages(), state.currTime, state.minutesToSimulate, state.warehouses, sol);
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }

    private void sendError(String message) {
        sendResponse("ERROR", message);
    }
}