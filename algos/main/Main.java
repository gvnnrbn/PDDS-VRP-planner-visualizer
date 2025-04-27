package main;

import domain.Environment;
import domain.Time;
import domain.Solution;
import domain.SolutionInitializer;

import utils.EnvironmentParser;

import tabusearch.TabuSearch;
import antcolonyoptimization.AntColonyOptimization;

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

        environment.reportGeneratedNodes();

        System.out.println("Best initial solution with fitness " + bestInitialSolution.fitness(environment) + ":");
        System.out.println(bestInitialSolution.toString());

        TabuSearch tabuSearch = new TabuSearch();
        Solution bestSolution = tabuSearch.run(environment, bestInitialSolution);
        
        double fitness = bestSolution.fitness(environment);
        
        System.out.printf("Best solution fitness: %.2f and is feasible: %s%n", fitness, bestSolution.isFeasible(environment));
        System.out.println(bestSolution.toString());

        AntColonyOptimization antColonyOptimization = new AntColonyOptimization();
        Solution antColonySolution = antColonyOptimization.run(environment, bestSolution);

        System.out.println("Final solution by Ant Colony Optimization with fitness " + antColonySolution.fitness(environment) + " and feasible: " + antColonySolution.isFeasible(environment));
        System.out.println(antColonySolution);
    }
}
