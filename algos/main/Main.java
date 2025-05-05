package main;

import domain.Environment;
import domain.Order;
import domain.SimulationState;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Time;
import domain.Vehicle;
import domain.Warehouse;
import java.util.List;
import localsearch.TabuSearch;
import utils.EnvironmentBuilder;
import utils.EnvironmentParser;
import utils.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        /*
         * EnvironmentParser environmentParser = new EnvironmentParser(new Time(0, 1, 0,
         * 0));
         * Environment environment =
         * environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv",
         * "main/blockages.csv", "main/warehouses.csv");
         * 
         * 
         * 
         * // HillClimbing hillClimbing = new HillClimbing();
         * // Solution bestHillClimbingSolution = hillClimbing.run(environment,
         * initialSolution);
         * List<Order> originalorders = new ArrayList<>(environment.orders);
         * environment.orders = originalorders.subList(0, 50);
         * SolutionInitializer solutionInitializer = new SolutionInitializer();
         * Solution initialSolution =
         * solutionInitializer.generateInitialSolution(environment);
         * for(int i = 0; i < 10; i++) {
         * HillClimbing algorithm = new HillClimbing();
         * Solution bestTabuSearchSolution = algorithm.run(environment,
         * initialSolution);
         * // System.out.println(bestTabuSearchSolution.getReport());
         * bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
         * }
         * 
         * environment = environmentParser.parseEnvironment("main/vehicles.csv",
         * "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
         * environment.orders = originalorders.subList(0, 100);
         * solutionInitializer = new SolutionInitializer();
         * initialSolution = solutionInitializer.generateInitialSolution(environment);
         * for(int i = 0; i < 10; i++) {
         * HillClimbing algorithm = new HillClimbing();
         * Solution bestTabuSearchSolution = algorithm.run(environment,
         * initialSolution);
         * // System.out.println(bestTabuSearchSolution.getReport());
         * bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
         * }
         * 
         * environment = environmentParser.parseEnvironment("main/vehicles.csv",
         * "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
         * environment.orders = originalorders.subList(0, 200);
         * solutionInitializer = new SolutionInitializer();
         * initialSolution = solutionInitializer.generateInitialSolution(environment);
         * for(int i = 0; i < 10; i++) {
         * HillClimbing algorithm = new HillClimbing();
         * Solution bestTabuSearchSolution = algorithm.run(environment,
         * initialSolution);
         * // System.out.println(bestTabuSearchSolution.getReport());
         * bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
         * }
         * 
         * environment = environmentParser.parseEnvironment("main/vehicles.csv",
         * "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
         * environment.orders = originalorders.subList(0, 400);
         * solutionInitializer = new SolutionInitializer();
         * initialSolution = solutionInitializer.generateInitialSolution(environment);
         * for(int i = 0; i < 10; i++) {
         * HillClimbing algorithm = new HillClimbing();
         * Solution bestTabuSearchSolution = algorithm.run(environment,
         * initialSolution);
         * // System.out.println(bestTabuSearchSolution.getReport());
         * bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
         * }
         * 
         * environment = environmentParser.parseEnvironment("main/vehicles.csv",
         * "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
         * environment.orders = originalorders.subList(0, 800);
         * solutionInitializer = new SolutionInitializer();
         * initialSolution = solutionInitializer.generateInitialSolution(environment);
         * for(int i = 0; i < 10; i++) {
         * HillClimbing algorithm = new HillClimbing();
         * Solution bestTabuSearchSolution = algorithm.run(environment,
         * initialSolution);
         * // System.out.println(bestTabuSearchSolution.getReport());
         * bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
         * }
         * // System.out.println("For a sample of " + environment.orders.size() +
         * " orders and " + environment.vehicles.size() + " vehicles:");
         * // System.out.println("=== Hill Climbing Solution ===");
         * // System.out.println(bestHillClimbingSolution.getReport());
         * // System.out.println("\n=== Tabu Search Solution ===");
         */

        // === 1. PARaMETROS DEL PLANIFICADOR ===
        int Ta = 1; // Tiempo que tarda el algoritmo (minutos)
        int Sa = 5; // Cada cuánto se lanza el planificador (minutos)
        int K = 14; // Escenario de simulación (1=día a día, 14=3 días, 75=colapso)
        int Sc = K * Sa; // Tiempo que se consume por cada planificación

        // === 2. PARSEAR DATOS DE ENTRADA ===
        Time startTime = new Time(1, 1, 0, 0); // 01/01 00:00
        EnvironmentParser parser = new EnvironmentParser(startTime);

        List<Vehicle> vehicles = parser.parseVehicles("main/vehicles.csv");
        List<Order> orders = parser.parseOrders("main/orders.csv");
        List<Warehouse> warehouses = parser.parseWarehouses("main/warehouses.csv");

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
                bestSolution.exportToCSV("main/tabuSearchResults1.csv");
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
