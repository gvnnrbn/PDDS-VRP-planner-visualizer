package main;

import algorithms.TabuSearch;
import domain.Blockage;
import domain.Environment;
import domain.Order;
import domain.Position;
import domain.Solution;
import domain.Vehicle;
import domain.Warehouse;
import algorithms.RandomSolutionInitializer;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Environment environment = new Environment();

        environment.vehicles = new ArrayList<>();
        Vehicle vehicle = new Vehicle();
        vehicle.weight = 2.5;
        vehicle.maxFuel = 100;
        vehicle.currentFuel = 50;
        vehicle.maxGLP = 100;
        vehicle.currentGLP = 50;
        vehicle.position = new Position(20, 20);
        environment.vehicles.add(vehicle);

        environment.orders = new ArrayList<>();
        Order order = new Order();
        order.position = new Position(10, 10);
        order.GLPRequired = 10;
        environment.orders.add(order);

        environment.warehouses = new ArrayList<>();
        Warehouse warehouse = new Warehouse();
        warehouse.position = new Position(20, 20);
        environment.warehouses.add(warehouse);

        environment.blockages = new ArrayList<>();
        Blockage blockage = new Blockage();
        blockage.positions = new ArrayList<>();
        blockage.positions.add(new Position(15, 15));
        blockage.positions.add(new Position(15, 25));
        blockage.positions.add(new Position(25, 25));
        environment.blockages.add(blockage);

        environment.gridLength = 30;
        environment.gridWidth = 30;

        environment.vehicleSpeed = 1;
        environment.GLPWeightPerm3 = 0.5;

        environment.dischargeSpeed = 1;
        environment.chargeSpeed = 1;
        environment.transferSpeed = 1;
        environment.fuelLoadSpeed = 1;

        TabuSearch tabuSearch = new TabuSearch(new RandomSolutionInitializer());

        Solution solution = tabuSearch.run(environment);

        System.out.println(solution);
    }
}
