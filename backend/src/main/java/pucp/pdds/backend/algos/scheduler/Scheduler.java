package pucp.pdds.backend.algos.scheduler;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.utils.SimulationVisualizer;
import pucp.pdds.backend.algos.utils.Time;

public class Scheduler {
    private SchedulerAgent agent;

    public Scheduler(SchedulerAgent agent) {
        this.agent = agent;
    }

    public void run() {
        SchedulerState state = SchedulerState.getInstance();

        // FILL WITH DATA
        state.warehouses = agent.getWarehouses();
        state.vehicles = agent.getVehicles();
        state.orders = agent.getOrders();
        state.blockages = agent.getBlockages();
        state.failures = agent.getFailures();
        state.maintenances = agent.getMaintenances();
        state.currTime = agent.getInitialTime();

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
                throw new RuntimeException("Solution is not feasible");
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
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        }
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

        SimulationVisualizer.draw(state.getActiveVehicles(), state.getActiveBlockages(), state.currTime, state.minutesToSimulate, state.warehouses, sol);
    }
}