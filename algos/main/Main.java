package main;

import java.util.ArrayList;
import java.util.List;

import domain.Environment;
import domain.Time;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Order;

import utils.EnvironmentParser;

import localsearch.HillClimbing;
import localsearch.TabuSearch;

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
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 100);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 200);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 400);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
        }

        environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");
        environment.orders = originalorders.subList(0, 800);
        solutionInitializer = new SolutionInitializer();
        initialSolution = solutionInitializer.generateInitialSolution(environment);
        for(int i = 0; i < 10; i++) {
            HillClimbing algorithm = new HillClimbing();
            Solution bestTabuSearchSolution = algorithm.run(environment, initialSolution);
            // System.out.println(bestTabuSearchSolution.getReport());
            bestTabuSearchSolution.exportToCSV( "main/hillClimbingResults.csv");
        }
        // System.out.println("For a sample of " + environment.orders.size() + " orders and " + environment.vehicles.size() + " vehicles:");
        // System.out.println("=== Hill Climbing Solution ===");
        // System.out.println(bestHillClimbingSolution.getReport());
        // System.out.println("\n=== Tabu Search Solution ===");
    }
}
