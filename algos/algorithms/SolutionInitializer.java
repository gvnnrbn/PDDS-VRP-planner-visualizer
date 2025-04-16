package algorithms;
import domain_environment.Environment;
import domain_environment.Solution;

public interface SolutionInitializer {
    public Solution initializeSolution(Environment environment);
}
