package antcolonyoptimization;

import domain.Environment;
import domain.Solution;

import java.util.Map;

public class Ant {
    private Solution solution;

    public Solution constructSolution(Map<Integer, Map<Integer, Double>> pheromones, double alpha, double beta, Environment environment) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Solution getSolution() {
        return solution;
    }
}
