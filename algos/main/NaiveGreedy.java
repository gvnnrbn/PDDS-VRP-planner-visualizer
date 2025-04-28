package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import domain.Environment;
import domain.Node;
import domain.Solution;
import domain.SolutionInitializer;
import domain.Time;
import tabusearch.Movement.MovementType;
import utils.EnvironmentParser;

public class NaiveGreedy {
    public static void main(String[] args) {
        EnvironmentParser parser = new EnvironmentParser(new Time(0, 1, 0, 0));
        Environment environment = parser.parseEnvironment(
            "main/vehicles.csv", 
            "main/orders.csv", 
            "main/blockages.csv", 
            "main/warehouses.csv"
        );

        environment.orders = environment.orders.subList(0, 300);

        System.out.println("Environment Report:");
        System.out.println("- Number of vehicles: " + environment.vehicles.size());
        System.out.println("- Number of orders: " + environment.orders.size());
        System.out.println("- Number of blockages: " + environment.blockages.size());
        System.out.println("- Number of warehouses: " + environment.warehouses.size());

        SolutionInitializer initializer = new SolutionInitializer();
        Solution solution = initializer.generateInitialSolution(environment);

        Solution currBestSolution = solution;

        int max_iter = 10_000;
        int max_time = 55 * 1000;
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        while (iterations < max_iter && (System.currentTimeMillis() - startTime) < max_time) {
            List<Solution> adjacentSolutions = generateAdjacentSolutions(currBestSolution, environment);

            if (iterations % 100 == 0) {
                long timePassed = System.currentTimeMillis() - startTime;
                System.out.println("Iteration " + iterations + ": Best solution found: " + currBestSolution.fitness(environment) + 
                    " (is feasible: " + currBestSolution.isFeasible(environment) + ") Time passed: " + timePassed + "ms");
            }

            Solution bestAdjacentSolution = Collections.max(adjacentSolutions, (solution1, solution2) -> Double.compare(solution1.fitness(environment), solution2.fitness(environment)));
            if (bestAdjacentSolution.fitness(environment) > currBestSolution.fitness(environment)) {
                currBestSolution = bestAdjacentSolution;
            }

            // System.out.println("------------------------------------------");

            iterations++;
        }

        System.out.println("------------------------------------------");
        System.out.println("For a problem of " + environment.orders.size() + " orders, the best solution found is: " + currBestSolution.fitness(environment) + " (is feasible: " + currBestSolution.isFeasible(environment) + ")");
        System.out.println("Best solution found: " + currBestSolution.fitness(environment) + " (is feasible: " + currBestSolution.isFeasible(environment) + ")");
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("Report: \n" + currBestSolution.getReport());
    }

    private static int attemptsPerOperation = 10;
    private static int neighborsPerOperator = 10;
    private static Random random = new Random();

    private static List<Solution> generateAdjacentSolutions(Solution solution, Environment environment) {
        List<Solution> adjacentSolutions = new ArrayList<>();
        Solution trimmedSolution = solution.clone();

        Map<Integer, Node> startNodes = new HashMap<>();
        Map<Integer, Node> finalNodes = new HashMap<>();
        for (Map.Entry<Integer, List<Node>> entry : trimmedSolution.routes.entrySet()) {
            startNodes.put(entry.getKey(), entry.getValue().get(0).clone());
            finalNodes.put(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1).clone());
            entry.getValue().remove(0);
            entry.getValue().remove(entry.getValue().size() - 1);
        }

        for (MovementType operator : MovementType.values()) {
            for (int i = 0; i < neighborsPerOperator; i++) {
                int attempts = 0;
                Solution neighbor = null;
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
                        neighbor.routes.get(entry.getKey()).add(0, entry.getValue());
                    }
                    for (Map.Entry<Integer, Node> entry : finalNodes.entrySet()) {
                        neighbor.routes.get(entry.getKey()).add(entry.getValue());
                    }

                    adjacentSolutions.add(neighbor);
                }
            }
        }
        
        return adjacentSolutions;
    }

    private static Solution intraRouteMove(Solution solution) {
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

        return newSolution;
    }

    private static Solution intraRouteSwap(Solution solution) {
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

        Collections.swap(route, index1, index2);
        return newSolution;
    }

    private static Solution intraRouteTwoOpt(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        int routeSize = route.size();
        if (routeSize < 4) return null;

        int i = random.nextInt(routeSize - 2);
        int j = random.nextInt(routeSize - 1 - (i + 1)) + (i + 1);

        List<Node> segmentToReverse = route.subList(i + 1, j + 1);
        Collections.reverse(segmentToReverse);
        return newSolution;
    }

    private static Solution interRouteMove(Solution solution) {
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
        return newSolution;
    }

    private static Solution interRouteSwap(Solution solution) {
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
        return newSolution;
    }

    private static Solution interRouteCrossExchange(Solution solution) {
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
        return newSolution;
    }

    private static Solution vehicleSwap(Solution solution) {
        Solution newSolution = solution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
        if (vehicleIds == null) return null;

        int vehicleId1 = vehicleIds[0];
        int vehicleId2 = vehicleIds[1];

        List<Node> route1 = newSolution.routes.get(vehicleId1);
        List<Node> route2 = newSolution.routes.get(vehicleId2);

        newSolution.routes.put(vehicleId1, route2);
        newSolution.routes.put(vehicleId2, route1);
        return newSolution;
    }

    private static Solution intraRouteReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        Collections.reverse(route);
        return newSolution;
    }

    private static Solution interRouteReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
        if (vehicleIds == null) return null;

        List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
        List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        Collections.reverse(route1);
        Collections.reverse(route2);
        return newSolution;
    }

    private static Solution routeSplit(Solution solution) {
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

        return newSolution;
    }

    private static Solution routeMerge(Solution solution) {
        Solution newSolution = solution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
        if (vehicleIds == null) return null;

        List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
        List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        route1.addAll(route2);
        newSolution.routes.put(vehicleIds[1], new ArrayList<>());
        return newSolution;
    }

    private static Solution multiNodeMove(Solution solution) {
        return null;
    }

    private static Solution multiRouteSwap(Solution solution) {
        return null;
    }

    private static Solution routeReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 2) return null;

        Collections.reverse(route);
        return newSolution;
    }

    private static Solution routeShuffle(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        List<Node> nodesToShuffle = new ArrayList<>(route.subList(1, route.size() - 1));
        Collections.shuffle(nodesToShuffle);
        route.subList(1, route.size() - 1).clear();
        route.addAll(1, nodesToShuffle);
        return newSolution;
    }

    private static Solution multiRouteMerge(Solution solution) {
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
        return newSolution;
    }

    private static Solution routeSplitMulti(Solution solution) {
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

        return newSolution;
    }

    private static Solution nodeRelocation(Solution solution) {
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
        return newSolution;
    }

    private static Solution routeExchange(Solution solution) {
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
        return newSolution;
    }

    private static Solution routeRotation(Solution solution) {
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
        return newSolution;
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
}
