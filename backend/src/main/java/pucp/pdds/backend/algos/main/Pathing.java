package pucp.pdds.backend.algos.main;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.PathVisualizer;

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

        // Run all cases and add them to collage
        runAllCasesForCollage();
        
        System.out.println("All cases completed and added to collage visualization.");
        
        // Keep the program running for a few seconds to allow viewing the collage
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runAllCasesForCollage() {
        // Clear any existing collage data
        PathVisualizer.clearCollage();
        
        // Run all cases and add to collage
        CaseA();
        System.out.println("--------------------------------");
        CaseB();
        System.out.println("--------------------------------");
        CaseC();
        System.out.println("--------------------------------");
        CaseD();
        System.out.println("--------------------------------");
        CaseE();
        System.out.println("--------------------------------");
        CaseF();
        System.out.println("--------------------------------");
        CaseG();
        System.out.println("--------------------------------");
        CaseH();
        System.out.println("--------------------------------");
        CaseI();
        System.out.println("--------------------------------");
        CaseJ();
        System.out.println("--------------------------------");
        CaseK();
        System.out.println("--------------------------------");
        CaseL();
        System.out.println("--------------------------------");
        CaseM();
        System.out.println("--------------------------------");
        CaseN();
        System.out.println("--------------------------------");
        CaseO();
        System.out.println("--------------------------------");
        CaseP();
        System.out.println("--------------------------------");
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
        
        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case A - Simple Path", start, end, visualPath, emptyBlockages);
        
        System.out.println("Ended Case A Simulation");
    }

    public static void CaseB() {
        Position start = new Position(0, 15);
        Position end = new Position(30, 15);

        List<PlannerBlockage> blockages = new ArrayList<>();
        blockages.add(new PlannerBlockage(0, startTime, endTime, new ArrayList<>(Arrays.asList(new Position(15, 0), new Position(15, 30)))));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case B - Path with Single Blockage", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case B - Single Blockage", start, end, visualPath, blockages);
        
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

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case C - Unreachable", start, end, visualPath, blockages);
        
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

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case D - Fortress", start, end, visualPath, blockages);
        
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

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case E - Corner Start", start, end, visualPath, blockages);
        
        System.out.println("Ended Case E Simulation");
    }

    public static void CaseF() {
        // Maze-like scenario with multiple valid paths
        Position start = new Position(5, 5);
        Position end = new Position(35, 35);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a maze pattern
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 0),
                new Position(15, 25)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 15),
                new Position(25, 40)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 25),
                new Position(35, 25)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case F - Maze Navigation", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case F - Maze", start, end, visualPath, blockages);
        
        System.out.println("Ended Case F Simulation");
    }

    public static void CaseG() {
        // Vertex-to-vertex movement scenario
        Position start = new Position(10, 10);
        Position end = new Position(30, 30);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a pattern where the optimal path should use blockage vertices
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 10),
                new Position(20, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 20),
                new Position(30, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 20),
                new Position(30, 30)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case G - Vertex-to-Vertex Path", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case G - Vertex Path", start, end, visualPath, blockages);
        
        System.out.println("Ended Case G Simulation");
    }

    public static void CaseH() {
        // Vertex-to-vertex movement scenario
        Position start = new Position(10, 10);
        Position end = new Position(30, 25);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a pattern where the optimal path should use blockage vertices
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 10),
                new Position(20, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 20),
                new Position(30, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 20),
                new Position(30, 30)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case H - Vertex-to-Vertex Path", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case H - Vertex Path", start, end, visualPath, blockages);
        
        System.out.println("Ended Case H Simulation");
    }

    public static void CaseI() {
        // Edge case: Path along grid boundaries
        Position start = new Position(0, 0);
        Position end = new Position(SimulationProperties.gridLength, SimulationProperties.gridWidth);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create blockages that force path along grid boundaries
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(0, 5),
                new Position(5, 5),
                new Position(5, 0)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(0, SimulationProperties.gridWidth - 5),
                new Position(SimulationProperties.gridLength - 5, SimulationProperties.gridWidth - 5)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case I - Grid Boundary Path", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case I - Boundary", start, end, visualPath, blockages);
        
        System.out.println("Ended Case I Simulation");
    }

    public static void CaseJ() {
        // End point caged inside a square blockage
        Position start = new Position(0, 0);
        Position end = new Position(30, 30);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a square blockage around the end point
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 25), // Top left
                new Position(35, 25), // Top right 
                new Position(35, 35), // Bottom right
                new Position(25, 35), // Bottom left
                new Position(25, 25)  // Back to start to close the square
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case J - End Point Caged in Square", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case J - Caged End", start, end, visualPath, blockages);
        
        System.out.println("Ended Case J Simulation");
    }

    public static void CaseK() {
        // Test case with decimal coordinates
        Position start = new Position(5.5, 5); // Start with decimal x
        Position end = new Position(20, 15.7); // End with decimal y

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Add some blockages to make the path more interesting
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(10, 0),
                new Position(10, 10)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case K - Decimal Coordinate Path", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case K - Decimal Coords", start, end, visualPath, blockages);
        
        System.out.println("Ended Case K Simulation");
    }

    public static void CaseL() {
        // Vertex-to-vertex movement scenario (swapped start/end)
        Position start = new Position(30, 25);
        Position end = new Position(10, 10);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a pattern where the optimal path should use blockage vertices
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 10),
                new Position(20, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 20),
                new Position(30, 20)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 20),
                new Position(30, 30)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case L - Vertex-to-Vertex Path (Swapped)", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case L - Vertex Path (Swapped)", start, end, visualPath, blockages);
        
        System.out.println("Ended Case L Simulation");
    }

    public static void CaseM() {
        // Zigzag maze requiring A* to find optimal path
        Position start = new Position(5.9, 5);
        Position end = new Position(35, 35);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a zigzag pattern of walls forcing an optimal path
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(10, 0),
                new Position(10, 25)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 15),
                new Position(20, 40)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 0),
                new Position(30, 25)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case M - Zigzag Maze (A* Optimal Path)", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case M - Zigzag", start, end, visualPath, blockages);
        
        System.out.println("Ended Case M Simulation");
    }

    public static void CaseN() {
        // Complex maze with multiple dead ends
        Position start = new Position(2.4, 2);
        Position end = new Position(38, 38);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a complex maze pattern with dead ends
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(10, 0),
                new Position(10, 30)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(10, 10),
                new Position(30, 10)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(20, 10),
                new Position(20, 30)
            ))
        ));
        
        blockages.add(new PlannerBlockage(3, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 20),
                new Position(30, 40)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case N - Complex Maze with Dead Ends (A* Navigation)", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case N - Complex Maze", start, end, visualPath, blockages);
        
        System.out.println("Ended Case N Simulation");
    }

    public static void CaseO() {
        // Spiral pattern requiring A* to navigate efficiently
        Position start = new Position(20, 20.5);
        Position end = new Position(5, 5);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a spiral pattern of blockages
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(10, 10),
                new Position(30, 10)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(30, 10),
                new Position(30, 30)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 30),
                new Position(30, 30)
            ))
        ));
        
        blockages.add(new PlannerBlockage(3, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 15),
                new Position(15, 30)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case O - Spiral Pattern (A* Efficiency Test)", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case O - Spiral", start, end, visualPath, blockages);
        
        System.out.println("Ended Case O Simulation");
    }

    public static void CaseP() {
        // Multiple equivalent paths requiring A* to choose optimal one
        Position start = new Position(5.8, 25);
        Position end = new Position(35, 25);

        List<PlannerBlockage> blockages = new ArrayList<>();
        
        // Create a pattern with multiple possible paths
        blockages.add(new PlannerBlockage(0, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 15),
                new Position(15, 23)
            ))
        ));
        
        blockages.add(new PlannerBlockage(1, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(15, 27),
                new Position(15, 35)
            ))
        ));
        
        blockages.add(new PlannerBlockage(2, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 15),
                new Position(25, 23)
            ))
        ));
        
        blockages.add(new PlannerBlockage(3, startTime, endTime,
            new ArrayList<>(Arrays.asList(
                new Position(25, 27),
                new Position(25, 35)
            ))
        ));

        List<Position> path = PathBuilder.buildPath(start, end, blockages);
        printPathInfo("Case P - Multiple Equivalent Paths (A* Optimal Choice)", start, end, path);

        // Add to collage instead of individual visualization
        List<Position> visualPath = (path != null) ? path : new ArrayList<>();
        PathVisualizer.addCaseToCollage("Case P - Multiple Paths", start, end, visualPath, blockages);
        
        System.out.println("Ended Case P Simulation");
    }
}