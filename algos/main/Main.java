package main;

import domain.Environment;
import domain.Time;
import domain.Solution;
import domain.SolutionInitializer;

import utils.EnvironmentParser;

import tabusearch.TabuSearch;

public class Main {
    public static void main(String[] args) {
        EnvironmentParser environmentParser = new EnvironmentParser(new Time(0, 1, 0, 0));
        Environment environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");

        SolutionInitializer solutionInitializer = new SolutionInitializer();
        Solution bestInitialSolution = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < 100; i++) {
            Solution currentSolution = solutionInitializer.generateInitialSolution(environment);
            double currentFitness = currentSolution.fitness(environment);

            if (currentFitness > bestFitness) {
                bestFitness = currentFitness;
                bestInitialSolution = currentSolution;
            }
        }

        System.out.println("Best initial solution with fitness " + bestInitialSolution.fitness(environment) + ":");

        TabuSearch tabuSearch = new TabuSearch();
        Solution bestSolution = tabuSearch.run(environment, bestInitialSolution);
        
        double fitness = bestSolution.fitness(environment);
        
        System.out.printf("Best solution fitness: %.2f and is feasible: %s%n", fitness, bestSolution.isFeasible(environment));
    }
}
