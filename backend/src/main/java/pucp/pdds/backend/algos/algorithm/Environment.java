package pucp.pdds.backend.algos.algorithm;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Random;

import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;

public class Environment {
    public Time currentTime;
    public int minutesToSimulate;

    public List<PlannerVehicle> vehicles;
    public List<PlannerOrder> orders;
    public List<PlannerWarehouse> warehouses;
    public List<PlannerBlockage> blockages;
    public List<PlannerFailure> failures;
    public List<PlannerMaintenance> maintenances;

    private List<Node> nodes;
    private boolean areNodesGenerated = false;

    private Map<Position, Map<Position, Double>> distances;
    private boolean areDistancesGenerated = false;

    public List<Node> getNodes() {
        if (!areNodesGenerated) {
            generateNodes();
        }
        return nodes;
    }

    public Map<Position, Map<Position, Double>> getDistances() {
        if (!areDistancesGenerated) {
            distances = PathBuilder.generateDistances(getNodes().stream().map(Node::getPosition).collect(Collectors.toList()), blockages);
            areDistancesGenerated = true;
        }
        return distances;
    }

    public Environment(List<PlannerVehicle> vehicles, List<PlannerOrder> orders, List<PlannerWarehouse> warehouses, List<PlannerBlockage> blockages,
            List<PlannerFailure> failures, List<PlannerMaintenance> maintenances, Time currentTime, int minutesToSimulate) {
        this.vehicles = vehicles;
        this.orders = orders;
        this.warehouses = warehouses;
        this.blockages = blockages;
        this.failures = failures;
        this.maintenances = maintenances;
        this.currentTime = currentTime;
        this.minutesToSimulate = minutesToSimulate;
    }

    public Environment() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Time(1, 1, 1, 0, 0), 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Environment{\n");
        for (PlannerVehicle vehicle : vehicles) {
            sb.append("  ").append(vehicle).append("\n");
        }
        for (PlannerOrder order : orders) {
            sb.append("  ").append(order).append("\n");
        }
        for (PlannerWarehouse warehouse : warehouses) {
            sb.append("  ").append(warehouse).append("\n");
        }
        for (PlannerBlockage blockage : blockages) {
            sb.append("  ").append(blockage).append("\n");
        }
        for (PlannerFailure failure : failures) {
            sb.append("  ").append(failure).append("\n");
        }
        for (PlannerMaintenance maintenance : maintenances) {
            sb.append("  ").append(maintenance).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public void generateNodes() {
        List<PlannerWarehouse> warehousesCopy = new ArrayList<>(warehouses);

        List<Node> nodes = new ArrayList<>();
        int nodeSerial = 0;

        for (PlannerVehicle vehicle : vehicles) {
            nodes.add(new EmptyNode(nodeSerial++, vehicle.initialPosition));
        }

        for (PlannerOrder order : orders) {
            int remainingGLP = order.amountGLP;
            while (remainingGLP > 0) {
                if (remainingGLP > OrderDeliverNode.chunkSize) {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, OrderDeliverNode.chunkSize));
                    remainingGLP -= OrderDeliverNode.chunkSize;
                } else {
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, remainingGLP));
                    remainingGLP = 0;
                }
            }
        }

        // Calculate the total amount of GLP that needs to be transported
        int totalGLP = 0;
        for (PlannerOrder order : orders) {
            totalGLP += order.amountGLP;
        }
        for (PlannerVehicle vehicle : vehicles) {
            totalGLP += vehicle.maxGLP - vehicle.currentGLP;
        }

        // Calculate the total amount of GLP currently in the vehicles
        int totalGLPInVehicles = 0;
        for (PlannerVehicle vehicle : vehicles) {
            totalGLPInVehicles += vehicle.currentGLP;
        }

        int totalGLPToRefill = totalGLP - totalGLPInVehicles;
        int totalAssignableGLP = (int) (totalGLPToRefill * 2);

        // Separate non-main and main warehouses
        List<PlannerWarehouse> nonMainWarehouses = warehousesCopy.stream()
            .filter(w -> !w.isMain)
            .collect(Collectors.toList());
        List<PlannerWarehouse> mainWarehouses = warehousesCopy.stream()
            .filter(w -> w.isMain)
            .collect(Collectors.toList());

        // First, exhaust all secondary warehouses
        for (PlannerWarehouse warehouse : nonMainWarehouses) {
            while (totalAssignableGLP > 0 && warehouse.currentGLP > 0) {
                int assignableGLP = Math.min(warehouse.currentGLP, ProductRefillNode.chunkSize);
                assignableGLP = Math.min(assignableGLP, totalAssignableGLP);

                // Create refill nodes in smaller chunks to allow for more frequent refueling
                int refillChunkSize = Math.min(assignableGLP, ProductRefillNode.chunkSize);
                nodes.add(new ProductRefillNode(nodeSerial++, warehouse, refillChunkSize));
                warehouse.currentGLP -= refillChunkSize;
                totalAssignableGLP -= refillChunkSize;
            }
        }

        // Only then, use main warehouses if secondary warehouses are exhausted
        for (PlannerWarehouse warehouse : mainWarehouses) {
            while (totalAssignableGLP > 0 && warehouse.currentGLP > 0) {
                int assignableGLP = Math.min(warehouse.currentGLP, ProductRefillNode.chunkSize);
                assignableGLP = Math.min(assignableGLP, totalAssignableGLP);

                // Create refill nodes in smaller chunks to allow for more frequent refueling
                int refillChunkSize = Math.min(assignableGLP, ProductRefillNode.chunkSize);
                nodes.add(new ProductRefillNode(nodeSerial++, warehouse, refillChunkSize));
                warehouse.currentGLP -= refillChunkSize;
                totalAssignableGLP -= refillChunkSize;
            }
        }

        // Add final nodes
        PlannerWarehouse mainWarehouse = null;
        for (PlannerWarehouse warehouse : warehouses) {
            if (warehouse.isMain) {
                mainWarehouse = warehouse;
                break;
            }
        }
        if (mainWarehouse == null) {
            throw new RuntimeException("No main warehouse found");
        }
        for (int i = 0; i < vehicles.size(); i++) {
            nodes.add(new FinalNode(nodeSerial++, mainWarehouse.position));
        }

        this.nodes = nodes;
        areNodesGenerated = true;
    }

    // Dist Max = 25 * 180 / 15 = 300 Km.
    // Fuel (in galons) = Distance (in km) * [weight (in kg) + 0.5 * GLP (in m3)] /
    // 180
    public static double calculateFuelCost(Node from, Node to, Map<Position, Map<Position, Double>> distances,
            PlannerVehicle vehicle) {
        double distance = distances.get(from.getPosition()).get(to.getPosition());
        double fuelCost = distance * (vehicle.weight / 1000 + vehicle.currentGLP * 0.5) / 180;
        return fuelCost;
    }

    public Solution getRandomSolution() {
        Solution solution = new Solution(this);
        solution.routes = new HashMap<>();

        solution.setStartingTime(this.currentTime);

        // Get all nodes from the environment
        List<Node> nodesPool = this.getNodes().stream()
            .filter(node -> !(node instanceof EmptyNode) && !(node instanceof FinalNode))
            .collect(Collectors.toCollection(ArrayList::new));

        // Initialize routes for each vehicle with their starting position
        for (PlannerVehicle vehicle : this.vehicles) {
            solution.routes.put(vehicle.id, new ArrayList<>());
            Node initialPositionNode = this.getNodes().stream()
                .filter(node -> node.getPosition().equals(vehicle.initialPosition))
                .findFirst().get();
            solution.routes.get(vehicle.id).add(initialPositionNode);
        }

        // Separate order nodes from refill nodes
        List<Node> orderNodes = nodesPool.stream()
            .filter(node -> node instanceof OrderDeliverNode)
            .collect(Collectors.toCollection(ArrayList::new));
        
        List<Node> refillNodes = nodesPool.stream()
            .filter(node -> node instanceof ProductRefillNode)
            .collect(Collectors.toCollection(ArrayList::new));

        // Assign order nodes to their nearest vehicles
        Random random = new Random();
        for (Node orderNode : orderNodes) {
            // Calculate distances from order position to all vehicle initial positions
            List<PlannerVehicle> sortedVehicles = this.vehicles.stream()
                .sorted((v1, v2) -> {
                    double dist1 = this.getDistances().get(orderNode.getPosition()).get(v1.initialPosition);
                    double dist2 = this.getDistances().get(orderNode.getPosition()).get(v2.initialPosition);
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());

            // Select from top 3-4-5 nearest vehicles (randomly choose how many to consider)
            int topK = 3 + (int)(Math.random() * 3); // Randomly choose 3, 4, or 5
            int selectedVehicleIndex = random.nextInt(Math.min(topK, sortedVehicles.size()));
            PlannerVehicle selectedVehicle = sortedVehicles.get(selectedVehicleIndex);
            
            solution.routes.get(selectedVehicle.id).add(orderNode);
        }

        // Randomly assign refill nodes to vehicles
        for (Node refillNode : refillNodes) {
            PlannerVehicle vehicle = this.vehicles.get(random.nextInt(this.vehicles.size()));
            solution.routes.get(vehicle.id).add(refillNode);
        }

        // Add final nodes to each route
        PlannerWarehouse mainWarehouse = this.warehouses.stream()
            .filter(w -> w.isMain)
            .findFirst()
            .get();

        List<FinalNode> finalNodes = this.getNodes().stream()
            .filter(localNode -> localNode instanceof FinalNode)
            .map(localNode -> (FinalNode) localNode)
            .filter(localNode -> localNode.getPosition().equals(mainWarehouse.position))
            .collect(Collectors.toCollection(ArrayList::new));

        for(int i=0; i<this.vehicles.size(); i++) {
            solution.routes.get(this.vehicles.get(i).id).add(finalNodes.get(i));
        }

        return solution;
    }
}
