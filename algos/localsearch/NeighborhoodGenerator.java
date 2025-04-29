package localsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import domain.Environment;
import domain.Node;
import domain.Solution;

import localsearch.Movement.MovementType;

public class NeighborhoodGenerator {

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

        for (MovementType operator : MovementType.values()) {
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

        Movement movement = new Movement(MovementType.INTRA_ROUTE_MOVE);
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

        Collections.swap(route, index1, index2);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_SWAP);
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
        Collections.reverse(segmentToReverse);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_TWO_OPT);
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

        Movement movement = new Movement(MovementType.INTER_ROUTE_MOVE);
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

        Movement movement = new Movement(MovementType.INTER_ROUTE_SWAP);
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

        Movement movement = new Movement(MovementType.INTER_ROUTE_CROSS_EXCHANGE);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;
        movement.nodeIdxFrom = i;
        movement.nodeIdxTo = j;
        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.VEHICLE_SWAP);
        movement.vehicle1 = vehicleId1;
        movement.vehicle2 = vehicleId2;

        return new Neighbor(newSolution, movement);
    }

    private static Neighbor intraRouteReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        Collections.reverse(route);

        Movement movement = new Movement(MovementType.INTRA_ROUTE_REVERSE);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
    }

    private static Neighbor interRouteReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int[] vehicleIds = getTwoDistinctRandomVehiclesWithRoutes(newSolution);
        if (vehicleIds == null) return null;

        List<Node> route1 = newSolution.routes.get(vehicleIds[0]);
        List<Node> route2 = newSolution.routes.get(vehicleIds[1]);

        if (route1.isEmpty() || route2.isEmpty()) return null;

        Collections.reverse(route1);
        Collections.reverse(route2);

        Movement movement = new Movement(MovementType.INTER_ROUTE_REVERSE);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.ROUTE_SPLIT);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.ROUTE_MERGE);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];

        return new Neighbor(newSolution, movement);
    }

    private static Neighbor routeReverse(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 2) return null;

        Collections.reverse(route);

        Movement movement = new Movement(MovementType.ROUTE_REVERSE);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
    }

    private static Neighbor routeShuffle(Solution solution) {
        Solution newSolution = solution.clone();
        int vehicleId = getRandomVehicleWithRoute(newSolution);
        if (vehicleId == -1) return null;

        List<Node> route = newSolution.routes.get(vehicleId);
        if (route.size() < 3) return null;

        List<Node> nodesToShuffle = new ArrayList<>(route.subList(1, route.size() - 1));
        Collections.shuffle(nodesToShuffle);
        route.subList(1, route.size() - 1).clear();
        route.addAll(1, nodesToShuffle);

        Movement movement = new Movement(MovementType.ROUTE_SHUFFLE);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.MULTI_ROUTE_MERGE);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.ROUTE_SPLIT_MULTI);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.NODE_RELOCATION);
        movement.vehicle1 = vehicleId;

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.ROUTE_EXCHANGE);
        movement.vehicle1 = vehicleIds[0];
        movement.vehicle2 = vehicleIds[1];

        return new Neighbor(newSolution, movement);
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

        Movement movement = new Movement(MovementType.ROUTE_ROTATION);
        movement.vehicle1 = vehicleId;

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

        Collections.shuffle(vehicleIds, random);
        return new int[]{vehicleIds.get(0), vehicleIds.get(1)};
    }
}
