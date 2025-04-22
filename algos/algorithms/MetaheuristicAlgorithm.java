package algorithms;
import domain.Environment;
import domain.Solution;

public interface MetaheuristicAlgorithm {
    public Solution run(Environment environment);
}