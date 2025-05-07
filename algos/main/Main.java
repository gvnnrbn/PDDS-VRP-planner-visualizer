package main;

import domain.Environment;
import domain.Order;
import domain.SimulationState;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Time;
import domain.Vehicle;
import domain.Warehouse;

import java.lang.Thread.State;
import java.util.List;
import localsearch.TabuSearch;
import scheduler.Maintenance;
import scheduler.StateVehicle;
import utils.EnvironmentBuilder;
import utils.EnvironmentParser;
import utils.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        
        // === 1. PARAMETROS DEL PLANIFICADOR ===
        int Ta = 1; // Tiempo que tarda el algoritmo (minutos)
        int Sa = 5; // Cada cuánto se lanza el planificador (minutos)
        int K = 14; // Escenario de simulación (1=día a día, 14=3 días, 75=colapso)
        int Sc = K * Sa; // Tiempo que se consume por cada planificación

        // === 2. PARSEAR DATOS DE ENTRADA ===
        Time startTime = new Time(1, 1, 0, 0); // 01/01 00:00
        EnvironmentParser parser = new EnvironmentParser(startTime);

        // cambiar a otras clases
        List<StateVehicle> vehicles = StateVehicle.parseVehicles("main/vehicles.csv");
        List<Maintenance> maintenances = Maintenance.parseMaintenances("main/maintenances.csv");
        List<StateOrder> orders = parser.parseOrders("main/orders.csv");
        List<StateWarehouse> warehouses = parser.parseWarehouses("main/warehouses.csv");
        List<StateBlockage> blockages = parser.parseBlockages("main/blockages.csv");
        
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
