package algorithms;
import domain_environment.Environment;
import domain_environment.Solution;

public interface MetaheuristicAlgorithm {
    public Solution run(Environment environment);
}