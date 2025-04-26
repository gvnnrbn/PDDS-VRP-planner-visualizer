package antcolonyoptimization;

import domain.Environment;
import domain.FinalNode;
import domain.Vehicle;
import domain.Solution;
import domain.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntColonyOptimization {
    private final int maxIterations;
    private final int numAnts;
    private final double alpha;
    private final double beta;
    private final double evaporationRate;
    private final double initialPheromone;
    private final int maxNoImprovement;
    
    private static final int DEFAULT_MAX_ITERATIONS = 1_000_000;
    private static final int DEFAULT_NUM_ANTS = 50;
    private static final double DEFAULT_ALPHA = 1.0;
    private static final double DEFAULT_BETA = 2.0;
    private static final double DEFAULT_EVAPORATION_RATE = 0.5;
    private static final double DEFAULT_INITIAL_PHEROMONE = 1.0;
    private static final int DEFAULT_MAX_NO_IMPROVEMENT = 1_000;

    private Map<Integer, Map<Integer, Double>> pheromones; // pheromones from nodeId to nodeId: pheromones[from][to]
    private List<Ant> ants;

    public AntColonyOptimization() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_NUM_ANTS, DEFAULT_ALPHA, DEFAULT_BETA, 
             DEFAULT_EVAPORATION_RATE, DEFAULT_INITIAL_PHEROMONE, DEFAULT_MAX_NO_IMPROVEMENT);
    }

    public AntColonyOptimization(int maxIterations, int numAnts, double alpha, double beta, 
              double evaporationRate, double initialPheromone, int maxNoImprovement) {
        this.maxIterations = maxIterations;
        this.numAnts = numAnts;
        this.alpha = alpha;
        this.beta = beta;
        this.evaporationRate = evaporationRate;
        this.initialPheromone = initialPheromone;
        this.maxNoImprovement = maxNoImprovement;
    }

    public Solution run(Environment environment, Solution initialSolution) {
        initializePheromones(environment, initialSolution);
        Solution bestSolution = initialSolution;
        double bestFitness = initialSolution.fitness(environment);
        int noImprovementCount = 0;

        for (int iteration = 0; iteration < maxIterations && noImprovementCount < maxNoImprovement; iteration++) {
            // Create and run ants
            ants = new ArrayList<>();
            for (int i = 0; i < numAnts; i++) {
                Ant ant = new Ant(environment.getNodes());
                ant.constructSolution(pheromones, alpha, beta, environment);
                ants.add(ant);
            }

            // Update pheromones
            updatePheromones(environment, bestFitness);

            // Find best solution in this iteration
            boolean improved = false;
            for (Ant ant : ants) {
                double fitness = ant.getSolution().fitness(environment);
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = ant.getSolution().clone();
                    improved = true;
                }
            }
            
            if (improved) {
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }
        }

        return bestSolution;
    }

    private void initializePheromones(Environment environment, Solution initialSolution) {
        pheromones = new HashMap<>();

        // Initialize pheromones at initial value
        for (Node node : environment.getNodes()) {
            Map<Integer, Double> currentNodePheromones = new HashMap<>();
            for (Node otherNode : environment.getNodes()) {
                currentNodePheromones.put(otherNode.id, initialPheromone);
            }
            pheromones.put(node.id, currentNodePheromones);
        }

        // Deposit pheromones from the initial solution
        List<Vehicle> vehicles = new ArrayList<>(environment.vehicles);
        Collections.sort(vehicles, (v1, v2) -> v1.id() - v2.id());

        int currIdx = 1;

        while (!vehicles.isEmpty()) {
            // Create a copy of vehicles to iterate over
            List<Vehicle> vehiclesToProcess = new ArrayList<>(vehicles);
            for (Vehicle vehicle : vehiclesToProcess) {
                List<Node> route = initialSolution.routes.get(vehicle.id());
                Node currNode = route.get(currIdx);
                Node prevNode = route.get(currIdx - 1);

                pheromones.get(prevNode.id).put(currNode.id, pheromones.get(prevNode.id).get(currNode.id) + 1);

                if (currNode instanceof FinalNode) {
                    vehicles.remove(vehicle);
                }
            }
            currIdx++;
        }
    }

    private void updatePheromones(Environment environment, double initialFitness) {
        // Evaporate pheromones
        for (Node node : environment.getNodes()) {
            for (Node otherNode : environment.getNodes()) {
                pheromones.get(node.id).put(otherNode.id, pheromones.get(node.id).get(otherNode.id) * (1 - evaporationRate));
            }
        }
        
        // Deposit pheromones proportional to solution quality in round robin fashion
        for (Ant ant : ants) {
            Solution solution = ant.getSolution();

            // Round robin pheromone deposit
            List<Vehicle> vehicles = new ArrayList<>(environment.vehicles);
            Collections.sort(vehicles, (v1, v2) -> v1.id() - v2.id());

            int currIdx = 1;

            while (!vehicles.isEmpty()) {
                // Create a copy of vehicles to iterate over
                List<Vehicle> vehiclesToProcess = new ArrayList<>(vehicles);
                for (Vehicle vehicle : vehiclesToProcess) {
                    List<Node> route = solution.routes.get(vehicle.id());
                    Node currNode = route.get(currIdx);
                    Node prevNode = route.get(currIdx - 1);

                    pheromones.get(prevNode.id).put(currNode.id, pheromones.get(prevNode.id).get(currNode.id) + 1);

                    if (currNode instanceof FinalNode) {
                        vehicles.remove(vehicle);
                    }
                }
                currIdx++;
            }
        }
    }
}
