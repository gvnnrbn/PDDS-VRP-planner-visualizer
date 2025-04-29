package localsearch;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import domain.Environment;
import domain.Solution;

public class HillClimbing {
    // Hyperparameters
    private final int maxIterations;
    private final int maxTimeMs;
    private final int maxNoImprovement;
    private final double acceptanceProbability;

    private final boolean isDebug;

    private static final Random random = new Random();

    // Default constructor with default values
    public HillClimbing() {
        this(10_000, 55 * 1000, 1000, 0.1, false);
    }

    // Parameterized constructor
    public HillClimbing(int maxIterations, int maxTimeMs, int maxNoImprovement, double acceptanceProbability, boolean isDebug) {
        this.maxIterations = maxIterations;
        this.maxTimeMs = maxTimeMs;
        this.maxNoImprovement = maxNoImprovement;
        this.acceptanceProbability = acceptanceProbability;
        this.isDebug = isDebug;
    }

    public Solution run(Environment environment, Solution initialSolution) {
        Solution currBestSolution = initialSolution;
        Solution currSolution = initialSolution.clone(); // Track current solution separately

        long startTime = System.currentTimeMillis();
        int iterations = 0;
        int noImprovementCount = 0;
        double bestFitness = currBestSolution.fitness(environment);

        while (iterations < maxIterations && 
               (System.currentTimeMillis() - startTime) < maxTimeMs &&
               noImprovementCount < maxNoImprovement) {
            
            List<Neighbor> neighborhood = NeighborhoodGenerator.generateNeighborhood(currSolution, environment);
            Neighbor bestNeighbor = Collections.max(neighborhood, 
                (n1, n2) -> Double.compare(n1.solution.fitness(environment), n2.solution.fitness(environment)));

            double currFitness = currSolution.fitness(environment);
            double newFitness = bestNeighbor.solution.fitness(environment);

            // Accept better solution
            if (newFitness > currFitness) {
                currSolution = bestNeighbor.solution;
                noImprovementCount = 0;
                
                // Update best solution if better
                if (newFitness > bestFitness) {
                    currBestSolution = bestNeighbor.solution.clone();
                    bestFitness = newFitness;
                }
            } 
            // Accept worse solution with some probability (simulated annealing-like)
            else if (random.nextDouble() < acceptanceProbability) {
                currSolution = bestNeighbor.solution;
            }

            if (isDebug && iterations % 100 == 0) {
                long timePassed = System.currentTimeMillis() - startTime;
                System.out.println("Iteration " + iterations + 
                    ": Current fitness: " + currFitness + 
                    ", Best fitness: " + bestFitness + 
                    ", Feasible: " + currSolution.isFeasible(environment) + 
                    ", Time passed: " + timePassed + "ms");
            }

            iterations++;
            noImprovementCount++;
        }

        return currSolution;
    }

}
