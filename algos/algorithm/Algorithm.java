package algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import utils.SimulationProperties;

public class Algorithm {
    // Hyperparameters
    private static int maxTimeMs = 30 * 1000;
    private static int maxNoImprovement = 3000;
    private static int maxNoImprovementFeasible = 1000; 

    public Algorithm() {
    }

    public static Solution run(Environment environment, int minutes) {
        Solution bestSolution = null;
        Solution bestFeasibleSolution = null;
        double bestFitness = Double.NEGATIVE_INFINITY;
        double bestFeasibleFitness = Double.NEGATIVE_INFINITY;
        long startTime = System.currentTimeMillis();
        int feasibleSolutionsFound = 0;
        
        // Ensure we have at least one solution
        bestSolution = _run(environment, minutes);
        bestFitness = bestSolution.fitness(environment);
        if (bestSolution.isFeasible(environment)) {
            bestFeasibleSolution = bestSolution;
            bestFeasibleFitness = bestFitness;
            feasibleSolutionsFound++;
        }
        
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            Solution solution = _run(environment, minutes);
            double fitness = solution.fitness(environment);
            
            if (solution.isFeasible(environment)) {
                feasibleSolutionsFound++;
                if (fitness > bestFeasibleFitness) {
                    bestFeasibleFitness = fitness;
                    bestFeasibleSolution = solution;
                }
                if (feasibleSolutionsFound >= 3) {
                    return bestFeasibleSolution;
                }
            }
            
            if (fitness > bestFitness) {
                bestFitness = fitness;
                bestSolution = solution;
            }
        }
        
        // Always return a solution, preferring feasible ones
        return bestFeasibleSolution != null ? bestFeasibleSolution : bestSolution;
    }

    private static Solution _run(Environment environment, int minutes) {
        Solution currSolution = environment.getRandomSolution();
        Solution bestSolution = currSolution.clone();
        double bestFitness = bestSolution.fitness(environment);
        double currFitness = bestFitness;
        int iterations = 0;
        int noImprovementCount = 0;

        while (true) {
            boolean isFeasible = bestSolution.isFeasible(environment);

            // Check termination conditions
            if (isFeasible && noImprovementCount >= maxNoImprovementFeasible) {
                if (SimulationProperties.isDebug) {
                    System.out.println("\nBreaking: No improvement for " + noImprovementCount + " iterations while solution is feasible");
                }
                break;
            }
            if (!isFeasible && noImprovementCount >= maxNoImprovement) {
                if (SimulationProperties.isDebug) {
                    System.out.println("\nBreaking: No improvement for " + noImprovementCount + " iterations while solution is not feasible");
                }
                break;
            }

            List<Neighbor> neighborhood = NeighborhoodGenerator.generateNeighborhood(currSolution, environment);

            Neighbor bestNeighbor = null;
            double bestNeighborFitness = Double.NEGATIVE_INFINITY;

            // Find best neighbor (no tabu logic)
            for (Neighbor neighbor : neighborhood) {
                double neighborFitness = neighbor.solution.fitness(environment);
                if (neighborFitness > bestNeighborFitness) {
                    bestNeighbor = neighbor;
                    bestNeighborFitness = neighborFitness;
                }
            }

            if (bestNeighbor != null && bestNeighborFitness > currFitness) {
                currSolution = bestNeighbor.solution;
                currFitness = bestNeighborFitness;
                // Update best solution if improved
                boolean currFeasible = currSolution.isFeasible(environment);
                boolean bestFeasible = bestSolution.isFeasible(environment);
                if ((currFeasible && !bestFeasible) ||
                    (currFeasible == bestFeasible && currFitness > bestFitness)) {
                    bestSolution = currSolution.clone();
                    bestFitness = currFitness;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
            } else {
                // No improvement found, stop (hill climbing terminates at local optimum)
                if (SimulationProperties.isDebug) {
                    System.out.println("No improving neighbor found, terminating hill climbing at iteration " + iterations);
                }
                break;
            }

            if (SimulationProperties.isDebug && iterations % 100 == 0) {
                System.out.println("--------------------------------");
                System.out.println("Iteration " + iterations +
                        ": Current fitness: " + String.format("%.4f", currFitness) +
                        ", Best fitness: " + String.format("%.4f", bestFitness) +
                        ", currSolution Feasible: " + currSolution.isFeasible(environment) +
                        ", bestSolution Feasible: " + bestSolution.isFeasible(environment) +
                        ", No improvement: " + noImprovementCount);
            }

            iterations++;
        }

        bestSolution.compress();
        return bestSolution;
    }

    public static class Movement {
        public enum MovementType {
            INTRA_ROUTE_MOVE,    // Essential - relocate within route
            INTRA_ROUTE_SWAP,    // Essential - exchange within route
            INTRA_ROUTE_TWO_OPT, // Essential - local route optimization
            INTER_ROUTE_MOVE,    // Essential - relocate between routes
            INTER_ROUTE_SWAP,    // Essential - exchange between routes
            INTER_ROUTE_CROSS_EXCHANGE  // Essential - complex multi-point exchange
        }

        public MovementType movementType;
        public int vehicle1 = 0;
        public int vehicle2 = 0;
        public int nodeIdxFrom = 0;
        public int nodeIdxTo = 0;

        public Movement() {
        }

        public Movement(MovementType type) {
            this.movementType = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Movement movement = (Movement) obj;
            return vehicle1 == movement.vehicle1 &&
                   vehicle2 == movement.vehicle2 &&
                   nodeIdxFrom == movement.nodeIdxFrom &&
                   nodeIdxTo == movement.nodeIdxTo &&
                   movementType == movement.movementType;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(movementType, vehicle1, vehicle2, nodeIdxFrom, nodeIdxTo);
        }

        public Movement getReverseMovement() {
            Movement reverse = new Movement(this.movementType);
            switch (this.movementType) {
                case INTRA_ROUTE_MOVE:
                case INTRA_ROUTE_SWAP:
                case INTRA_ROUTE_TWO_OPT:
                    reverse.vehicle1 = this.vehicle1;
                    reverse.nodeIdxFrom = this.nodeIdxTo;
                    reverse.nodeIdxTo = this.nodeIdxFrom;
                    break;
                case INTER_ROUTE_MOVE:
                case INTER_ROUTE_SWAP:
                case INTER_ROUTE_CROSS_EXCHANGE:
                    reverse.vehicle1 = this.vehicle2;
                    reverse.vehicle2 = this.vehicle1;
                    reverse.nodeIdxFrom = this.nodeIdxTo;
                    reverse.nodeIdxTo = this.nodeIdxFrom;
                    break;
                default:
                    return null;
            }
            return reverse;
        }
    }

    private static class Neighbor {
        public Solution solution;
        public Movement movement;

        public Neighbor(Solution solution, Movement movement) {
            this.solution = solution;
            this.movement = movement;
        }
    }

    private static class NeighborhoodGenerator {
        private static final Random random = new Random();
        private static final int attemptsPerOperation = 10;
        private static final int neighborsPerOperator = 3;

        public static List<Neighbor> generateNeighborhood(Solution solution, Environment environment) {
            List<Neighbor> neighbors = new ArrayList<>();
            Solution trimmedSolution = solution.clone();

            Map<Integer, Node> startNodes = new HashMap<>();
            Map<Integer, Node> finalNodes = new HashMap<>();
            for (Map.Entry<Integer, List<Node>> entry : trimmedSolution.routes.entrySet()) {
                startNodes.put(entry.getKey(), entry.getValue().get(0).clone());
                finalNodes.put(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1).clone());
                entry.getValue().remove(0);
                entry.getValue().remove(entry.getValue().size() - 1);
            }

            for (Movement.MovementType operator : Movement.MovementType.values()) {
                for (int i = 0; i < neighborsPerOperator; i++) {
                    int attempts = 0;
                    Neighbor neighbor = null;
                    while (attempts < attemptsPerOperation && neighbor == null) {
                        switch (operator) {
                            case INTRA_ROUTE_MOVE:
                                neighbor = intraRouteMove(trimmedSolution);
                                break;
                            case INTRA_ROUTE_SWAP:
                                neighbor = intraRouteSwap(trimmedSolution);
                                break;
                            case INTRA_ROUTE_TWO_OPT:
                                neighbor = intraRouteTwoOpt(trimmedSolution);
                                break;
                            case INTER_ROUTE_MOVE:
                                neighbor = interRouteMove(trimmedSolution);
                                break;
                            case INTER_ROUTE_SWAP:
                                neighbor = interRouteSwap(trimmedSolution);
                                break;
                            case INTER_ROUTE_CROSS_EXCHANGE:
                                neighbor = interRouteCrossExchange(trimmedSolution);
                                break;
                        }
                        attempts++;
                    }

                    if (neighbor != null) {
                        for (Map.Entry<Integer, Node> entry : startNodes.entrySet()) {
                            neighbor.solution.routes.get(entry.getKey()).add(0, entry.getValue());
                        }
                        for (Map.Entry<Integer, Node> entry : finalNodes.entrySet()) {
                            neighbor.solution.routes.get(entry.getKey()).add(entry.getValue());
                        }
                        neighbors.add(neighbor);
                    }
                }
            }
            
            return neighbors;
        }

        private static Neighbor intraRouteMove(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            int routeSize = route.size();
            int indexFrom = random.nextInt(routeSize);
            int indexTo = random.nextInt(routeSize);

            Node nodeToMove = route.remove(indexFrom);
            if (indexFrom < indexTo) {
                route.add(indexTo - 1, nodeToMove);
            } else {
                route.add(indexTo, nodeToMove);
            }

            Movement movement = new Movement(Movement.MovementType.INTRA_ROUTE_MOVE);
            movement.vehicle1 = vehicleId;
            movement.nodeIdxFrom = indexFrom;
            movement.nodeIdxTo = indexTo;

            return new Neighbor(newSolution, movement);
        }

        private static Neighbor intraRouteSwap(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;
            
            if (newSolution.routes.get(vehicleId).size() < 2) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            int routeSize = route.size();
            int index1 = random.nextInt(routeSize);
            int index2 = random.nextInt(routeSize);
            while (index1 == index2) {
                index2 = random.nextInt(routeSize);
            }

            java.util.Collections.swap(route, index1, index2);

            Movement movement = new Movement(Movement.MovementType.INTRA_ROUTE_SWAP);
            movement.vehicle1 = vehicleId;
            movement.nodeIdxFrom = index1;
            movement.nodeIdxTo = index2;

            return new Neighbor(newSolution, movement);
        }

        private static Neighbor intraRouteTwoOpt(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            int routeSize = route.size();
            if (routeSize < 4) return null;

            int i = random.nextInt(routeSize - 2);
            int j = random.nextInt(routeSize - 1 - (i + 1)) + (i + 1);

            List<Node> segmentToReverse = route.subList(i + 1, j + 1);
            java.util.Collections.reverse(segmentToReverse);

            Movement movement = new Movement(Movement.MovementType.INTRA_ROUTE_TWO_OPT);
            movement.vehicle1 = vehicleId;
            movement.nodeIdxFrom = i + 1;
            movement.nodeIdxTo = j + 1;

            return new Neighbor(newSolution, movement);
        }

        private static Neighbor interRouteMove(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            int vehicleIdFrom = vehicleIds[0];
            int vehicleIdTo = vehicleIds[1];

            List<Node> routeFrom = newSolution.routes.get(vehicleIdFrom);
            List<Node> routeTo = newSolution.routes.get(vehicleIdTo);

            if (routeFrom.isEmpty()) return null;

            int indexFrom = random.nextInt(routeFrom.size());
            int indexTo = random.nextInt(routeTo.size() + 1);

            Node nodeToMove = routeFrom.remove(indexFrom);
            routeTo.add(indexTo, nodeToMove);

            Movement movement = new Movement(Movement.MovementType.INTER_ROUTE_MOVE);
            movement.vehicle1 = vehicleIdFrom;
            movement.vehicle2 = vehicleIdTo;
            movement.nodeIdxFrom = indexFrom;
            movement.nodeIdxTo = indexTo;

            return new Neighbor(newSolution, movement);
        }

        private static Neighbor interRouteSwap(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            int vehicleId1 = vehicleIds[0];
            int vehicleId2 = vehicleIds[1];

            List<Node> route1 = newSolution.routes.get(vehicleId1);
            List<Node> route2 = newSolution.routes.get(vehicleId2);

            if (route1.isEmpty() || route2.isEmpty()) return null;

            int index1 = random.nextInt(route1.size());
            int index2 = random.nextInt(route2.size());

            Node node1 = route1.get(index1);
            Node node2 = route2.get(index2);

            route1.set(index1, node2);
            route2.set(index2, node1);

            Movement movement = new Movement(Movement.MovementType.INTER_ROUTE_SWAP);
            movement.vehicle1 = vehicleId1;
            movement.vehicle2 = vehicleId2;
            movement.nodeIdxFrom = index1;
            movement.nodeIdxTo = index2;

            return new Neighbor(newSolution, movement);
        }

        private static Neighbor interRouteCrossExchange(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            int vehicleId1 = vehicleIds[0];
            int vehicleId2 = vehicleIds[1];

            List<Node> route1 = newSolution.routes.get(vehicleId1);
            List<Node> route2 = newSolution.routes.get(vehicleId2);

            if (route1.isEmpty() || route2.isEmpty()) return null;

            int i = random.nextInt(route1.size());
            int j = random.nextInt(route2.size());

            List<Node> tail1 = new ArrayList<>(route1.subList(i + 1, route1.size()));
            List<Node> tail2 = new ArrayList<>(route2.subList(j + 1, route2.size()));

            List<Node> newRoute1 = new ArrayList<>(route1.subList(0, i + 1));
            newRoute1.addAll(tail2);

            List<Node> newRoute2 = new ArrayList<>(route2.subList(0, j + 1));
            newRoute2.addAll(tail1);

            newSolution.routes.put(vehicleId1, newRoute1);
            newSolution.routes.put(vehicleId2, newRoute2);

            Movement movement = new Movement(Movement.MovementType.INTER_ROUTE_CROSS_EXCHANGE);
            movement.vehicle1 = vehicleId1;
            movement.vehicle2 = vehicleId2;
            movement.nodeIdxFrom = i;
            movement.nodeIdxTo = j;

            return new Neighbor(newSolution, movement);
        }

        private static int getRandomVehicleWithRoute(Solution solution) {
            List<Integer> vehicleIds = new ArrayList<>();
            for (Map.Entry<Integer, List<Node>> entry : solution.routes.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    vehicleIds.add(entry.getKey());
                }
            }
            
            if (vehicleIds.isEmpty()) return -1;
            return vehicleIds.get(random.nextInt(vehicleIds.size()));
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

            java.util.Collections.shuffle(vehicleIds, random);
            return new int[]{vehicleIds.get(0), vehicleIds.get(1)};
        }
    }
}
