package algorithms;
import domain_environment.Environment;
import domain_environment.Solution;
import java.util.ArrayList;
import java.util.List;

public class TabuSearch implements MetaheuristicAlgorithm {


    private SolutionInitializer solutionInitializer;
    
    public TabuSearch(SolutionInitializer solutionInitializer) {
        this.solutionInitializer = solutionInitializer;
    }

    @Override
    public Solution run(Environment environment) {
        // Inicializar parámetros: tamaño_lista, mejor_solución, lista_tabú, sol_actual 
        final int MAX_ITERATIONS = 1000;
        final int TABU_LIST_SIZE = 50;
        final int MAX_NO_IMPROVEMENT = 100;
        final List<Move> tabuList = new ArrayList<>();
        Solution currentSolution = solutionInitializer.initializeSolution(environment);
        Solution bestSolution = currentSolution.clone();

        int noImprovementCount = 0;
        int iteration = 0;

        // Hasta cumplir condición de parada 
        while (iteration < MAX_ITERATIONS && noImprovementCount < MAX_NO_IMPROVEMENT) {

            // 	vecindario = generar_vecindario(sol_actual)
            List<Solution> neighborhood = generateNeighborhood(currentSolution);
            
            // 	candidatos = Lista vacía
            List<Solution> candidates = new ArrayList<>();

            // Por cada vecino en vecindario:
             for (Solution neighbor : neighborhood) {
                Move move = getMove(currentSolution, neighbor);
                boolean isTabu = tabuList.contains(move);
                int neighborFitness = neighbor.calculateFitness(environment);
                
                // Si (movimiento(sol_actual, vecino) no está en lista_tabú) o (evaluar(vecino) > evaluar(mejor_solución)):
                if (!isTabu || neighborFitness > bestSolution.calculateFitness(environment)) {
                    // candidatos.agregar(vecino)
                    candidates.add(neighbor);
                }
            }
            
            // 	mejor_candidato = seleccionar_mejor(candidatos)
            Solution bestCandidate = selectBestCandidate(candidates, environment);
            
            if (bestCandidate != null) {

                //sol_actual = mejor_candidato
                currentSolution = bestCandidate;
                
                // 	Si mejor_candidato > mejor_solución:
                if (bestCandidate.calculateFitness(environment) > bestSolution.calculateFitness(environment)) {
                    // mejor_solución = mejor_candidato
                    bestSolution = bestCandidate.clone();
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
                
                // 	lista_tabú.agregar(mejor_candidato)
                Move move = getMove(currentSolution, bestCandidate);
                tabuList.add(move);
                if (tabuList.size() > TABU_LIST_SIZE) {
                    // 	lista_tabú.eliminar_primero()   
                    tabuList.remove(0);
                }
            }
            
            iteration++;
        }
        
        return bestSolution;
    }
    
    private List<Solution> generateNeighborhood(Solution solution) {
        // Generate neighborhood using 2-opt
        throw new UnsupportedOperationException("Not implemented");
    }
    
    private Solution selectBestCandidate(List<Solution> candidates, Environment environment) {
        if (candidates.isEmpty()) return null;
        
        Solution best = candidates.get(0);
        int bestFitness = best.calculateFitness(environment);
        
        for (int i = 1; i < candidates.size(); i++) {
            int currentFitness = candidates.get(i).calculateFitness(environment);
            if (currentFitness > bestFitness) {
                best = candidates.get(i);
                bestFitness = currentFitness;
            }
        }
        
        return best;
    }

    private Move getMove(Solution current, Solution neighbor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private class Move {
    }
}
