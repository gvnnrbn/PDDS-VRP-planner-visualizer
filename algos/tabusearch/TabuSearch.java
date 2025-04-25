package tabusearch;

import domain.Environment;
import domain.Node;
import domain.Solution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import tabusearch.Movement.MovementType;

public class TabuSearch {
    private static final Random random = new Random();

    private final int maxIterations;
    private final int tabuListSize;
    private final int maxNoImprovement;
    private final Set<MovementType> enabledOperators;
    
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final int DEFAULT_TABU_LIST_SIZE = 50;
    private static final int DEFAULT_MAX_NO_IMPROVEMENT = 100;
    private static final Set<MovementType> DEFAULT_ENABLED_OPERATORS = EnumSet.allOf(MovementType.class);

    public TabuSearch() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_TABU_LIST_SIZE, DEFAULT_MAX_NO_IMPROVEMENT, DEFAULT_ENABLED_OPERATORS);
    }

    public TabuSearch(int maxIterations, int tabuListSize, int maxNoImprovement, Set<MovementType> enabledOperators) {
        this.maxIterations = maxIterations;
        this.tabuListSize = tabuListSize;
        this.maxNoImprovement = maxNoImprovement;
        this.enabledOperators = new HashSet<>(enabledOperators);
    }

    public TabuSearch(int maxIterations, int tabuListSize, int maxNoImprovement) {
        this(maxIterations, tabuListSize, maxNoImprovement, DEFAULT_ENABLED_OPERATORS);
    }

    public Solution run(Environment environment, Solution initialSolution) {
        List<Movement> tabuList = new ArrayList<>();
        Solution currentSolution = initialSolution;
        Solution bestSolution = currentSolution.clone();
        int noImprovementCount = 0;
        int iteration = 0;

        while (iteration < maxIterations && noImprovementCount < maxNoImprovement) {
            List<Neighbor> neighborhood = generateNeighborhood(currentSolution, environment);
            List<Neighbor> candidates = new ArrayList<>();
            
            for (Neighbor neighbor : neighborhood) {
                boolean isTabu = tabuList.contains(neighbor.movement);
                int neighborFitness = neighbor.solution.fitness(environment);
                int bestFitness = bestSolution.fitness(environment);
                
                // Only consider feasible solutions
                if (neighbor.solution.isFeasible(environment) && 
                    (!isTabu || neighborFitness > bestFitness)) {
                    candidates.add(neighbor);
                }
            }
            
            Neighbor bestCandidate = selectBestCandidate(candidates, environment);
            
            if (bestCandidate != null) {
                currentSolution = bestCandidate.solution;
                int currentFitness = currentSolution.fitness(environment);
                int bestFitness = bestSolution.fitness(environment);
                
                if (currentFitness > bestFitness) {
                    bestSolution = currentSolution.clone();
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
                
                tabuList.add(bestCandidate.movement);
                if (tabuList.size() > tabuListSize) {
                    tabuList.remove(0);
                }
            }
            
            iteration++;
        }
        
        return bestSolution;
    }

    private static Neighbor selectBestCandidate(List<Neighbor> candidates, Environment environment) {
        if (candidates.isEmpty()) return null;
        
        Neighbor bestCandidate = candidates.get(0);
        for (Neighbor candidate : candidates) {
            if (candidate.solution.fitness(environment) > bestCandidate.solution.fitness(environment)) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    public List<Neighbor> generateNeighborhood(Solution solution, Environment environment) {
        List<Neighbor> neighborhood = new ArrayList<>();
        
        // Generate multiple neighbors for each operator
        int neighborsPerOperator = 3;
        
        for (int i = 0; i < neighborsPerOperator; i++) {
            Neighbor neighbor;
            if (enabledOperators.contains(MovementType.INTRA_ROUTE_MOVE) && 
                (neighbor = intraRouteMove(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
            if (enabledOperators.contains(MovementType.INTRA_ROUTE_SWAP) && 
                (neighbor = intraRouteSwap(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
            if (enabledOperators.contains(MovementType.INTRA_ROUTE_TWO_OPT) && 
                (neighbor = intraRouteTwoOpt(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
            if (enabledOperators.contains(MovementType.INTER_ROUTE_MOVE) && 
                (neighbor = interRouteMove(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
            if (enabledOperators.contains(MovementType.INTER_ROUTE_SWAP) && 
                (neighbor = interRouteSwap(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
            if (enabledOperators.contains(MovementType.INTER_ROUTE_CROSS_EXCHANGE) && 
                (neighbor = interRouteCrossExchange(solution)) != null && 
                neighbor.solution.isFeasible(environment)) neighborhood.add(neighbor);
        }

        return neighborhood;
    }

    private static int getRandomVehicleWithRoute(Solution solution) {
        List<Integer> vehicleIds = new ArrayList<>(solution.routes.keySet());
        Collections.shuffle(vehicleIds, random);
        for (int vehicleId : vehicleIds) {
            if (solution.routes.get(vehicleId) != null && !solution.routes.get(vehicleId).isEmpty()) {
                return vehicleId;
            }
        }
        return -1;
    }

    private static int[] getTwoDistinctRandomVehiclesWithRoutes(Solution solution) {
        List<Integer> vehicleIds = new ArrayList<>();
        for (Map.Entry<Integer, List<Node>> entry : solution.routes.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                vehicleIds.add(entry.getKey());
            }
        }

        if (vehicleIds.size() < 2) {
            return null;
        }

        Collections.shuffle(vehicleIds, random);
        return new int[]{vehicleIds.get(0), vehicleIds.get(1)};
    }

    public static Neighbor intraRouteMove(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        int routeSize = route.size();

        if (routeSize < 2) return null;

        int indexFrom = random.nextInt(routeSize);
        int indexTo = random.nextInt(routeSize);

        Node nodeToMove = route.remove(indexFrom);
        if (indexFrom < indexTo) {
            route.add(indexTo - 1, nodeToMove);
        } else {
            route.add(indexTo, nodeToMove);
        }

        Movement movement = new Movement(MovementType.INTRA_ROUTE_MOVE);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = indexFrom;
        movement.nodeIdxTo = indexTo;

        return new Neighbor(solution, movement);
    }

    public static Neighbor intraRouteSwap(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        int routeSize = route.size();

        if (routeSize < 2) return null;

        int index1 = random.nextInt(routeSize);
        int index2 = random.nextInt(routeSize);
        while (index1 == index2) {
            index2 = random.nextInt(routeSize);
        }

        Collections.swap(route, index1, index2);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_SWAP);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = index1;
        movement.nodeIdxTo = index2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor intraRouteTwoOpt(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        int routeSize = route.size();

        if (routeSize < 4) return null;

        int i = random.nextInt(routeSize - 2);
        int j = random.nextInt(routeSize - 1 - (i + 1)) + (i + 1);

        List<Node> segmentToReverse = route.subList(i + 1, j + 1);
        Collections.reverse(segmentToReverse);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_TWO_OPT);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = i + 1;
        movement.nodeIdxTo = j + 1;

        return new Neighbor(solution, movement);
    }

    public static Neighbor interRouteMove(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleIdFrom = vehicleIds[0];
        int vehicleIdTo = vehicleIds[1];

        List<Node> routeFrom = solution.routes.get(vehicleIdFrom);
        List<Node> routeTo = solution.routes.get(vehicleIdTo);

        if (routeFrom.isEmpty()) return null;

        int indexFrom = random.nextInt(routeFrom.size());
        int indexTo = random.nextInt(routeTo.size() + 1);

        Node nodeToMove = routeFrom.remove(indexFrom);
        routeTo.add(indexTo, nodeToMove);

        Movement movement = new Movement(MovementType.INTER_ROUTE_MOVE);
        movement.vehicle1 = vehicleIdFrom;
        movement.vehicle2 = vehicleIdTo;
        movement.nodeIdxFrom = indexFrom;
        movement.nodeIdxTo = indexTo;

        return new Neighbor(solution, movement);
    }

    public static Neighbor interRouteSwap(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        int index1 = random.nextInt(route1.size());
        int index2 = random.nextInt(route2.size());

        Node node1 = route1.get(index1);
        Node node2 = route2.get(index2);

        route1.set(index1, node2);
        route2.set(index2, node1);

        Movement movement = new Movement(MovementType.INTER_ROUTE_SWAP);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;
        movement.nodeIdxFrom = index1;
        movement.nodeIdxTo = index2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor interRouteCrossExchange(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        int i = random.nextInt(route1.size());
        int j = random.nextInt(route2.size());

        List<Node> tail1 = new ArrayList<>(route1.subList(i + 1, route1.size()));
        List<Node> tail2 = new ArrayList<>(route2.subList(j + 1, route2.size()));

        List<Node> newRoute1 = new ArrayList<>(route1.subList(0, i + 1));
        newRoute1.addAll(tail2);

        List<Node> newRoute2 = new ArrayList<>(route2.subList(0, j + 1));
        newRoute2.addAll(tail1);

        solution.routes.put(vehicleId1, newRoute1);
        solution.routes.put(vehicleId2, newRoute2);

        Movement movement = new Movement(MovementType.INTER_ROUTE_CROSS_EXCHANGE);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;
        movement.nodeIdxFrom = i + 1;
        movement.nodeIdxTo = j + 1;

        return new Neighbor(solution, movement);
    }
}
