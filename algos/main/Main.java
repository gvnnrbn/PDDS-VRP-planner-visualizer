package main;

import domain.Environment;
import domain.Time;
import domain.Solution;
import domain.SolutionInitializer;

import utils.EnvironmentParser;

import localsearch.HillClimbing;
import localsearch.TabuSearch;

public class Main {
    public static void main(String[] args) {
        EnvironmentParser environmentParser = new EnvironmentParser(new Time(0, 1, 0, 0));
        Environment environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");

        environment.orders = environment.orders.subList(0, 300);

        SolutionInitializer solutionInitializer = new SolutionInitializer();
        Solution initialSolution = solutionInitializer.generateInitialSolution(environment);

        HillClimbing hillClimbing = new HillClimbing();
        Solution bestHillClimbingSolution = hillClimbing.run(environment, initialSolution);

        TabuSearch tabuSearch = new TabuSearch();
        Solution bestTabuSearchSolution = tabuSearch.run(environment, initialSolution);

        System.out.println("For a sample of " + environment.orders.size() + " orders and " + environment.vehicles.size() + " vehicles:");
        System.out.println("=== Hill Climbing Solution ===");
        System.out.println(bestHillClimbingSolution.getReport());
        System.out.println("\n=== Tabu Search Solution ===");
        System.out.println(bestTabuSearchSolution.getReport());
    }
}
