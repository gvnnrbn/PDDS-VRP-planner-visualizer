package main;

import domain.Environment;
import domain.Order;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Vehicle;
import domain.Warehouse;

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
        // NEW, CHECK PARAMETERS
        int updateStateTimeUnit = 1; 

        // === 1. PARAMETROS DEL PLANIFICADOR ===
        int Ta = 1; // Tiempo que tarda el algoritmo (minutos)
        int Sa = 5; // Cada cuánto se lanza el planificador (minutos)
        int K = 14; // Duracion de escenario de simulación (1=día a día, 14=3 días, 75=colapso)
        int Sc = K * Sa; // Tiempo que se consume por cada planificación

        Time startTime = new Time(1,1, 1, 0, 0); // 1/01/01 00:00
        
        // === 2. PARSE SCHEDULER INPUT ===
        List<SchedulerWarehouse> warehouses = SchedulerWarehouse.parseWarehouses("main/warehouses.csv");
        SchedulerWarehouse mainWarehouse = warehouses.stream()
            .filter(SchedulerWarehouse::isMain)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No main warehouse found"));
        List<SchedulerVehicle> vehicles = SchedulerVehicle.parseVehicles("main/vehicles.csv", mainWarehouse.position);
        List<SchedulerMaintenance> maintenances = SchedulerMaintenance.parseMaintenances("main/maintenances.csv");
        List<SchedulerOrder> orders = SchedulerOrder.parseOrders("main/orders.csv");
        List<SchedulerBlockage> blockages = SchedulerBlockage.parseBlockages("main/blockages.csv");
        List<SchedulerFailure> failures = SchedulerFailure.parseFailures("main/failures.csv");
        
        // === 3. SCHEDULER ===
        ScheduleState simulationState = new ScheduleState(startTime); // initial state (no input)

        int iteration = 0;
        while (!orders.isEmpty()) {// remove orders added to simulationState.pendingOrders from orders
            
            // 3.0 Filters data available until currentTime
            simulationState.updateData(simulationState.currentTime, vehicles, orders, warehouses, blockages, maintenances, failures);
            
            System.out.println("\n--- PLANIFICACION #" + (++iteration) + " ---");
            System.out.println("Tiempo actual: " + simulationState.currentTime);
            
            // 3.1. Build environment with current state
            Environment environment = EnvironmentBuilder.build(simulationState);
            
            // 3.2. Generar solución inicial
            SolutionInitializer initializer = new SolutionInitializer();
            Solution initialSolution = initializer.generateInitialSolution(environment);
            
            // 3.3. Ejecutar Tabu Search
            TabuSearch tabuSearch = new TabuSearch();
            Solution bestSolution = tabuSearch.run(environment, initialSolution);
            
            if (!bestSolution.routes.isEmpty()) {
                // bestSolution.exportToCSV("main/tabuSearchResults1.csv");
                System.out.println(bestSolution.getReport());
            } else {
                System.out.println("No se exporta solucion porque no hay rutas generadas.");
            }
            
            
            // 3.5. Simular la ejecución del plan y actualizar el estado
            // inside apply() calculate if failure happens
            SimulationEngine.apply(bestSolution, simulationState, Sc, updateStateTimeUnit,mainWarehouse);
            // simulationState.printState(iteration);
        }

        System.out.println("\n✅ Todas las planificaciones completadas.");

    }
}
