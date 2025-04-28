package main;

import domain.Environment;
import domain.Time;
import domain.Solution;
import domain.SolutionInitializer;

import utils.EnvironmentParser;

import localsearch.HillClimbing;

public class Main {
    public static void main(String[] args) {
        EnvironmentParser environmentParser = new EnvironmentParser(new Time(0, 1, 0, 0));
        Environment environment = environmentParser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");

        environment.orders = environment.orders.subList(0, 300);

        SolutionInitializer solutionInitializer = new SolutionInitializer();
        Solution initialSolution = solutionInitializer.generateInitialSolution(environment);

        HillClimbing hillClimbing = new HillClimbing();
        Solution bestSolution = hillClimbing.run(environment, initialSolution);

        System.out.println(bestSolution.toString());
    }
}
