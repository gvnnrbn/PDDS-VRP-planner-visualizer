package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Algorithm {
    // Hyperparameters
    private static final int maxIterations = 100_000;
    private static final int maxTimeMs = 55 * 10000;
    private static final int maxNoImprovement = 1_000;
    private static final int maxNoImprovementFeasible = 400;
    private static final double baseAcceptanceProbability = 0.5;
    private static final double maxAcceptanceProbability = 0.9;
    private static final double minAcceptanceProbability = 0.1;
    private static final double temperatureDecay = 0.99;
    private static final double minImprovementThreshold = 0.0001; // Threshold for considering an improvement "significant"

    private static final boolean isDebug = true;

    private static final Random random = new Random();

    // Default constructor with default values
    public Algorithm() {
    }

    public static Solution run(Environment environment, int minutes) {
        Solution currBestSolution = environment.getRandomSolution();
        Solution currSolution = currBestSolution.clone();

        long startTime = System.currentTimeMillis();
        int iterations = 0;
        int noImprovementCount = 0;
        double lastBestFitness = Double.NEGATIVE_INFINITY;
        int resetCount = 0;
        final int MAX_RESETS = 3;
        double temperature = 1.0;

        currBestSolution.simulate(environment, minutes);
        currSolution.simulate(environment, minutes);
        double bestFitness = currBestSolution.fitness(environment);
        lastBestFitness = bestFitness;
        double initialFitness = bestFitness;

        while (iterations < maxIterations && 
               (System.currentTimeMillis() - startTime) < maxTimeMs) {
            
            currBestSolution.simulate(environment, minutes);
            boolean isFeasible = currBestSolution.isFeasible(environment);
            
            // Check termination or reset conditions
            if (isFeasible && noImprovementCount >= maxNoImprovementFeasible) {
                break;
            } else if (!isFeasible && noImprovementCount >= maxNoImprovement) {
                if (resetCount < MAX_RESETS) {
                    if (isDebug) {
                        System.out.println("Performing random reset " + (resetCount + 1) + " of " + MAX_RESETS + " after " + noImprovementCount + " iterations without improvement");
                    }
                    Solution newSolution = environment.getRandomSolution();
                    newSolution.simulate(environment, minutes);
                    
                    if (isDebug) {
                        System.out.println("Reset accepted with fitness: " + newSolution.fitness(environment));
                    }
                    
                    currSolution = newSolution;
                    noImprovementCount = 0;
                    resetCount++;
                    temperature = 1.0; // Reset temperature after each reset
                } else {
                    break;
                }
            }

            // Calculate dynamic acceptance probability based on stagnation and temperature
            double stagnationFactor = Math.min(1.0, noImprovementCount / 1000.0);
            double acceptanceProbability = Math.max(
                minAcceptanceProbability,
                Math.min(
                    maxAcceptanceProbability,
                    baseAcceptanceProbability + (baseAcceptanceProbability * stagnationFactor)
                ) * temperature
            );

            List<Neighbor> neighborhood = NeighborhoodGenerator.generateNeighborhood(currSolution, environment);
            for (Neighbor neighbor : neighborhood) {
                neighbor.solution.simulate(environment, minutes);
            }
            
            // Sort neighbors by fitness and try top 3
            Collections.sort(neighborhood, 
                (n1, n2) -> Double.compare(n2.solution.fitness(environment), n1.solution.fitness(environment)));
            
            boolean improved = false;
            double currFitness = currSolution.fitness(environment);
            
            // Try top 3 neighbors
            for (int i = 0; i < Math.min(3, neighborhood.size()); i++) {
                Neighbor candidate = neighborhood.get(i);
                double newFitness = candidate.solution.fitness(environment);
                
                if (newFitness > currFitness) {
                    currSolution = candidate.solution;
                    if (newFitness > bestFitness) {
                        double improvement = (newFitness - lastBestFitness) / Math.abs(lastBestFitness);
                        if (improvement > minImprovementThreshold) {
                            noImprovementCount = 0;
                            lastBestFitness = newFitness;
                        } else {
                            noImprovementCount++;
                        }
                        currBestSolution = candidate.solution.clone();
                        bestFitness = newFitness;
                    } else {
                        noImprovementCount++;
                    }
                    improved = true;
                    break;
                }
            }
            
            // If no improvement found, consider accepting a worse solution
            if (!improved) {
                Neighbor bestNeighbor = neighborhood.get(0);
                double newFitness = bestNeighbor.solution.fitness(environment);
                
                // Calculate acceptance probability based on fitness difference
                double fitnessDiff = (newFitness - currFitness) / Math.abs(initialFitness);
                double acceptanceProb = acceptanceProbability * Math.exp(fitnessDiff / temperature);
                
                if (random.nextDouble() < acceptanceProb) {
                    currSolution = bestNeighbor.solution;
                }
                noImprovementCount++;
            }

            // Update temperature
            temperature *= temperatureDecay;

            if (isDebug && iterations % 100 == 0) {
                long timePassed = System.currentTimeMillis() - startTime;
                System.out.println("Iteration " + iterations + 
                    ": Current fitness: " + currFitness + 
                    ", Best fitness: " + bestFitness + 
                    ", Feasible: " + currSolution.isFeasible(environment) + 
                    ", No improvement count: " + noImprovementCount +
                    ", Temperature: " + String.format("%.4f", temperature) +
                    ", Accept prob: " + String.format("%.4f", acceptanceProbability) +
                    ", Time passed: " + timePassed + "ms");
            }

            iterations++;
        }

        currBestSolution.compress();

        return currBestSolution;
    }

    public static class Movement {
        public enum MovementType {
            INTRA_ROUTE_MOVE,
            INTRA_ROUTE_SWAP,
            INTRA_ROUTE_TWO_OPT,
            INTER_ROUTE_MOVE,
            INTER_ROUTE_SWAP,
            INTER_ROUTE_CROSS_EXCHANGE,
            VEHICLE_SWAP,
            INTRA_ROUTE_REVERSE,
            INTER_ROUTE_REVERSE,
            ROUTE_SPLIT,
            ROUTE_MERGE,
            ROUTE_REVERSE,
            ROUTE_SHUFFLE,
            MULTI_ROUTE_MERGE,
            ROUTE_SPLIT_MULTI,
            NODE_RELOCATION,
            ROUTE_EXCHANGE,
            ROUTE_ROTATION
        }

        public MovementType movementType;
        public int vehicle1 = 0;
        public int vehicle2 = 0;
        public int nodeIdxFrom = 0;
        public int nodeIdxTo = 0;

        public Movement() {
        }

        public Movement(MovementType movementType) {
            this.movementType = movementType;
        }

        public Movement getReverseMovement() {
            Movement reverse = new Movement(this.movementType);
            switch (this.movementType) {
                case INTRA_ROUTE_MOVE:
                case INTRA_ROUTE_SWAP:
                case INTRA_ROUTE_TWO_OPT:
                case INTRA_ROUTE_REVERSE:
                    reverse.vehicle1 = this.vehicle1;
                    reverse.nodeIdxFrom = this.nodeIdxTo;
                    reverse.nodeIdxTo = this.nodeIdxFrom;
                    break;
                case INTER_ROUTE_MOVE:
                case INTER_ROUTE_SWAP:
                case INTER_ROUTE_CROSS_EXCHANGE:
                case INTER_ROUTE_REVERSE:
                case ROUTE_SPLIT:
                case ROUTE_MERGE:
                    reverse.vehicle1 = this.vehicle2;
                    reverse.vehicle2 = this.vehicle1;
                    reverse.nodeIdxFrom = this.nodeIdxTo;
                    reverse.nodeIdxTo = this.nodeIdxFrom;
                    break;
                case VEHICLE_SWAP:
                    reverse.vehicle1 = this.vehicle2;
                    reverse.vehicle2 = this.vehicle1;
                    break;
                default:
                    return null;
            }
            return reverse;
        }

        public boolean isReverseMovement(Movement movement) {
            if (this.movementType != movement.movementType) {
                return false;
            }

            switch (this.movementType) {
                case INTRA_ROUTE_MOVE:
                case INTRA_ROUTE_SWAP:
                case INTRA_ROUTE_TWO_OPT:
                case INTRA_ROUTE_REVERSE:
                    return this.vehicle1 == movement.vehicle1 && 
                           this.nodeIdxFrom == movement.nodeIdxTo && 
                           this.nodeIdxTo == movement.nodeIdxFrom;
                case INTER_ROUTE_MOVE:
                case INTER_ROUTE_SWAP:
                case INTER_ROUTE_CROSS_EXCHANGE:
                case INTER_ROUTE_REVERSE:
                case ROUTE_SPLIT:
                case ROUTE_MERGE:
                    return this.vehicle1 == movement.vehicle2 && 
                           this.vehicle2 == movement.vehicle1 && 
                           this.nodeIdxFrom == movement.nodeIdxTo && 
                           this.nodeIdxTo == movement.nodeIdxFrom;
                case VEHICLE_SWAP:
                    return this.vehicle1 == movement.vehicle2 && 
                           this.vehicle2 == movement.vehicle1;
                default:
                    return false;
            }
        }
    }

    private static class NeighborhoodGenerator {

        private static final Random random = new Random();

        // Auxiliary parameters
        private static final int attemptsPerOperation = 10;
        private static final int neighborsPerOperator = 10;

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
                            case VEHICLE_SWAP:
                                neighbor = vehicleSwap(trimmedSolution);
                                break;
                            case INTRA_ROUTE_REVERSE:
                                neighbor = intraRouteReverse(trimmedSolution);
                                break;
                            case INTER_ROUTE_REVERSE:
                                neighbor = interRouteReverse(trimmedSolution);
                                break;
                            case ROUTE_SPLIT:
                                neighbor = routeSplit(trimmedSolution);
                                break;
                            case ROUTE_MERGE:
                                neighbor = routeMerge(trimmedSolution);
                                break;
                            case ROUTE_REVERSE:
                                neighbor = routeReverse(trimmedSolution);
                                break;
                            case ROUTE_SHUFFLE:
                                neighbor = routeShuffle(trimmedSolution);
                                break;
                            case MULTI_ROUTE_MERGE:
                                neighbor = multiRouteMerge(trimmedSolution);
                                break;
                            case ROUTE_SPLIT_MULTI:
                                neighbor = routeSplitMulti(trimmedSolution);
                                break;
                            case NODE_RELOCATION:
                                neighbor = nodeRelocation(trimmedSolution);
                                break;
                            case ROUTE_EXCHANGE:
                                neighbor = routeExchange(trimmedSolution);
                                break;
                            case ROUTE_ROTATION:
                                neighbor = routeRotation(trimmedSolution);
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

            return new Neighbor(newSolution);
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

            return new Neighbor(newSolution);
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

            return new Neighbor(newSolution);
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

            return new Neighbor(newSolution);
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

            return new Neighbor(newSolution);
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

            return new Neighbor(newSolution);
        }

        private static Neighbor vehicleSwap(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            int vehicleId1 = vehicleIds[0];
            int vehicleId2 = vehicleIds[1];

            List<Node> route1 = newSolution.routes.get(vehicleId1);
            List<Node> route2 = newSolution.routes.get(vehicleId2);

            newSolution.routes.put(vehicleId1, route2);
            newSolution.routes.put(vehicleId2, route1);

            return new Neighbor(newSolution);
        }

        private static Neighbor intraRouteReverse(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 3) return null;

            java.util.Collections.reverse(route);

            return new Neighbor(newSolution);
        }

        private static Neighbor interRouteReverse(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
            List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

            if (route1.isEmpty() || route2.isEmpty()) return null;

            java.util.Collections.reverse(route1);
            java.util.Collections.reverse(route2);

            return new Neighbor(newSolution);
        }

        private static Neighbor routeSplit(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 2) return null;

            // Find an empty vehicle
            int emptyVehicleId = -1;
            for (Map.Entry<Integer, List<Node>> entry : newSolution.routes.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    emptyVehicleId = entry.getKey();
                    break;
                }
            }
            if (emptyVehicleId == -1) return null;

            int splitPoint = random.nextInt(route.size() - 1) + 1;
            List<Node> secondPart = new ArrayList<>(route.subList(splitPoint, route.size()));
            route.subList(splitPoint, route.size()).clear();
            newSolution.routes.put(emptyVehicleId, secondPart);

            return new Neighbor(newSolution);
        }

        private static Neighbor routeMerge(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
            List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

            if (route1.isEmpty() || route2.isEmpty()) return null;

            route1.addAll(route2);
            newSolution.routes.put(vehicleIds[1], new ArrayList<>());

            return new Neighbor(newSolution);
        }

        private static Neighbor routeReverse(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 2) return null;

            java.util.Collections.reverse(route);

            return new Neighbor(newSolution);
        }

        private static Neighbor routeShuffle(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 3) return null;

            List<Node> nodesToShuffle = new ArrayList<>(route.subList(1, route.size() - 1));
            java.util.Collections.shuffle(nodesToShuffle);
            route.subList(1, route.size() - 1).clear();
            route.addAll(1, nodesToShuffle);

            return new Neighbor(newSolution);
        }

        private static Neighbor multiRouteMerge(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
            List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

            if (route1.isEmpty() || route2.isEmpty()) return null;

            // Randomly choose merge point for both routes
            int mergePoint1 = random.nextInt(route1.size() + 1);
            int mergePoint2 = random.nextInt(route2.size() + 1);

            List<Node> newRoute = new ArrayList<>(route1.subList(0, mergePoint1));
            newRoute.addAll(route2.subList(mergePoint2, route2.size()));
            newRoute.addAll(route1.subList(mergePoint1, route1.size()));
            newRoute.addAll(route2.subList(0, mergePoint2));

            newSolution.routes.put(vehicleIds[0], newRoute);
            newSolution.routes.put(vehicleIds[1], new ArrayList<>());

            return new Neighbor(newSolution);
        }

        private static Neighbor routeSplitMulti(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 3) return null;

            // Find two empty vehicles
            List<Integer> emptyVehicleIds = new ArrayList<>();
            for (Map.Entry<Integer, List<Node>> entry : newSolution.routes.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    emptyVehicleIds.add(entry.getKey());
                    if (emptyVehicleIds.size() == 2) break;
                }
            }
            if (emptyVehicleIds.size() < 2) return null;

            int splitPoint1 = random.nextInt(route.size() - 2) + 1;
            int splitPoint2 = random.nextInt(route.size() - splitPoint1) + splitPoint1 + 1;

            List<Node> secondPart = new ArrayList<>(route.subList(splitPoint1, splitPoint2));
            List<Node> thirdPart = new ArrayList<>(route.subList(splitPoint2, route.size()));
            
            route.subList(splitPoint1, route.size()).clear();
            newSolution.routes.put(emptyVehicleIds.get(0), secondPart);
            newSolution.routes.put(emptyVehicleIds.get(1), thirdPart);

            return new Neighbor(newSolution);
        }

        private static Neighbor nodeRelocation(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 3) return null;

            int segmentSize = random.nextInt(Math.min(3, route.size() - 1)) + 1;
            int startIndex = random.nextInt(route.size() - segmentSize);
            int targetIndex = random.nextInt(route.size() - segmentSize + 1);

            List<Node> segment = new ArrayList<>(route.subList(startIndex, startIndex + segmentSize));
            route.subList(startIndex, startIndex + segmentSize).clear();
            route.addAll(targetIndex, segment);

            return new Neighbor(newSolution);
        }

        private static Neighbor routeExchange(Solution solution) {
            Solution newSolution = solution.clone();
            int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
            if (vehicleIds == null) return null;

            List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
            List<Node> route2 = newSolution.routes.get(vehicleIds[1]);
            if (route1.isEmpty() || route2.isEmpty()) return null;

            int index1 = random.nextInt(route1.size());
            int index2 = random.nextInt(route2.size());

            Node node1 = route1.get(index1);
            Node node2 = route2.get(index2);

            route1.set(index1, node2);
            route2.set(index2, node1);

            return new Neighbor(newSolution);
        }

        private static Neighbor routeRotation(Solution solution) {
            Solution newSolution = solution.clone();
            int vehicleId = getRandomVehicleWithRoute(newSolution);
            if (vehicleId == -1) return null;

            List<Node> route = newSolution.routes.get(vehicleId);
            if (route.size() < 3) return null;

            int segmentSize = random.nextInt(Math.min(3, route.size() - 1)) + 1;
            int startIndex = random.nextInt(route.size() - segmentSize);
            int targetIndex = random.nextInt(route.size() - segmentSize + 1);

            List<Node> segment = new ArrayList<>(route.subList(startIndex, startIndex + segmentSize));
            route.subList(startIndex, startIndex + segmentSize).clear();
            route.addAll(targetIndex, segment);

            return new Neighbor(newSolution);
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

    private static class Neighbor {
        public Solution solution;

        public Neighbor(Solution solution) {
            this.solution = solution;
        }
    }
}
