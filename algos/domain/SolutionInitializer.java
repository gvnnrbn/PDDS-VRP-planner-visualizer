package domain;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class SolutionInitializer {
    private Random random = new Random();

    // Generate an initial solution with all finite nodes and a random number of infinite nodes
    public Solution generateInitialSolution(Environment environment) {
        Solution solution = new Solution();

        solution.routes = new HashMap<>();

        List<Node> nodesPool = new ArrayList<>(environment.getNodes());

        // Filter all empty nodes and final nodes, pool should be only of assignable nodes
        nodesPool = nodesPool.stream()
            .filter(node -> !(node instanceof EmptyNode) && !(node instanceof FinalNode))
            .collect(Collectors.toCollection(ArrayList::new));

        // Set starting position node for each vehicle
        for (Vehicle vehicle : environment.vehicles) {
            solution.routes.put(vehicle.id(), new ArrayList<>());

            // Find initial position node corresponding to this vehicle
            Node initialPositionNode = environment.getNodes().stream()
                .filter(node -> node.getPosition().equals(vehicle.initialPosition()))
                .findFirst().get();

            solution.routes.get(vehicle.id()).add(initialPositionNode);
        }

        // Randomly assign nodes to vehicles
        while (nodesPool.size() > 0) {
            // Select random vehicle
            Vehicle vehicle = environment.vehicles.get(random.nextInt(environment.vehicles.size()));

            // Select random node from nodesPool
            Node node = nodesPool.get(random.nextInt(nodesPool.size()));

            solution.routes.get(vehicle.id()).add(node);
            nodesPool.remove(node);
        }

        Warehouse mainWarehouse = environment.warehouses.stream()
            .filter(Warehouse::isMain)
            .findFirst()
            .get();

        List<FinalNode> finalNodes = environment.getNodes().stream()
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
