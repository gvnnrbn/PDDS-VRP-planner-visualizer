package localsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import domain.Environment;
import domain.Solution;

public class TabuSearch {
    // Hyperparameters
    private final int maxIterations;
    private final int maxTimeMs;
    private final int maxNoImprovement;
    private final int tabuListSize;
    private final double aspirationCriteria;
    private final boolean isDebug;

    public TabuSearch(int maxIterations, int maxTimeMs, int maxNoImprovement,
            int tabuListSize, double aspirationCriteria, boolean isDebug) {
        this.maxIterations = maxIterations;
        this.maxTimeMs = maxTimeMs;
        this.maxNoImprovement = maxNoImprovement;
        this.tabuListSize = tabuListSize;
        this.aspirationCriteria = aspirationCriteria;
        this.isDebug = isDebug;
    }

    public TabuSearch() {
        this(200, 10 * 1000, 100, 50, 0.1, true);
    }

    public Solution run(Environment environment, Solution initialSolution) {
        Solution currSolution = initialSolution;
        Solution bestSolution = currSolution.clone();
        double bestFitness = bestSolution.fitness(environment);
        double currFitness = bestFitness;

        List<Movement> tabuList = new ArrayList<>();
        Map<Movement, Integer> tabuTenure = new HashMap<>();
        int iterations = 0;
        int noImprovementCount = 0;
        long startTime = System.currentTimeMillis();

        while (iterations < maxIterations &&
                System.currentTimeMillis() - startTime < maxTimeMs &&
                noImprovementCount < maxNoImprovement) {

            List<Neighbor> neighborhood = NeighborhoodGenerator.generateNeighborhood(currSolution, environment);
            Neighbor bestNeighbor = null;
            double bestNeighborFitness = Double.NEGATIVE_INFINITY;

            for (Neighbor neighbor : neighborhood) {
                double neighborFitness = neighbor.solution.fitness(environment);

                // Check if movement is in tabu list
                boolean isTabu = tabuList.contains(neighbor.movement);

                // Aspiration criteria: accept tabu move if it improves the best solution
                boolean satisfiesAspiration = neighborFitness > bestFitness * (1 + aspirationCriteria);

                if ((!isTabu || satisfiesAspiration) && neighborFitness > bestNeighborFitness) {
                    bestNeighbor = neighbor;
                    bestNeighborFitness = neighborFitness;
                }
            }

            if (bestNeighbor != null) {
                currSolution = bestNeighbor.solution;
                currFitness = bestNeighborFitness;

                // Update tabu list
                tabuList.add(bestNeighbor.movement);
                tabuTenure.put(bestNeighbor.movement, iterations);

                // Remove old tabu moves
                while (tabuList.size() > tabuListSize) {
                    Movement oldestMove = tabuList.remove(0);
                    tabuTenure.remove(oldestMove);
                }

                // Update best solution
                if (currFitness > bestFitness) {
                    bestSolution = currSolution.clone();
                    bestFitness = currFitness;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
            }

            if (isDebug && iterations % 100 == 0) {
                long timePassed = System.currentTimeMillis() - startTime;
                System.out.println("Iteration " + iterations +
                        ": Current fitness: " + currFitness +
                        ", Best fitness: " + bestFitness +
                        ", Tabu list size: " + tabuList.size() +
                        ", Time passed: " + timePassed + "ms");
            }

            iterations++;
        }

        return currSolution;
    }
}
