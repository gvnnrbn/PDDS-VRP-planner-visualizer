package main;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import entities.PlannerBlockage;
import utils.PathBuilder;
import utils.Position;
import utils.SimulationProperties;
import utils.Time;
import utils.PathVisualizer;

public class Pathing {
    static Time startTime = new Time(2025, 1, 1, 0, 0);
    static Time endTime = startTime.addMinutes(120);
    static List<PlannerBlockage> emptyBlockages = new ArrayList<>();
    
    public static void main(String[] args) {
        // redirect to results.txt
        try {
            PrintStream out = new PrintStream(new File("results.txt"));
            System.setOut(out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CaseA();
        System.out.println("\n" + "=".repeat(50) + "\n");
        CaseB();
        System.out.println("\n" + "=".repeat(50) + "\n");
        CaseC();
        System.out.println("\n" + "=".repeat(50) + "\n");
        CaseD();
        System.out.println("\n" + "=".repeat(50) + "\n");
        CaseE();
    }

    private static void printPathInfo(String caseName, Position start, Position end, List<Position> path) {
        System.out.println(caseName);
        System.out.println("Start Position: " + start);
        System.out.println("End Position: " + end);

        if (path == null) {
            System.out.println("Path: NO PATH FOUND - This could be due to:");
            System.out.println("  - Invalid start/end positions (null or out of bounds)");
            System.out.println("  - No possible path exists due to blockages");
            System.out.println("  - Start or end position is inside a blockage");
            System.out.println("Distance: UNREACHABLE");
        } else if (path.isEmpty()) {
            System.out.println("Path: EMPTY (Start and end positions are the same)");
            System.out.println("Distance: 0");
        } else {
            System.out.println("Path: " + path);
            System.out.println("Distance: " + PathBuilder.calculateDistance(path));
        }
    }

    public static void CaseA() {
        Position start = new Position(3, 10);
        Position end = new Position(30, 39);

        List<Position> path = PathBuilder.buildPath(start, end, emptyBlockages);
        printPathInfo("Case A - Simple Path", start, end, path);
        
        // Visualize the path with null handling
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.visualizePath(start, end, visualPath, emptyBlockages, SimulationProperties.gridLength, SimulationProperties.gridWidth);
        
        System.out.println("Ended Case A Simulation");
    }

    public static void CaseB() {
        Position start = new Position(0, 15);
        Position end = new Position(30, 15);

        List<PlannerBlockage> blockages = new ArrayList<>();
        blockages.add(new PlannerBlockage(0, startTime, endTime, new ArrayList<>(Arrays.asList(new Position(15, 0), new Position(15, 30)))));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case B - Path with Single Blockage", start, end, path);

        // Visualize the path with null handling
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.visualizePath(start, end, visualPath, blockages, SimulationProperties.gridLength, SimulationProperties.gridWidth);
        
        System.out.println("Ended Case B Simulation");
    }

    public static void CaseC() {
        Position start = new Position(5, 15);
        Position end = new Position(35, 15);

        // Create a wall of blockages that completely blocks the path
        List<PlannerBlockage> blockages = new ArrayList<>();
        // Vertical wall in the middle
        blockages.add(new PlannerBlockage(0, startTime, endTime, 
            new ArrayList<>(Arrays.asList(
                new Position(20, 0), 
                new Position(20, 39)
            ))
        ));
        // Horizontal walls to prevent going around
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(0, 0),
                new Position(39, 0)
            ))
        ));
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(0, 39),
                new Position(39, 39)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case C - Unreachable Route (Blocked by Walls)", start, end, path);

        // Visualize the path with null handling
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.visualizePath(start, end, visualPath, blockages, SimulationProperties.gridLength, SimulationProperties.gridWidth);
        
        System.out.println("Ended Case C Simulation");
    }

    public static void CaseD() {
        Position start = new Position(38, 20);
        Position end = new Position(30, 20);

        // Create a fortress around the end point with only one opening
        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Left wall of the fortress
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 10),
                new Position(25, 19)  // Gap at y=20
            ))
        ));
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 21),
                new Position(25, 30)
            ))
        ));

        // Top wall
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 10),
                new Position(35, 10)
            ))
        ));

        // Right wall
        blockages.add(new PlannerBlockage(3, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(35, 10),
                new Position(35, 30)
            ))
        ));

        // Bottom wall
        blockages.add(new PlannerBlockage(4, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 30),
                new Position(35, 30)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case D - Fortress with Single Entry Point", start, end, path);

        // Visualize the path with null handling
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.visualizePath(start, end, visualPath, blockages, SimulationProperties.gridLength, SimulationProperties.gridWidth);
        
        System.out.println("Ended Case D Simulation");
    }

    public static void CaseE() {
        Position start = new Position(0, 0);
        Position end = new Position(30, 30);
        
        List<PlannerBlockage> blockages = new ArrayList<>();
        blockages.add(new PlannerBlockage(0, startTime, endTime, new ArrayList<>(Arrays.asList(new Position(0, 1), new Position(0, 30)))));
        blockages.add(new PlannerBlockage(0, startTime, endTime, new ArrayList<>(Arrays.asList(new Position(1, 0), new Position(30, 0)))));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case E - Corner Start Position", start, end, path);

        // Visualize the path with null handling
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.visualizePath(start, end, visualPath, blockages, SimulationProperties.gridLength, SimulationProperties.gridWidth);
        
        System.out.println("Ended Case E Simulation");
    }
}