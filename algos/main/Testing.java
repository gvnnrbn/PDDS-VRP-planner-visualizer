package main;

import algorithm.Environment;
import utils.EnvironmentParser;
import utils.Time;

public class Testing {
    public static void main(String[] args) {
        Time currTime = new Time(1, 1, 1, 0, 0);

        EnvironmentParser parser = new EnvironmentParser(currTime);
        Environment environment = parser.parseEnvironment("main/vehicles.csv", "main/orders.csv", "main/blockages.csv", "main/warehouses.csv");

        System.out.println(environment);
    }
}
