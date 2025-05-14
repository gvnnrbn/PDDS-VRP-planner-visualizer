package main;

import domain.Environment;
import domain.Order;
import domain.Position;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Vehicle;
import domain.Warehouse;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Thread.State;
import java.util.List;
import localsearch.TabuSearch;
import scheduler.SchedulerFailure;
import scheduler.SchedulerMaintenance;
import scheduler.ScheduleState;
import scheduler.SchedulerBlockage;
import scheduler.SchedulerOrder;
import scheduler.SchedulerVehicle;
import scheduler.SchedulerWarehouse;
import utils.EnvironmentBuilder;
import utils.EnvironmentParser;
import utils.SimulationEngine;
import utils.Time;

public class Main {
    public static void main(String[] args) {
        // redirect stdout to a results.txt file
        try {
            System.setOut(new PrintStream(new FileOutputStream("results.txt")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=== Starting Simulation ===");
        System.out.println("Timestamp: " + new java.util.Date());
        
        // NEW, CHECK PARAMETERS
        int minutesToSimulate = 75;
        int timeUnit = 1; 
        int iterations = (7 * 24 * 60) / minutesToSimulate + 1;
        System.out.printf("Simulation Parameters:%n- Minutes per iteration: %d%n- Time unit: %d%n- Max iterations: %d%n", 
            minutesToSimulate, timeUnit, iterations);

        // === 1. PARAMETROS DEL PLANIFICADOR ===
        int Ta = 1; // Tiempo que tarda el algoritmo (minutos)
        int Sa = 5; // Cada cuánto se lanza el planificador (minutos)
        int Sc = 60; // Cada cuánto se lanza el planificador (minutos)
        
        System.out.println("\n=== Loading Input Data ===");
        Time startTime = new Time(1, 1, 1, 0, 0);
        System.out.println("Start time: " + startTime);

        // === 2. CARGAR DATOS ===
        // 2.1. Cargar datos de entrada
        System.out.println("\nLoading vehicles...");
        List<SchedulerVehicle> vehicles = SchedulerVehicle.parseVehicles("main/vehicles.csv", new Position(0, 0));
        System.out.println("Vehicles loaded: " + vehicles.size());
        
        System.out.println("Loading orders...");
        List<SchedulerOrder> orders = SchedulerOrder.parseOrders("main/orders.csv");
        System.out.println("Orders loaded: " + orders.size());
        
        System.out.println("Loading warehouses...");
        List<SchedulerWarehouse> warehouses = SchedulerWarehouse.parseWarehouses("main/warehouses.csv");
        SchedulerWarehouse mainWarehouse = warehouses.get(0);
        System.out.println("Warehouses loaded: " + warehouses.size() + " (main: " + mainWarehouse.id + ")");
        
        System.out.println("Loading blockages...");
        List<SchedulerBlockage> blockages = SchedulerBlockage.parseBlockages("main/blockages.csv");
        System.out.println("Blockages loaded: " + blockages.size());
        
        System.out.println("Loading maintenances...");
        List<SchedulerMaintenance> maintenances = SchedulerMaintenance.parseMaintenances("main/maintenances.csv");
        System.out.println("Maintenances loaded: " + maintenances.size());
        
        System.out.println("Loading failures...");
        List<SchedulerFailure> failures = SchedulerFailure.parseFailures("main/failures.csv");
        System.out.println("Failures loaded: " + failures.size());
        
        // === 3. SCHEDULER ===
        System.out.println("\n=== Initializing Simulation ===");
        ScheduleState simulationState = new ScheduleState(startTime); // initial state (no input)


        int iteration = 0;
        int maxIterations = 24 * 60 * 7 / Sc + 1;
        System.out.println("\n=== Starting Scheduling Process ===");
        System.out.printf("Max iterations: %d, Time per iteration: %d minutes%n", maxIterations, Sc);
        
        while (orders.stream().noneMatch(o -> o.deliverTime != null) && iteration < maxIterations) {
            System.out.println("\n=== Iteration " + (iteration + 1) + " ===");
            System.out.println("Current time: " + simulationState.currentTime);
            
            // 3.0 Filters data available until currentTime
            System.out.println("Updating simulation state...");
            simulationState.updateData(simulationState.currentTime, vehicles, orders, warehouses, blockages, maintenances, failures);
            
            System.out.println("Current state:" +
                "\n- Vehicles: " + simulationState.vehicles.size() +
                "\n- Pending orders: " + simulationState.orders.size() +
                "\n- Available warehouses: " + simulationState.warehouses.size() +
                "\n- Active blockages: " + simulationState.blockages.size());
            
            // 3.1. Build environment with current state
            System.out.println("\nBuilding environment...");
            Environment environment = EnvironmentBuilder.build(simulationState, Sc);

            if (environment.orders.isEmpty()) {
                System.out.println("No orders to plan. Skipping to next time window.");
                iteration++;
                simulationState.currentTime = simulationState.currentTime.addMinutes(Sc);
                continue;
            }
            
            System.out.println("Planning for " + environment.orders.size() + " orders...");
            
            // 3.2. Generate initial solution
            System.out.println("Generating initial solution...");
            SolutionInitializer initializer = new SolutionInitializer();
            Solution initialSolution = initializer.generateInitialSolution(environment);
            
            // 3.3. Run Tabu Search
            System.out.println("Running Tabu Search optimization...");
            TabuSearch tabuSearch = new TabuSearch();
            Solution bestSolution = tabuSearch.run(environment, initialSolution);
            
            if (!bestSolution.routes.isEmpty()) {
                System.out.println("Solution found with " + bestSolution.routes.size() + " routes");
                System.out.println("\n=== Solution Report ===");
                System.out.println(bestSolution.getReport());
                // bestSolution.exportToCSV("main/tabuSearchResults1.csv");
            } else {
                System.out.println("No valid routes generated in this iteration.");
            }
            
            // 3.5. Simulate plan execution and update state
            System.out.println("\nSimulating plan execution...");
            SimulationEngine.apply(bestSolution, simulationState, minutesToSimulate, timeUnit, mainWarehouse);
            
            // Update time for next iteration
            simulationState.currentTime = simulationState.currentTime.addMinutes(Sc);
            iteration++;
            
            System.out.println("=== End of Iteration " + iteration + " ===");
            System.out.println("Current time: " + simulationState.currentTime);
            System.out.println("Pending orders: " + orders.stream().filter(o -> o.deliverTime == null).count());
        }

        System.out.println("\n=== Simulation Complete ===");
        System.out.println("Total iterations: " + iteration);
        System.out.println("Final time: " + simulationState.currentTime);
        int completedOrders = (int)orders.stream().filter(o -> o.deliverTime != null).count();
        System.out.printf("Completed %d out of %d orders (%.1f%%)%n", 
            completedOrders, orders.size(), (completedOrders * 100.0) / orders.size());
            
        System.out.println("\n✅ All planning completed.");
    }
}
