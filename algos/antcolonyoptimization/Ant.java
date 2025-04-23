package antcolonyoptimization;

import domain.Environment;
import domain.Node;
import domain.EmptyNode;
import domain.Solution;
import domain.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ant {
    private Solution solution;
    private List<Node> nodes;

    public void constructSolution(Map<Integer, Map<Integer, Double>> pheromones, double alpha, double beta, Environment environment) {
        solution = new Solution();
        solution.routes = new HashMap<>();

        // Copy the nodes to avoid modifying the original list
        nodes = new ArrayList<>(environment.getNodes());

        // Sort vehicles for deterministic round robin
        List<Vehicle> vehicles = new ArrayList<>(environment.vehicles);
        Collections.sort(vehicles, (v1, v2) -> v1.id() - v2.id());

        int totalFiniteNodes = countFiniteNodes();
        int assignedFiniteNodes = 0;

        // Skip first index and construct first Node for each vehicle
        for(Vehicle vehicle : vehicles) {
            solution.routes.put(vehicle.id(), new ArrayList<>());
            solution.routes.get(vehicle.id()).add(nodes.get(0));
            nodes.remove(nodes.get(0));
            assignedFiniteNodes++;
        }

        while (assignedFiniteNodes < totalFiniteNodes) {
            for(Vehicle vehicle : vehicles) {
                List<Node> route = solution.routes.get(vehicle.id());
                Node currNode = route.getLast();

                // If the current node is an empty node and it's not the first node, skip since it's an end node
                if(currNode instanceof EmptyNode && route.size() != 1) {
                    continue;
                }

                Node nextNode = getNextNode(pheromones, currNode, alpha);

                route.add(nextNode);

                // If the next node is finite, remove it from the nodes list and increment the assignedFiniteNodes counter
                if (!nextNode.isInfiniteNode()) {
                    assignedFiniteNodes++;
                    Node correspondingNode = nodes.stream()
                        .filter(node -> node.id == nextNode.id)
                        .findFirst()
                        .orElse(null);
                    nodes.remove(correspondingNode);
                }
            }
        }
    }

    public Solution getSolution() {
        return solution;
    }

    private int countFiniteNodes() {
        int count = 0;
        for(Node node : nodes) {
            if(!node.isInfiniteNode()) {
                count++;
            }
        }
        return count;
    }

    private Node getNextNode(Map<Integer, Map<Integer, Double>> pheromones, Node currNode, double alpha) {
        if (nodes.isEmpty()) {
            return null;
        }

        double[] probabilities = new double[nodes.size()];
        double sum = 0;

        for(int i = 0; i < nodes.size(); i++) {
            Node nextNode = nodes.get(i);
            
            double pheromone = pheromones.get(currNode.id).get(nextNode.id);
            probabilities[i] = Math.pow(pheromone, alpha);
            sum += probabilities[i];
        }

        if (sum == 0) {
            return nodes.get(0);  // Fallback if all probabilities are 0
        }

        for(int i = 0; i < nodes.size(); i++) {
            probabilities[i] /= sum;
        }

        double random = Math.random() * sum;
        double cumulative = 0;

        for(int i = 0; i < nodes.size(); i++) {
            cumulative += probabilities[i];
            if(random <= cumulative) {
                return nodes.get(i);
            }
        }

        return nodes.get(nodes.size() - 1);
    }
}
