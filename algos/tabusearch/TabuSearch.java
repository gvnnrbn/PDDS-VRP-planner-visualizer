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
import java.util.HashMap;

import tabusearch.Movement.MovementType;

public class TabuSearch {
    private static final Random random = new Random();

    public int maxIterations;
    public int tabuListSize;
    public int maxNoImprovement;
    public Set<MovementType> enabledOperators;
    
    private static final int DEFAULT_MAX_ITERATIONS = 1_000_000;
    private static final int DEFAULT_TABU_LIST_SIZE = 100_000;
    private static final int DEFAULT_MAX_NO_IMPROVEMENT = 10_000;
    private static final int DEFAULT_NEIGHBORS_PER_OPERATOR = 10;
    private static final Set<MovementType> DEFAULT_ENABLED_OPERATORS = EnumSet.of(
        MovementType.INTRA_ROUTE_MOVE,
        MovementType.INTRA_ROUTE_SWAP,
        MovementType.INTRA_ROUTE_TWO_OPT,
        MovementType.INTER_ROUTE_MOVE,
        MovementType.INTER_ROUTE_SWAP,
        MovementType.INTER_ROUTE_CROSS_EXCHANGE,
        MovementType.VEHICLE_SWAP,
        MovementType.INTRA_ROUTE_REVERSE,
        MovementType.INTER_ROUTE_REVERSE,
        MovementType.ROUTE_SPLIT,
        MovementType.ROUTE_MERGE,
        MovementType.MULTI_NODE_MOVE,
        MovementType.MULTI_ROUTE_SWAP,
        MovementType.ROUTE_REVERSE,
        MovementType.ROUTE_SHUFFLE,
        MovementType.MULTI_ROUTE_MERGE,
        MovementType.ROUTE_SPLIT_MULTI,
        MovementType.NODE_RELOCATION,
        MovementType.ROUTE_EXCHANGE,
        MovementType.ROUTE_ROTATION
    );

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
        double currentFitness = currentSolution.fitness(environment);
        double bestFitness = currentFitness;

        while (iteration < maxIterations && noImprovementCount < maxNoImprovement) {
            List<Neighbor> neighborhood = generateNeighborhood(currentSolution, environment);
            List<Neighbor> candidates = new ArrayList<>();
            
            for (Neighbor neighbor : neighborhood) {
                boolean isTabu = tabuList.contains(neighbor.movement);
                double neighborFitness = neighbor.solution.fitness(environment);
                
                if (neighbor.solution.isFeasible(environment) && 
                    (!isTabu || neighborFitness > bestFitness)) {
                    candidates.add(neighbor);
                }
            }
            
            Neighbor bestCandidate = selectBestCandidate(candidates, environment);
            
            if (bestCandidate != null) {
                currentSolution = bestCandidate.solution;
                currentFitness = currentSolution.fitness(environment);
                
                if (currentFitness > bestFitness) {
                    bestSolution = currentSolution.clone();
                    bestFitness = currentFitness;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
                
                tabuList.add(bestCandidate.movement);
                if (tabuList.size() > tabuListSize) {
                    tabuList.remove(0);
                }
            } else {
                noImprovementCount++;
            }
            
            iteration++;
        }
        
        return bestSolution;
    }

    private static Neighbor selectBestCandidate(List<Neighbor> candidates, Environment environment) {
        if (candidates.isEmpty()) return null;
        
        Neighbor bestCandidate = candidates.get(0);
        double bestFitness = bestCandidate.solution.fitness(environment);
        
        for (Neighbor candidate : candidates) {
            double fitness = candidate.solution.fitness(environment);
            if (fitness > bestFitness) {
                bestCandidate = candidate;
                bestFitness = fitness;
            }
        }
        return bestCandidate;
    }

    public List<Neighbor> generateNeighborhood(Solution solution, Environment environment) {
        List<Neighbor> neighborhood = new ArrayList<>();
        Solution trimmedSolution = solution.clone();

        Map<Integer, Node> startNodes = new HashMap<>();
        Map<Integer, Node> finalNodes = new HashMap<>();
        for (Map.Entry<Integer, List<Node>> entry : trimmedSolution.routes.entrySet()) {
            startNodes.put(entry.getKey(), entry.getValue().get(0).clone());
            finalNodes.put(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1).clone());
            entry.getValue().remove(0);
            entry.getValue().remove(entry.getValue().size() - 1);
        }

        int neighborsPerOperator = DEFAULT_NEIGHBORS_PER_OPERATOR;
        
        for (int i = 0; i < neighborsPerOperator; i++) {
            List<MovementType> operators = new ArrayList<>(enabledOperators);
            Collections.shuffle(operators, random);
            
            for (MovementType operator : operators) {
                Neighbor neighbor = null;
                int attempts = 0;
                int maxAttempts = 10;
                
                while (neighbor == null && attempts < maxAttempts) {
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
                        case MULTI_NODE_MOVE:
                            neighbor = multiNodeMove(trimmedSolution);
                            break;
                        case MULTI_ROUTE_SWAP:
                            neighbor = multiRouteSwap(trimmedSolution);
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
                    
                    if (neighbor.solution.isFeasible(environment)) {
                        neighborhood.add(neighbor);
                    }
                }
            }
        }

        if (neighborhood.size() != 0) {
            System.out.println("Generated " + neighborhood.size() + " neighbors");
        }
        return neighborhood;
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

        Collections.shuffle(vehicleIds, random);
        return new int[]{vehicleIds.get(0), vehicleIds.get(1)};
    }

    public static Neighbor intraRouteMove(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        int routeSize = route.size();

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
        
        if (solution.routes.get(vehicleId).size() < 2) return null;

        List<Node> route = solution.routes.get(vehicleId);
        int routeSize = route.size();

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

    public static Neighbor vehicleSwap(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);

        solution.routes.put(vehicleId1, route2);
        solution.routes.put(vehicleId2, route1);

        Movement movement = new Movement(MovementType.VEHICLE_SWAP);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor intraRouteReverse(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        int start = random.nextInt(route.size() - 2) + 1;
        int end = random.nextInt(route.size() - start) + start + 1;
        
        List<Node> segment = route.subList(start, end);
        Collections.reverse(segment);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_REVERSE);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = start;
        movement.nodeIdxTo = end;

        return new Neighbor(solution, movement);
    }

    public static Neighbor interRouteReverse(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        int start1 = random.nextInt(route1.size());
        int end1 = random.nextInt(route1.size() - start1) + start1 + 1;
        
        int start2 = random.nextInt(route2.size());
        int end2 = random.nextInt(route2.size() - start2) + start2 + 1;

        List<Node> segment1 = new ArrayList<>(route1.subList(start1, end1));
        List<Node> segment2 = new ArrayList<>(route2.subList(start2, end2));

        Collections.reverse(segment1);
        Collections.reverse(segment2);

        route1.subList(start1, end1).clear();
        route2.subList(start2, end2).clear();

        route1.addAll(start1, segment2);
        route2.addAll(start2, segment1);

        Movement movement = new Movement(MovementType.INTER_ROUTE_REVERSE);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;
        movement.nodeIdxFrom = start1;
        movement.nodeIdxTo = end1;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeSplit(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 4) return null;

        int emptyVehicleId = -1;
        for (Map.Entry<Integer, List<Node>> entry : solution.routes.entrySet()) {
            if (entry.getValue().size() == 1) {
                emptyVehicleId = entry.getKey();
                break;
            }
        }

        if (emptyVehicleId == -1) return null;

        int splitPoint = random.nextInt(route.size() - 2) + 1;
        List<Node> newRoute = new ArrayList<>(route.subList(splitPoint, route.size()));
        route.subList(splitPoint, route.size()).clear();

        solution.routes.put(emptyVehicleId, newRoute);

        Movement movement = new Movement(MovementType.ROUTE_SPLIT);
        movement.vehicle1 = vehicleId;
        movement.vehicle2 = emptyVehicleId;
        movement.nodeIdxFrom = splitPoint;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeMerge(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);

        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        route1.addAll(route2);
        route2.clear();

        Movement movement = new Movement(MovementType.ROUTE_MERGE);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor multiNodeMove(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);
        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        int segmentSize = random.nextInt(Math.min(3, route.size() - 1)) + 1;
        int startIndex = random.nextInt(route.size() - segmentSize);
        int targetIndex = random.nextInt(route.size() - segmentSize + 1);

        List<Node> segment = new ArrayList<>(route.subList(startIndex, startIndex + segmentSize));
        route.subList(startIndex, startIndex + segmentSize).clear();
        route.addAll(targetIndex, segment);

        Movement movement = new Movement(MovementType.MULTI_NODE_MOVE);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = startIndex;
        movement.nodeIdxTo = targetIndex;

        return new Neighbor(solution, movement);
    }

    public static Neighbor multiRouteSwap(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);
        if (vehicleIds == null) return null;

        List<Node> route1 = solution.routes.get(vehicleIds[0]);
        List<Node> route2 = solution.routes.get(vehicleIds[1]);
        if (route1.size() < 2 || route2.size() < 2) return null;

        int segmentSize1 = random.nextInt(Math.min(3, route1.size() - 1)) + 1;
        int segmentSize2 = random.nextInt(Math.min(3, route2.size() - 1)) + 1;

        int start1 = random.nextInt(route1.size() - segmentSize1 + 1);
        int start2 = random.nextInt(route2.size() - segmentSize2 + 1);

        List<Node> segment1 = new ArrayList<>(route1.subList(start1, start1 + segmentSize1));
        List<Node> segment2 = new ArrayList<>(route2.subList(start2, start2 + segmentSize2));

        route1.subList(start1, start1 + segmentSize1).clear();
        route2.subList(start2, start2 + segmentSize2).clear();

        route1.addAll(start1, segment2);
        route2.addAll(start2, segment1);

        Movement movement = new Movement(MovementType.MULTI_ROUTE_SWAP);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];
        movement.nodeIdxFrom = start1;
        movement.nodeIdxTo = start2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeReverse(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);
        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 2) return null;

        Collections.reverse(route);

        Movement movement = new Movement(MovementType.ROUTE_REVERSE);
        movement.vehicle1 = vehicleId;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeShuffle(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);
        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        List<Node> nodesToShuffle = new ArrayList<>(route.subList(1, route.size() - 1));
        Collections.shuffle(nodesToShuffle);
        route.subList(1, route.size() - 1).clear();
        route.addAll(1, nodesToShuffle);

        Movement movement = new Movement(MovementType.ROUTE_SHUFFLE);
        movement.vehicle1 = vehicleId;

        return new Neighbor(solution, movement);
    }

    public static Neighbor multiRouteMerge(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(solution);
        if (vehicleIds == null) return null;

        List<Node> route1 = solution.routes.get(vehicleIds[0]);
        List<Node> route2 = solution.routes.get(vehicleIds[1]);
        if (route1.isEmpty() || route2.isEmpty()) return null;

        // Find empty vehicle for the merged route
        int emptyVehicleId = -1;
        for (Map.Entry<Integer, List<Node>> entry : solution.routes.entrySet()) {
            if (entry.getValue().size() == 1) {
                emptyVehicleId = entry.getKey();
                break;
            }
        }
        if (emptyVehicleId == -1) return null;

        List<Node> mergedRoute = new ArrayList<>(route1);
        mergedRoute.addAll(route2);
        solution.routes.put(emptyVehicleId, mergedRoute);
        route1.clear();
        route2.clear();

        Movement movement = new Movement(MovementType.MULTI_ROUTE_MERGE);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeSplitMulti(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);

        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 4) return null;

        int emptyVehicleId = -1;
        for (Map.Entry<Integer, List<Node>> entry : solution.routes.entrySet()) {
            if (entry.getValue().size() == 1) {
                emptyVehicleId = entry.getKey();
                break;
            }
        }

        if (emptyVehicleId == -1) return null;

        int splitPoint = random.nextInt(route.size() - 2) + 1;
        List<Node> newRoute = new ArrayList<>(route.subList(splitPoint, route.size()));
        route.subList(splitPoint, route.size()).clear();

        solution.routes.put(emptyVehicleId, newRoute);

        Movement movement = new Movement(MovementType.ROUTE_SPLIT_MULTI);
        movement.vehicle1 = vehicleId;
        movement.vehicle2 = emptyVehicleId;
        movement.nodeIdxFrom = splitPoint;

        return new Neighbor(solution, movement);
    }

    public static Neighbor nodeRelocation(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);
        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        int segmentSize = random.nextInt(Math.min(3, route.size() - 1)) + 1;
        int startIndex = random.nextInt(route.size() - segmentSize);
        int targetIndex = random.nextInt(route.size() - segmentSize + 1);

        List<Node> segment = new ArrayList<>(route.subList(startIndex, startIndex + segmentSize));
        route.subList(startIndex, startIndex + segmentSize).clear();
        route.addAll(targetIndex, segment);

        Movement movement = new Movement(MovementType.NODE_RELOCATION);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = startIndex;
        movement.nodeIdxTo = targetIndex;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeExchange(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId1 = getRandomVehicleWithRoute(solution);
        int vehicleId2 = getRandomVehicleWithRoute(solution);
        if (vehicleId1 == -1 || vehicleId2 == -1) return null;

        List<Node> route1 = solution.routes.get(vehicleId1);
        List<Node> route2 = solution.routes.get(vehicleId2);
        if (route1.isEmpty() || route2.isEmpty()) return null;

        int index1 = random.nextInt(route1.size());
        int index2 = random.nextInt(route2.size());

        Node node1 = route1.get(index1);
        Node node2 = route2.get(index2);

        route1.set(index1, node2);
        route2.set(index2, node1);

        Movement movement = new Movement(MovementType.ROUTE_EXCHANGE);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;
        movement.nodeIdxFrom = index1;
        movement.nodeIdxTo = index2;

        return new Neighbor(solution, movement);
    }

    public static Neighbor routeRotation(Solution currentSolution) {
        Solution solution = currentSolution.clone();
        int vehicleId = getRandomVehicleWithRoute(solution);
        if (vehicleId == -1) return null;

        List<Node> route = solution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        int segmentSize = random.nextInt(Math.min(3, route.size() - 1)) + 1;
        int startIndex = random.nextInt(route.size() - segmentSize);
        int targetIndex = random.nextInt(route.size() - segmentSize + 1);

        List<Node> segment = new ArrayList<>(route.subList(startIndex, startIndex + segmentSize));
        route.subList(startIndex, startIndex + segmentSize).clear();
        route.addAll(targetIndex, segment);

        Movement movement = new Movement(MovementType.ROUTE_ROTATION);
        movement.vehicle1 = vehicleId;
        movement.nodeIdxFrom = startIndex;
        movement.nodeIdxTo = targetIndex;

        return new Neighbor(solution, movement);
    }
}
    