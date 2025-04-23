package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class SolutionInitializer {
    private Random random = new Random();

    public Solution generateInitialSolution(Environment environment) {
        Solution solution = new Solution();

        solution.routes = new HashMap<>();

        List<Node> nodesPool = new ArrayList<>(environment.getNodes());

        // Filter all empty nodes
        nodesPool = nodesPool.stream()
            .filter(node -> !(node instanceof EmptyNode))
            .collect(Collectors.toCollection(ArrayList::new));

        int nonInfiniteNodesCount = 0;
        for (Node node : nodesPool) {
            if (!(node.isInfiniteNode())) {
                nonInfiniteNodesCount++;
            }
        }

        // Set starting position node for each vehicle
        for (Vehicle vehicle : environment.vehicles) {
            solution.routes.put(vehicle.id(), new ArrayList<>());

            // Find initial position node corresponding to this vehicle
            Node initialPositionNode = environment.getNodes().stream()
                .filter(node -> node.getPosition().equals(vehicle.initialPosition()))
                .findFirst().get();

            solution.routes.get(vehicle.id()).add(initialPositionNode);
        }

        // Acceptance rate for infinite nodes when infinite nodes are more than finite ones
        double infiniteNodeAcceptanceRate = 0.2;

        while (nonInfiniteNodesCount > 0) {
            // Select random vehicle
            Vehicle vehicle = environment.vehicles.get(random.nextInt(environment.vehicles.size()));

            // Select random node from nodesPool
            Node node = nodesPool.get(random.nextInt(nodesPool.size()));

            if (nonInfiniteNodesCount > nodesPool.size() - nonInfiniteNodesCount) {
                // Accept all nodes
                solution.routes.get(vehicle.id()).add(node);
                if (!node.isInfiniteNode()) {
                    nodesPool.remove(node);
                    nonInfiniteNodesCount--;
                }
            } else {
                // Accept infinite nodes with probability infiniteNodeAcceptanceRate
                if (node.isInfiniteNode()) {
                    if (random.nextDouble() < infiniteNodeAcceptanceRate) {
                        solution.routes.get(vehicle.id()).add(node);
                    }
                } else {
                    solution.routes.get(vehicle.id()).add(node);
                    nodesPool.remove(node);
                    nonInfiniteNodesCount--;
                }
            }
        }

        Warehouse mainWarehouse = environment.warehouses.stream()
            .filter(Warehouse::isMain)
            .findFirst()
            .get();

        List<FinalNode> finalNodes = nodesPool.stream()
            .filter(localNode -> localNode instanceof FinalNode)
            .map(localNode -> (FinalNode) localNode)
            .filter(localNode -> localNode.getPosition().equals(mainWarehouse.position()))
            .collect(Collectors.toCollection(ArrayList::new));

        // Assign final nodes to vehicles
        for(int i=0; i<environment.vehicles.size(); i++) {
            solution.routes.get(environment.vehicles.get(i).id()).add(finalNodes.get(i));
        }

        return solution;
    }
}
