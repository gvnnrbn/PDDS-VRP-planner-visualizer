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

        // Estrategia de entrega inteligente: decidir cuándo dividir pedidos
        System.out.println("🎯 Aplicando estrategia de entrega inteligente...");
        System.out.println("📋 Total de pedidos a procesar: " + orders.size());
        
        // Verificación de seguridad: asegurar que todos los pedidos activos estén incluidos
        List<PlannerOrder> activeOrders = orders.stream()
            .filter(order -> !order.isDelivered() && order.amountGLP > 0)
            .collect(Collectors.toList());
        
        System.out.println("✅ Pedidos activos verificados: " + activeOrders.size());
        
        for (PlannerOrder order : activeOrders) {
            // Encontrar la capacidad máxima de vehículos disponibles
            int maxVehicleCapacity = vehicles.stream()
                .mapToInt(v -> v.maxGLP)
                .max()
                .orElse(OrderDeliverNode.chunkSize);
            
            // Determinar si debe entregarse completo (considerando vehículos disponibles)
            boolean shouldDeliverComplete = OrderDeliverNode.shouldDeliverComplete(order, currentTime, maxVehicleCapacity, vehicles.size());
            
            if (shouldDeliverComplete) {
                // Entregar pedido completo
                nodes.add(new OrderDeliverNode(nodeSerial++, order, order.amountGLP));
                System.out.println("✅ Pedido " + order.id + " (" + order.amountGLP + "m³) - ENTREGA COMPLETA");
            } else {
                // Dividir pedido en chunks inteligentes
            int remainingGLP = order.amountGLP;
                int chunkCount = 0;
                
            while (remainingGLP > 0) {
                    int chunkSize = OrderDeliverNode.calculateOptimalChunkSize(order, currentTime, maxVehicleCapacity);
                    int actualChunkSize = Math.min(chunkSize, remainingGLP);
                    
                    nodes.add(new OrderDeliverNode(nodeSerial++, order, actualChunkSize));
                    remainingGLP -= actualChunkSize;
                    chunkCount++;
                }
                
                System.out.println("📦 Pedido " + order.id + " (" + order.amountGLP + "m³) - DIVIDIDO EN " + chunkCount + " CHUNKS");
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
        System.out.println("🔧 Iniciando heurística inteligente...");
        
        Solution solution = new Solution(this);
        solution.routes = new HashMap<>();

        solution.setStartingTime(this.currentTime);

        // Get all nodes from the environment
        List<Node> nodesPool = this.getNodes().stream()
            .filter(node -> !(node instanceof EmptyNode) && !(node instanceof FinalNode))
            .collect(Collectors.toCollection(ArrayList::new));

        System.out.println("📦 Nodos disponibles: " + nodesPool.size());

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

        System.out.println("🚚 Vehículos: " + this.vehicles.size());
        System.out.println("📋 Pedidos: " + orderNodes.size());
        System.out.println("⛽ Reabastecimientos: " + refillNodes.size());

        // HEURÍSTICA SIMPLIFICADA: Ordenar pedidos por urgencia
        List<Node> sortedOrderNodes = orderNodes.stream()
            .sorted((n1, n2) -> {
                OrderDeliverNode o1 = (OrderDeliverNode) n1;
                OrderDeliverNode o2 = (OrderDeliverNode) n2;
                return o1.order.deadline.compareTo(o2.order.deadline);
            })
            .collect(Collectors.toCollection(ArrayList::new));

        // HEURÍSTICA ANTI-COLAPSO: Priorización inteligente de pedidos
        System.out.println("🔄 Distribuyendo " + sortedOrderNodes.size() + " pedidos entre " + this.vehicles.size() + " vehículos...");
        
        // Crear lista de vehículos disponibles con su carga actual
        List<VehicleLoadInfo> vehicleLoads = new ArrayList<>();
        for (PlannerVehicle vehicle : this.vehicles) {
            vehicleLoads.add(new VehicleLoadInfo(vehicle, 0, 0.0));
        }
        
        // Ordenar pedidos por prioridad inteligente (considerando entrega completa)
        List<Node> prioritizedOrders = new ArrayList<>(sortedOrderNodes);
        prioritizedOrders.sort((n1, n2) -> {
            OrderDeliverNode o1 = (OrderDeliverNode) n1;
            OrderDeliverNode o2 = (OrderDeliverNode) n2;
            
            // Actualizar urgencia de ambos pedidos
            o1.order.updateUrgency(this.currentTime);
            o2.order.updateUrgency(this.currentTime);
            
            // Priorizar por nivel de emergencia
            if (o1.order.isEmergency && !o2.order.isEmergency) return -1;
            if (!o1.order.isEmergency && o2.order.isEmergency) return 1;
            
            // Si ambos son emergencia, priorizar por score de prioridad
            double score1 = o1.order.getPriorityScore(this.currentTime);
            double score2 = o2.order.getPriorityScore(this.currentTime);
            
            // Bonus para pedidos completos (evitar dividir más)
            boolean isComplete1 = o1.amountGLP == o1.order.amountGLP;
            boolean isComplete2 = o2.amountGLP == o2.order.amountGLP;
            
            if (isComplete1 && !isComplete2) {
                score1 += 50; // Bonus para pedidos completos
            } else if (!isComplete1 && isComplete2) {
                score2 += 50;
            }
            
            return Double.compare(score2, score1); // Mayor score primero
        });
        
        // Contar pedidos por prioridad
        long emergencyCount = prioritizedOrders.stream()
            .mapToLong(n -> ((OrderDeliverNode) n).order.isEmergency ? 1 : 0)
            .sum();
        long urgentCount = prioritizedOrders.stream()
            .mapToLong(n -> ((OrderDeliverNode) n).order.priorityLevel == 2 ? 1 : 0)
            .sum();
        
        System.out.println("🚨 " + emergencyCount + " pedidos de emergencia, ⚠️ " + urgentCount + " pedidos urgentes");
        
        // ESTRATEGIA DE ASIGNACIÓN INTELIGENTE: Priorizar vehículos libres para entregas completas
        System.out.println("🚚 Estrategia de asignación de vehículos libres...");
        
        // Separar pedidos completos y parciales
        List<Node> completeOrders = new ArrayList<>();
        List<Node> partialOrders = new ArrayList<>();
        
        for (Node orderNode : prioritizedOrders) {
            OrderDeliverNode deliverNode = (OrderDeliverNode) orderNode;
            if (deliverNode.amountGLP == deliverNode.order.amountGLP) {
                completeOrders.add(orderNode);
            } else {
                partialOrders.add(orderNode);
            }
        }
        
        System.out.println("📊 Pedidos completos: " + completeOrders.size() + ", Parciales: " + partialOrders.size());
        
        // PRIMERA FASE: Asignar pedidos completos a vehículos libres
        System.out.println("🎯 Fase 1: Asignando pedidos completos a vehículos libres...");
        for (Node orderNode : completeOrders) {
            OrderDeliverNode deliverNode = (OrderDeliverNode) orderNode;
            
            // Buscar vehículo libre (sin pedidos asignados) que pueda manejar el pedido completo
            VehicleLoadInfo freeVehicle = findFreeVehicleForCompleteOrder(deliverNode, vehicleLoads, solution);
            
            if (freeVehicle != null) {
                // Asignar a vehículo libre
                solution.routes.get(freeVehicle.vehicle.id).add(orderNode);
                freeVehicle.orderCount++;
                freeVehicle.totalGLP += deliverNode.amountGLP;
                
                System.out.println("🚚✅ VEHÍCULO LIBRE: Pedido " + deliverNode.order.id + 
                                 " (" + deliverNode.amountGLP + "m³ COMPLETO) → " + freeVehicle.vehicle.plaque);
            } else {
                // Si no hay vehículo libre, usar asignación normal
                VehicleLoadInfo bestVehicleInfo = findBestVehicleForPriorityAssignment(deliverNode, vehicleLoads, solution);
                if (bestVehicleInfo != null) {
                    solution.routes.get(bestVehicleInfo.vehicle.id).add(orderNode);
                    bestVehicleInfo.orderCount++;
                    bestVehicleInfo.totalGLP += deliverNode.amountGLP;
                    
                    System.out.println("📦✅ Pedido " + deliverNode.order.id + 
                                     " (" + deliverNode.amountGLP + "m³ COMPLETO) → " + bestVehicleInfo.vehicle.plaque + " (ocupado)");
                }
            }
        }
        
        // SEGUNDA FASE: Asignar pedidos parciales
        System.out.println("🎯 Fase 2: Asignando pedidos parciales...");
        for (Node orderNode : partialOrders) {
            OrderDeliverNode deliverNode = (OrderDeliverNode) orderNode;
            
            // Encontrar el mejor vehículo considerando prioridad y balance
            VehicleLoadInfo bestVehicleInfo = findBestVehicleForPriorityAssignment(deliverNode, vehicleLoads, solution);
            
            if (bestVehicleInfo != null) {
                // Asignar pedido al vehículo
                solution.routes.get(bestVehicleInfo.vehicle.id).add(orderNode);
                
                // Actualizar carga del vehículo
                bestVehicleInfo.orderCount++;
                bestVehicleInfo.totalGLP += deliverNode.amountGLP;
                
                String priorityIcon = deliverNode.order.isEmergency ? "🚨" : 
                                    deliverNode.order.priorityLevel == 2 ? "⚠️" : "📦";
                
                System.out.println(priorityIcon + "📦 Pedido " + deliverNode.order.id + 
                                 " (" + deliverNode.amountGLP + "/" + deliverNode.order.amountGLP + "m³ PARCIAL" + 
                                 ", prioridad " + deliverNode.order.priorityLevel + 
                                 ", urgencia " + String.format("%.1f", deliverNode.order.urgencyScore) + 
                                 ") asignado a vehículo " + bestVehicleInfo.vehicle.id);
            }
        }
        
        // Mostrar distribución final y estadísticas de entrega
        System.out.println("📊 Distribución final de carga:");
        for (VehicleLoadInfo info : vehicleLoads) {
            System.out.println("  🚚 " + info.vehicle.plaque + ": " + info.orderCount + " pedidos, " + info.totalGLP + " GLP");
        }
        
        // Estadísticas de entrega inteligente y uso de vehículos
        long completeDeliveries = prioritizedOrders.stream()
            .mapToLong(n -> {
                OrderDeliverNode node = (OrderDeliverNode) n;
                return (node.amountGLP == node.order.amountGLP) ? 1 : 0;
            })
            .sum();
        
        long partialDeliveries = prioritizedOrders.size() - completeDeliveries;
        
        // Contar vehículos utilizados
        long usedVehicles = vehicleLoads.stream()
            .mapToLong(info -> info.orderCount > 0 ? 1 : 0)
            .sum();
        
        long freeVehicles = vehicleLoads.size() - usedVehicles;
        
        System.out.println("🎯 Estadísticas de entrega inteligente:");
        System.out.println("  ✅ Entregas completas: " + completeDeliveries);
        System.out.println("  📦 Entregas parciales: " + partialDeliveries);
        System.out.println("  📈 Eficiencia: " + String.format("%.1f", (double)completeDeliveries/prioritizedOrders.size()*100) + "% completas");
        System.out.println("🚚 Uso de flota:");
        System.out.println("  🚚 Vehículos utilizados: " + usedVehicles + "/" + vehicleLoads.size());
        System.out.println("  🆓 Vehículos libres: " + freeVehicles);
        System.out.println("  📊 Utilización: " + String.format("%.1f", (double)usedVehicles/vehicleLoads.size()*100) + "%");

        // HEURÍSTICA MEJORADA: Asignar reabastecimientos estratégicamente
        System.out.println("⛽ Asignando " + refillNodes.size() + " reabastecimientos...");
        
        for (Node refillNode : refillNodes) {
            ProductRefillNode refillNodeCast = (ProductRefillNode) refillNode;
            
            // Encontrar el vehículo que más necesita reabastecimiento
            VehicleLoadInfo vehicleWithMostNeed = findVehicleWithMostRefillNeed(refillNodeCast, vehicleLoads, solution);
            
            // Insertar reabastecimiento antes del primer pedido que necesite GLP
            insertRefillBeforeFirstNeed(vehicleWithMostNeed.vehicle.id, refillNode, solution);
            
            System.out.println("⛽ Reabastecimiento asignado a " + vehicleWithMostNeed.vehicle.plaque + 
                             " (GLP: " + refillNodeCast.amountGLP + ")");
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

        // Verificación final de seguridad
        long totalOrderNodes = nodes.stream()
            .filter(node -> node instanceof OrderDeliverNode)
            .count();
        
        System.out.println("✅ Heurística inteligente completada");
        System.out.println("🔍 Verificación final: " + totalOrderNodes + " nodos de pedidos generados");
        System.out.println("📊 Nodos totales generados: " + nodes.size());

        return solution;
    }

    /**
     * Clase auxiliar para tracking de carga de vehículos
     */
    private static class VehicleLoadInfo {
        PlannerVehicle vehicle;
        int orderCount;
        double totalGLP;
        
        VehicleLoadInfo(PlannerVehicle vehicle, int orderCount, double totalGLP) {
            this.vehicle = vehicle;
            this.orderCount = orderCount;
            this.totalGLP = totalGLP;
        }
    }

    /**
     * Encuentra el mejor vehículo para asignación por prioridad (anti-colapso)
     */
    private VehicleLoadInfo findBestVehicleForPriorityAssignment(OrderDeliverNode orderNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            PlannerVehicle vehicle = vehicleInfo.vehicle;
            
            // Verificar si el vehículo tiene suficiente GLP
            if (vehicle.currentGLP < orderNode.amountGLP) {
                continue;
            }

            // Calcular score considerando prioridad anti-colapso
            double distanceScore = calculateDistanceScore(vehicle, orderNode);
            double loadBalanceScore = -vehicleInfo.totalGLP;
            double orderBalanceScore = -vehicleInfo.orderCount * 10;
            double priorityScore = orderNode.order.getPriorityScore(this.currentTime) * 100; // Prioridad alta
            double capacityEfficiencyScore = calculateCapacityEfficiencyScore(vehicle, orderNode);
            
            // Score total con énfasis en prioridad para pedidos críticos
            double totalScore;
            if (orderNode.order.isEmergency) {
                // Para emergencias: prioridad máxima
                totalScore = priorityScore * 0.6 + distanceScore * 0.2 + loadBalanceScore * 0.1 + orderBalanceScore * 0.1;
            } else {
                // Para pedidos normales: balance
                totalScore = distanceScore * 0.3 + loadBalanceScore * 0.25 + orderBalanceScore * 0.25 + priorityScore * 0.1 + capacityEfficiencyScore * 0.1;
            }

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestVehicle = vehicleInfo;
            }
        }

        // Si ningún vehículo tiene capacidad, elegir el que tenga más GLP
        if (bestVehicle == null) {
            bestVehicle = vehicleLoads.stream()
                .max((v1, v2) -> Integer.compare(v1.vehicle.currentGLP, v2.vehicle.currentGLP))
                .orElse(vehicleLoads.get(0));
        }

        return bestVehicle;
    }

    /**
     * Encuentra un vehículo libre para entrega completa
     */
    private VehicleLoadInfo findFreeVehicleForCompleteOrder(OrderDeliverNode orderNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestFreeVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            PlannerVehicle vehicle = vehicleInfo.vehicle;
            
            // Solo considerar vehículos libres (sin pedidos asignados)
            if (vehicleInfo.orderCount > 0) {
                continue;
            }
            
            // Verificar si el vehículo tiene suficiente GLP para el pedido completo
            if (vehicle.currentGLP < orderNode.amountGLP) {
                continue;
            }
            
            // Calcular score para vehículo libre
            double distanceScore = calculateDistanceScore(vehicle, orderNode);
            double capacityEfficiencyScore = calculateCapacityEfficiencyScore(vehicle, orderNode);
            double priorityScore = orderNode.order.getPriorityScore(this.currentTime) * 50; // Prioridad alta para libres
            
            // Score total para vehículos libres
            double totalScore = distanceScore * 0.4 + capacityEfficiencyScore * 0.3 + priorityScore * 0.3;
            
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestFreeVehicle = vehicleInfo;
            }
        }

        return bestFreeVehicle;
    }

    /**
     * Encuentra el mejor vehículo para asignación balanceada
     */
    private VehicleLoadInfo findBestVehicleForBalancedAssignment(OrderDeliverNode orderNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            PlannerVehicle vehicle = vehicleInfo.vehicle;
            
            // Verificar si el vehículo tiene suficiente GLP
            if (vehicle.currentGLP < orderNode.amountGLP) {
                continue;
            }

            // Calcular score considerando múltiples factores optimizados
            double distanceScore = calculateDistanceScore(vehicle, orderNode);
            double loadBalanceScore = -vehicleInfo.totalGLP; // Preferir vehículos con menos carga
            double orderBalanceScore = -vehicleInfo.orderCount * 15; // Más peso al balance de pedidos
            double urgencyScore = calculateUrgencyScore(orderNode);
            double capacityEfficiencyScore = calculateCapacityEfficiencyScore(vehicle, orderNode);
            
            // Score total ponderado optimizado
            double totalScore = distanceScore * 0.25 + loadBalanceScore * 0.25 + orderBalanceScore * 0.25 + urgencyScore * 0.15 + capacityEfficiencyScore * 0.1;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestVehicle = vehicleInfo;
            }
        }

        // Si ningún vehículo tiene capacidad, elegir el que tenga más GLP
        if (bestVehicle == null) {
            bestVehicle = vehicleLoads.stream()
                .max((v1, v2) -> Integer.compare(v1.vehicle.currentGLP, v2.vehicle.currentGLP))
                .orElse(vehicleLoads.get(0));
        }

        return bestVehicle;
    }

    /**
     * Agrupa pedidos por proximidad geográfica
     */
    private List<List<Node>> clusterOrdersByProximity(List<Node> orderNodes) {
        List<List<Node>> clusters = new ArrayList<>();
        List<Node> unassignedOrders = new ArrayList<>(orderNodes);
        
        while (!unassignedOrders.isEmpty()) {
            Node seedOrder = unassignedOrders.remove(0);
            List<Node> cluster = new ArrayList<>();
            cluster.add(seedOrder);
            
            // Buscar pedidos cercanos al seed
            for (int i = unassignedOrders.size() - 1; i >= 0; i--) {
                Node candidateOrder = unassignedOrders.get(i);
                
                double distance = this.getDistances().get(seedOrder.getPosition()).get(candidateOrder.getPosition());
                
                // Si está a menos de 10 unidades de distancia, agregar al cluster
                if (distance <= 10.0 && cluster.size() < 5) { // Máximo 5 pedidos por cluster
                    cluster.add(candidateOrder);
                    unassignedOrders.remove(i);
                }
            }
            
            clusters.add(cluster);
        }
        
        return clusters;
    }

    /**
     * Encuentra el mejor vehículo para un cluster de pedidos
     */
    private VehicleLoadInfo findBestVehicleForCluster(List<Node> cluster, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            PlannerVehicle vehicle = vehicleInfo.vehicle;
            
            // Calcular GLP total requerido por el cluster
            int totalGLPRequired = cluster.stream()
                .mapToInt(node -> ((OrderDeliverNode) node).amountGLP)
                .sum();
            
            // Verificar si el vehículo tiene suficiente GLP
            if (vehicle.currentGLP < totalGLPRequired) {
                continue;
            }

            // Calcular score para el cluster
            double distanceScore = calculateClusterDistanceScore(vehicle, cluster);
            double loadBalanceScore = -vehicleInfo.totalGLP;
            double orderBalanceScore = -vehicleInfo.orderCount * 10;
            double clusterEfficiencyScore = calculateClusterEfficiencyScore(cluster);
            
            double totalScore = distanceScore * 0.4 + loadBalanceScore * 0.3 + orderBalanceScore * 0.2 + clusterEfficiencyScore * 0.1;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestVehicle = vehicleInfo;
            }
        }

        // Si ningún vehículo puede manejar el cluster, dividirlo
        if (bestVehicle == null) {
            bestVehicle = vehicleLoads.stream()
                .max((v1, v2) -> Integer.compare(v1.vehicle.currentGLP, v2.vehicle.currentGLP))
                .orElse(vehicleLoads.get(0));
        }

        return bestVehicle;
    }

    /**
     * Calcula score de distancia para un cluster
     */
    private double calculateClusterDistanceScore(PlannerVehicle vehicle, List<Node> cluster) {
        double totalDistance = 0.0;
        Position currentPos = vehicle.initialPosition;
        
        for (Node orderNode : cluster) {
            totalDistance += this.getDistances().get(currentPos).get(orderNode.getPosition());
            currentPos = orderNode.getPosition();
        }
        
        return -totalDistance; // Distancia negativa para maximizar
    }

    /**
     * Calcula score de eficiencia para un cluster
     */
    private double calculateClusterEfficiencyScore(List<Node> cluster) {
        if (cluster.size() <= 1) return 0.0;
        
        // Calcular la densidad del cluster (pedidos por unidad de área)
        double totalDistance = 0.0;
        int connections = 0;
        
        for (int i = 0; i < cluster.size(); i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                totalDistance += this.getDistances().get(cluster.get(i).getPosition()).get(cluster.get(j).getPosition());
                connections++;
            }
        }
        
        if (connections == 0) return 0.0;
        
        double averageDistance = totalDistance / connections;
        return -averageDistance; // Distancia promedio negativa para maximizar
    }

    /**
     * Encuentra el vehículo que más necesita reabastecimiento
     */
    private VehicleLoadInfo findVehicleWithMostRefillNeed(ProductRefillNode refillNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestNeed = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            PlannerVehicle vehicle = vehicleInfo.vehicle;
            
            // Calcular necesidad de reabastecimiento
            double glpNeed = Math.max(0, vehicle.maxGLP - vehicle.currentGLP);
            double fuelNeed = Math.max(0, vehicle.maxFuel - vehicle.currentFuel);
            
            // Score basado en necesidad total
            double totalNeed = glpNeed + fuelNeed * 0.5; // GLP es más importante
            
            if (totalNeed > bestNeed) {
                bestNeed = totalNeed;
                bestVehicle = vehicleInfo;
            }
        }

        return bestVehicle != null ? bestVehicle : vehicleLoads.get(0);
    }

    /**
     * Inserta reabastecimiento antes del primer pedido que necesite GLP
     */
    private void insertRefillBeforeFirstNeed(int vehicleId, Node refillNode, Solution solution) {
        List<Node> route = solution.routes.get(vehicleId);
        
        // Buscar la primera posición donde se necesite GLP
        int insertPosition = route.size(); // Por defecto al final
        
        for (int i = 1; i < route.size(); i++) {
            Node node = route.get(i);
            if (node instanceof OrderDeliverNode) {
                OrderDeliverNode orderNode = (OrderDeliverNode) node;
                
                // Verificar si necesitamos GLP antes de este pedido
                VehicleState stateBeforeOrder = simulateVehicleStateUpTo(vehicleId, route, i);
                if (stateBeforeOrder.currentGLP < orderNode.amountGLP) {
                    insertPosition = i;
                    break;
                }
            }
        }
        
        route.add(insertPosition, refillNode);
    }

    /**
     * Encuentra el vehículo más cercano que tenga capacidad para el pedido
     */
    private PlannerVehicle findClosestVehicleWithCapacity(OrderDeliverNode orderNode, Solution solution) {
        PlannerVehicle bestVehicle = null;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (PlannerVehicle vehicle : this.vehicles) {
            // Verificar si el vehículo tiene suficiente GLP
            if (vehicle.currentGLP < orderNode.amountGLP) {
                continue;
            }

            // Calcular distancia al pedido
            double distance = this.getDistances().get(vehicle.initialPosition).get(orderNode.getPosition());
            
            if (distance < bestDistance) {
                bestDistance = distance;
                bestVehicle = vehicle;
            }
        }

        // Si ningún vehículo tiene capacidad, elegir el que tenga más GLP
        if (bestVehicle == null) {
            bestVehicle = this.vehicles.stream()
                .max((v1, v2) -> Integer.compare(v1.currentGLP, v2.currentGLP))
                .orElse(this.vehicles.get(0));
        }

        return bestVehicle;
    }

    /**
     * Encuentra el mejor vehículo para un pedido basado en:
     * 1. Capacidad disponible (GLP y combustible)
     * 2. Distancia al pedido
     * 3. Tiempo estimado de llegada
     */
    private PlannerVehicle findBestVehicleForOrder(OrderDeliverNode orderNode, Solution solution) {
        PlannerVehicle bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlannerVehicle vehicle : this.vehicles) {
            // Simular la ruta actual del vehículo para calcular su estado
            VehicleState simulatedState = simulateVehicleState(vehicle, solution.routes.get(vehicle.id));
            
            // Verificar si el vehículo puede manejar este pedido
            if (simulatedState.currentGLP < orderNode.amountGLP) {
                continue; // No tiene suficiente GLP
            }

            // Calcular score basado en múltiples factores
            double distanceScore = calculateDistanceScore(vehicle, orderNode);
            double capacityScore = calculateCapacityScore(simulatedState, orderNode);
            double urgencyScore = calculateUrgencyScore(orderNode);
            double loadBalanceScore = calculateLoadBalanceScore(vehicle, solution);

            double totalScore = distanceScore + capacityScore + urgencyScore + loadBalanceScore;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestVehicle = vehicle;
            }
        }

        // Si ningún vehículo puede manejar el pedido, elegir el que tenga más GLP
        if (bestVehicle == null) {
            bestVehicle = this.vehicles.stream()
                .max((v1, v2) -> Double.compare(v1.currentGLP, v2.currentGLP))
                .orElse(this.vehicles.get(0));
        }

        return bestVehicle;
    }

    /**
     * Simula el estado actual de un vehículo basado en su ruta
     */
    private VehicleState simulateVehicleState(PlannerVehicle vehicle, List<Node> route) {
        VehicleState state = new VehicleState();
        state.currentGLP = vehicle.currentGLP;
        state.currentFuel = vehicle.currentFuel;
        state.currentPosition = vehicle.initialPosition;

        for (Node node : route) {
            if (node instanceof OrderDeliverNode) {
                OrderDeliverNode deliverNode = (OrderDeliverNode) node;
                state.currentGLP -= deliverNode.amountGLP;
            } else if (node instanceof ProductRefillNode) {
                ProductRefillNode refillNode = (ProductRefillNode) node;
                state.currentGLP += refillNode.amountGLP;
                if (!refillNode.warehouse.wasVehicle) {
                    state.currentFuel = vehicle.maxFuel;
                }
            }
        }

        return state;
    }

    /**
     * Calcula score basado en la distancia al pedido
     */
    private double calculateDistanceScore(PlannerVehicle vehicle, OrderDeliverNode orderNode) {
        double distance = this.getDistances().get(vehicle.initialPosition).get(orderNode.getPosition());
        // Preferir vehículos más cercanos (score más alto = mejor)
        return -distance; // Distancia negativa para maximizar
    }

    /**
     * Calcula score basado en la capacidad disponible
     */
    private double calculateCapacityScore(VehicleState state, OrderDeliverNode orderNode) {
        double glpRatio = (double) state.currentGLP / orderNode.amountGLP;
        double fuelRatio = state.currentFuel / 100.0; // Normalizar combustible
        
        // Preferir vehículos con capacidad adecuada pero no excesiva
        return Math.min(glpRatio, 3.0) + fuelRatio;
    }

    /**
     * Calcula score basado en la urgencia del pedido (mejorado)
     */
    private double calculateUrgencyScore(OrderDeliverNode orderNode) {
        long minutesUntilDeadline = this.currentTime.minutesUntil(orderNode.order.deadline);
        
        // Pedidos críticos (menos de 1 hora) tienen prioridad máxima
        if (minutesUntilDeadline < 60) {
            return 200.0;
        }
        // Pedidos muy urgentes (menos de 2 horas) tienen prioridad alta
        else if (minutesUntilDeadline < 120) {
            return 150.0;
        }
        // Pedidos urgentes (menos de 4 horas) tienen prioridad media-alta
        else if (minutesUntilDeadline < 240) {
            return 100.0;
        }
        // Pedidos moderadamente urgentes (menos de 8 horas) tienen prioridad media
        else if (minutesUntilDeadline < 480) {
            return 50.0;
        }
        // Pedidos normales
        else {
            return 20.0;
        }
    }

    /**
     * Calcula score basado en la eficiencia de capacidad del vehículo
     */
    private double calculateCapacityEfficiencyScore(PlannerVehicle vehicle, OrderDeliverNode orderNode) {
        // Preferir vehículos que usen su capacidad de manera eficiente
        double glpUtilization = (double) vehicle.currentGLP / vehicle.maxGLP;
        double fuelUtilization = vehicle.currentFuel / vehicle.maxFuel;
        
        // Score más alto para vehículos con capacidad bien utilizada (no vacíos, no llenos)
        double optimalGLPUtilization = 0.7; // 70% de utilización es óptimo
        double optimalFuelUtilization = 0.8; // 80% de utilización es óptimo
        
        double glpEfficiency = 1.0 - Math.abs(glpUtilization - optimalGLPUtilization);
        double fuelEfficiency = 1.0 - Math.abs(fuelUtilization - optimalFuelUtilization);
        
        return glpEfficiency + fuelEfficiency;
    }

    /**
     * Calcula score basado en el balance de carga entre vehículos
     */
    private double calculateLoadBalanceScore(PlannerVehicle vehicle, Solution solution) {
        List<Node> route = solution.routes.get(vehicle.id);
        int currentLoad = route.stream()
            .filter(node -> node instanceof OrderDeliverNode)
            .mapToInt(node -> ((OrderDeliverNode) node).amountGLP)
            .sum();

        // Preferir vehículos con menos carga para balancear
        return -currentLoad;
    }

    /**
     * Inserta un pedido en la posición óptima de la ruta
     */
    private void insertOrderInOptimalPosition(int vehicleId, Node orderNode, Solution solution) {
        List<Node> route = solution.routes.get(vehicleId);
        
        // Si la ruta está vacía (solo tiene el nodo inicial), insertar al final
        if (route.size() <= 1) {
            route.add(orderNode);
            return;
        }

        // Encontrar la mejor posición para insertar
        int bestPosition = 1; // Después del nodo inicial
        double bestCost = Double.POSITIVE_INFINITY;

        for (int i = 1; i <= route.size(); i++) {
            double insertionCost = calculateInsertionCost(route, orderNode, i);
            if (insertionCost < bestCost) {
                bestCost = insertionCost;
                bestPosition = i;
            }
        }

        route.add(bestPosition, orderNode);
    }

    /**
     * Calcula el costo de insertar un nodo en una posición específica
     */
    private double calculateInsertionCost(List<Node> route, Node newNode, int position) {
        if (position == 0) return Double.POSITIVE_INFINITY; // No insertar al inicio

        double cost = 0.0;
        
        // Calcular distancia adicional
        if (position < route.size()) {
            Node prevNode = route.get(position - 1);
            Node nextNode = route.get(position);
            
            double originalDistance = this.getDistances().get(prevNode.getPosition()).get(nextNode.getPosition());
            double newDistance1 = this.getDistances().get(prevNode.getPosition()).get(newNode.getPosition());
            double newDistance2 = this.getDistances().get(newNode.getPosition()).get(nextNode.getPosition());
            
            cost += (newDistance1 + newDistance2 - originalDistance);
        } else {
            // Insertar al final
            Node prevNode = route.get(route.size() - 1);
            cost += this.getDistances().get(prevNode.getPosition()).get(newNode.getPosition());
        }

        return cost;
    }

    /**
     * Asigna nodos de reabastecimiento de manera inteligente
     */
    private void assignRefillNodesIntelligently(List<Node> refillNodes, Solution solution) {
        for (Node refillNode : refillNodes) {
            ProductRefillNode refillNodeCast = (ProductRefillNode) refillNode;
            
            // Encontrar el vehículo que más necesita reabastecimiento
            PlannerVehicle bestVehicle = findBestVehicleForRefill(refillNodeCast, solution);
            
            // Insertar en la mejor posición
            insertRefillInOptimalPosition(bestVehicle.id, refillNode, solution);
        }
    }

    /**
     * Encuentra el mejor vehículo para reabastecimiento
     */
    private PlannerVehicle findBestVehicleForRefill(ProductRefillNode refillNode, Solution solution) {
        PlannerVehicle bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlannerVehicle vehicle : this.vehicles) {
            VehicleState state = simulateVehicleState(vehicle, solution.routes.get(vehicle.id));
            
            // Score basado en necesidad de reabastecimiento
            double glpNeed = Math.max(0, vehicle.maxGLP - state.currentGLP);
            double fuelNeed = Math.max(0, vehicle.maxFuel - state.currentFuel);
            
            double score = glpNeed + fuelNeed * 0.5; // GLP es más importante
            
            if (score > bestScore) {
                bestScore = score;
                bestVehicle = vehicle;
            }
        }

        return bestVehicle != null ? bestVehicle : this.vehicles.get(0);
    }

    /**
     * Inserta un nodo de reabastecimiento en la posición óptima
     */
    private void insertRefillInOptimalPosition(int vehicleId, Node refillNode, Solution solution) {
        List<Node> route = solution.routes.get(vehicleId);
        
        // Buscar la mejor posición: antes de un pedido que necesite GLP
        int bestPosition = route.size(); // Por defecto al final
        
        for (int i = 1; i < route.size(); i++) {
            Node node = route.get(i);
            if (node instanceof OrderDeliverNode) {
                OrderDeliverNode orderNode = (OrderDeliverNode) node;
                
                // Verificar si necesitamos GLP antes de este pedido
                VehicleState stateBeforeOrder = simulateVehicleStateUpTo(vehicleId, route, i);
                if (stateBeforeOrder.currentGLP < orderNode.amountGLP) {
                    bestPosition = i;
                    break;
                }
            }
        }
        
        route.add(bestPosition, refillNode);
    }

    /**
     * Simula el estado del vehículo hasta una posición específica en la ruta
     */
    private VehicleState simulateVehicleStateUpTo(int vehicleId, List<Node> route, int position) {
        PlannerVehicle vehicle = this.vehicles.stream()
            .filter(v -> v.id == vehicleId)
            .findFirst()
            .orElse(this.vehicles.get(0));
            
        VehicleState state = new VehicleState();
        state.currentGLP = vehicle.currentGLP;
        state.currentFuel = vehicle.currentFuel;
        state.currentPosition = vehicle.initialPosition;

        for (int i = 0; i < position && i < route.size(); i++) {
            Node node = route.get(i);
            if (node instanceof OrderDeliverNode) {
                OrderDeliverNode deliverNode = (OrderDeliverNode) node;
                state.currentGLP -= deliverNode.amountGLP;
            } else if (node instanceof ProductRefillNode) {
                ProductRefillNode refillNode = (ProductRefillNode) node;
                state.currentGLP += refillNode.amountGLP;
                if (!refillNode.warehouse.wasVehicle) {
                    state.currentFuel = vehicle.maxFuel;
                }
            }
        }

        return state;
    }

    /**
     * Clase auxiliar para simular el estado de un vehículo
     */
    private static class VehicleState {
        int currentGLP;
        double currentFuel;
        Position currentPosition;
    }
}
