package antcolonyoptimization;

import domain.Environment;
import domain.Solution;
import domain.Node;

import java.util.ArrayList;
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
    
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final int DEFAULT_NUM_ANTS = 50;
    private static final double DEFAULT_ALPHA = 1.0;
    private static final double DEFAULT_BETA = 2.0;
    private static final double DEFAULT_EVAPORATION_RATE = 0.5;
    private static final double DEFAULT_INITIAL_PHEROMONE = 1.0;

    private Map<Integer, Map<Integer, Double>> pheromones; // pheromones from nodeId to nodeId: pheromones[from][to]
    private List<Ant> ants;

    public AntColonyOptimization() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_NUM_ANTS, DEFAULT_ALPHA, DEFAULT_BETA, 
             DEFAULT_EVAPORATION_RATE, DEFAULT_INITIAL_PHEROMONE);
    }

    public AntColonyOptimization(int maxIterations, int numAnts, double alpha, double beta, 
              double evaporationRate, double initialPheromone) {
        this.maxIterations = maxIterations;
        this.numAnts = numAnts;
        this.alpha = alpha;
        this.beta = beta;
        this.evaporationRate = evaporationRate;
        this.initialPheromone = initialPheromone;
    }

    public Solution run(Environment environment) {
        initializePheromones(environment);
        Solution bestSolution = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Create and run ants
            ants = new ArrayList<>();
            for (int i = 0; i < numAnts; i++) {
                Ant ant = new Ant();
                ant.constructSolution(pheromones, alpha, beta, environment);
                ants.add(ant);
            }

            // Update pheromones
            updatePheromones(environment);

            // Find best solution in this iteration
            for (Ant ant : ants) {
                double fitness = ant.getSolution().fitness(environment);
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = ant.getSolution().clone();
                }
            }
        }

        return bestSolution;
    }

    private void initializePheromones(Environment environment) {
        pheromones = new HashMap<>();
        for (Node node : environment.getNodes()) {
            Map<Integer, Double> currentNodePheromones = new HashMap<>();
            for (Node otherNode : environment.getNodes()) {
                currentNodePheromones.put(otherNode.id, initialPheromone);
            }
            pheromones.put(node.id, currentNodePheromones);
        }
    }

    private void updatePheromones(Environment environment) {
        // Evaporate pheromones
        for (Node node : environment.getNodes()) {
            for (Node otherNode : environment.getNodes()) {
                pheromones.get(node.id).put(otherNode.id, pheromones.get(node.id).get(otherNode.id) * (1 - evaporationRate));
            }
        }
        
        // Deposit pheromones proportional to solution quality
        for (Ant ant : ants) {
            Solution solution = ant.getSolution();
            for (int i = 0; i < solution.routes.size(); i++) {
                List<Node> route = solution.routes.get(i);
                for (int j = 0; j < route.size() - 1; j++) {
                    Node node = route.get(j);
                    Node nextNode = route.get(j + 1);
                    double currentPheromone = pheromones.get(node.id).get(nextNode.id);
                    pheromones.get(node.id).put(nextNode.id, currentPheromone + solution.fitness(environment));
                }
            }
        }
    }
}
