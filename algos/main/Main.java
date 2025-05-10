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
import scheduler.SimulationState;
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
        
        // === 1. PARAMETROS DEL PLANIFICADOR ===
        int Ta = 1; // Tiempo que tarda el algoritmo (minutos)
        int Sa = 5; // Cada cuánto se lanza el planificador (minutos)
        int K = 14; // Escenario de simulación (1=día a día, 14=3 días, 75=colapso)
        int Sc = K * Sa; // Tiempo que se consume por cada planificación

        // === 2. PARSEAR DATOS DE ENTRADA ===
        Time startTime = new Time(1,1, 1, 0, 0); // 1/01/01 00:00
        EnvironmentParser parser = new EnvironmentParser(startTime);

        // Parsing state elements
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
        
        // agregar bloqueos, mantenimientos y lista de averias (validar si empty para los escenarios que no consideren averias)
        // todo debe entrar filtrado inicialmente en t=0
        SimulationState simulationState = new SimulationState(startTime, vehicles, orders, warehouses);

        // === 3. BUCLE DE PLANIFICACIÓN ===
        int iteration = 0;
        while (!simulationState.pendingOrders.isEmpty()) {
            

            System.out.println("\n--- PLANIFICACION #" + (++iteration) + " ---");
            System.out.println("Tiempo actual: " + simulationState.currentTime);

            // 3.1. Construir environment con los pedidos dentro del próximo SC
            Environment environment = EnvironmentBuilder.build(simulationState, Sc);

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
            SimulationEngine.apply(bestSolution, simulationState, Sc);
            simulationState.printState(iteration);
        }

        System.out.println("\n✅ Todas las planificaciones completadas.");

    }
}
