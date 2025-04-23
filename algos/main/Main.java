package main;

import domain.Blockage;
import domain.Environment;
import domain.Order;
import domain.Position;
import domain.Vehicle;
import domain.Warehouse;
import domain.Time;
import domain.Solution;

import java.util.ArrayList;
import java.util.List;

import tabusearch.TabuSearch;
import antcolonyoptimization.AntColonyOptimization;

public class Main {
    public static void main(String[] args) {
        Environment environment = new Environment();

        environment.vehicles = new ArrayList<>();
        Vehicle vehicle = new Vehicle(1, 1000, 100, 100.0, 20, 0, new Position(0, 0));
        environment.vehicles.add(vehicle);

        environment.orders = new ArrayList<>();
        Order order = new Order(1, 10, new Position(10, 10), new Time(0, 0, 0, 0));
        environment.orders.add(order);

        environment.warehouses = new ArrayList<>();
        Warehouse warehouse = new Warehouse(1, new Position(20, 20), 100, 200, true);
        environment.warehouses.add(warehouse);

        environment.blockages = new ArrayList<>();
        List<Position> vertices = new ArrayList<>();
        vertices.add(new Position(15, 15));
        vertices.add(new Position(15, 25));
        vertices.add(new Position(25, 25));
        Blockage blockage = new Blockage(vertices);
        environment.blockages.add(blockage);

        System.out.println("Environment:");
        System.out.println(environment);

        Solution initialSolution = environment.generateInitialSolution();
        System.out.println("Initial solution with fitness " + initialSolution.fitness(environment) + ":");
        System.out.println(initialSolution);

        TabuSearch tabuSearch = new TabuSearch();
        Solution solution = tabuSearch.run(environment, initialSolution);

        System.out.println("Final solution by Tabu Search with fitness " + solution.fitness(environment) + ":");
        System.out.println(solution);

        AntColonyOptimization antColonyOptimization = new AntColonyOptimization();
        Solution antColonySolution = antColonyOptimization.run(environment, initialSolution);

        System.out.println("Final solution by Ant Colony Optimization with fitness " + antColonySolution.fitness(environment) + ":");
        System.out.println(antColonySolution);
    }
}
