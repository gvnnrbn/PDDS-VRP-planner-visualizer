package main;

import domain.Environment;
import domain.Order;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Time;
import java.util.ArrayList;
import java.util.List;
import localsearch.HillClimbing;
import utils.EnvironmentParser;

public class Main {
    public static void main(String[] args) {
        EnvironmentParser environmentParser = new EnvironmentParser(new Time(0, 1, 0, 0));
        Environment environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");

        
        
        // HillClimbing hillClimbing = new HillClimbing();
        // Solution bestHillClimbingSolution = hillClimbing.run(environment, initialSolution);
        List<Order> originalorders = new ArrayList<>(environment.orders);
        environment.orders = originalorders.subList(0, 50);
        SolutionInitializer solutionInitializer = new SolutionInitializer();
        Solution initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults1.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 100);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults1.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 200);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults1.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 400);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults1.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 800);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults1.csv");
        }
        // System.out.println("For a sample of " + environment.orders.size() + " orders and " + environment.vehicles.size() + " vehicles:");
        // System.out.println("=== Hill Climbing Solution ===");
        // System.out.println(bestHillClimbingSolution.getReport());
        // System.out.println("\n=== Tabu Search Solution ===");
        
        /*int[] sampleSizes = {50, 100, 200, 400, 800};
        for (int size : sampleSizes) {
            // 3.1) Truncar pedidos
            environment.orders = originalorders.subList(0, Math.min(size, originalorders.size()));

            // 3.2) Generar solución inicial
            SolutionInitializer init = new SolutionInitializer();
            Solution initialSolution = init.generateInitialSolution(environment);

            // 3.3) Repetir 10 veces Tabu y HC
            for (int run = 0; run < 10; run++) {
                // —— Tabu Search ——
                TabuSearch ts = new TabuSearch();
                Solution bestTS = ts.run(environment, initialSolution.clone());
                bestTS.exportToCSV("main/tabuSearchResults1.csv");

                // —— Hill Climbing ——
                HillClimbing hc = new HillClimbing();
                Solution bestHC = hc.run(environment, initialSolution.clone());
                bestHC.fitness(environment);
                bestHC.exportToCSV("main/hillClimbingResults1.csv");
            }
        
        }*/
        System.out.println("¡Experimentos completados!");



    }
}
