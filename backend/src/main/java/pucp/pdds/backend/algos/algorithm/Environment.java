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

    // Clase auxiliar para tracking de carga de vehículos
    private static class VehicleLoadInfo {
        public PlannerVehicle vehicle;
        public int orderCount;
        public double totalGLP;

        public VehicleLoadInfo(PlannerVehicle vehicle, int orderCount, double totalGLP) {
            this.vehicle = vehicle;
            this.orderCount = orderCount;
            this.totalGLP = totalGLP;
        }
    }

    /**
     * Encuentra el mejor vehículo para asignación de prioridad
     */
    private VehicleLoadInfo findBestVehicleForPriorityAssignment(OrderDeliverNode deliverNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            // Verificar capacidad
            if (vehicleInfo.totalGLP + deliverNode.amountGLP > vehicleInfo.vehicle.maxGLP) {
                continue; // Vehículo no tiene capacidad
            }

            // Calcular score basado en múltiples factores
            double distanceScore = calculateDistanceScore(deliverNode, vehicleInfo.vehicle, solution);
            double loadBalanceScore = calculateLoadBalanceScore(vehicleInfo, vehicleLoads);
            double capacityEfficiencyScore = calculateCapacityEfficiencyScore(deliverNode, vehicleInfo.vehicle);

            double totalScore = distanceScore + loadBalanceScore + capacityEfficiencyScore;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestVehicle = vehicleInfo;
            }
        }

        return bestVehicle;
    }

    /**
     * Encuentra un vehículo libre para entrega completa
     */
    private VehicleLoadInfo findFreeVehicleForCompleteOrder(OrderDeliverNode deliverNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            // Vehículo libre = sin pedidos asignados
            if (vehicleInfo.orderCount == 0) {
                // Verificar que pueda manejar el pedido completo
                if (deliverNode.amountGLP <= vehicleInfo.vehicle.maxGLP) {
                    return vehicleInfo;
                }
            }
        }
        return null; // No hay vehículos libres disponibles
    }

    /**
     * Calcula score de distancia para asignación
     */
    private double calculateDistanceScore(OrderDeliverNode deliverNode, PlannerVehicle vehicle, Solution solution) {
        // Obtener la posición actual del vehículo (último nodo en su ruta)
        List<Node> vehicleRoute = solution.routes.get(vehicle.id);
        Position vehicleCurrentPosition = vehicleRoute.get(vehicleRoute.size() - 1).getPosition();
        
        // Calcular distancia al pedido
        double distance = this.getDistances().get(vehicleCurrentPosition).get(deliverNode.getPosition());
        
        // Menor distancia = mejor score
        return -distance; // Negativo porque queremos minimizar distancia
    }

    /**
     * Calcula score de balance de carga
     */
    private double calculateLoadBalanceScore(VehicleLoadInfo vehicleInfo, List<VehicleLoadInfo> allVehicles) {
        // Calcular carga promedio
        double avgLoad = allVehicles.stream()
            .mapToDouble(v -> v.totalGLP)
            .average()
            .orElse(0.0);
        
        // Score mejor si está cerca del promedio (balance)
        double loadDifference = Math.abs(vehicleInfo.totalGLP - avgLoad);
        return -loadDifference; // Negativo porque queremos balance
    }

    /**
     * Calcula score de eficiencia de capacidad
     */
    private double calculateCapacityEfficiencyScore(OrderDeliverNode deliverNode, PlannerVehicle vehicle) {
        // Preferir vehículos que usen mejor su capacidad
        double capacityUtilization = (vehicle.currentGLP + deliverNode.amountGLP) / (double) vehicle.maxGLP;
        
        // Score óptimo alrededor del 80% de utilización
        double optimalUtilization = 0.8;
        double utilizationDifference = Math.abs(capacityUtilization - optimalUtilization);
        
        return -utilizationDifference; // Menor diferencia = mejor score
    }

    /**
     * Encuentra el vehículo que más necesita reabastecimiento
     */
    private VehicleLoadInfo findVehicleWithMostRefillNeed(ProductRefillNode refillNode, List<VehicleLoadInfo> vehicleLoads, Solution solution) {
        VehicleLoadInfo bestVehicle = null;
        double bestNeedScore = Double.NEGATIVE_INFINITY;

        for (VehicleLoadInfo vehicleInfo : vehicleLoads) {
            // Calcular cuánto GLP necesita el vehículo
            double currentGLP = vehicleInfo.vehicle.currentGLP;
            double maxGLP = vehicleInfo.vehicle.maxGLP;
            double needScore = (maxGLP - currentGLP) / maxGLP; // 0 = lleno, 1 = vacío

            if (needScore > bestNeedScore) {
                bestNeedScore = needScore;
                bestVehicle = vehicleInfo;
            }
        }

        return bestVehicle != null ? bestVehicle : vehicleLoads.get(0); // Fallback
    }

    /**
     * Inserta reabastecimiento antes del primer pedido que necesite GLP
     */
    private void insertRefillBeforeFirstNeed(int vehicleId, Node refillNode, Solution solution) {
        List<Node> route = solution.routes.get(vehicleId);
        
        // Buscar la posición donde insertar el reabastecimiento
        int insertPosition = route.size() - 1; // Por defecto al final (antes del nodo final)
        
        // Simular la ruta para encontrar dónde se necesita GLP
        double currentGLP = this.vehicles.stream()
            .filter(v -> v.id == vehicleId)
            .findFirst()
            .map(v -> (double) v.currentGLP)
            .orElse(0.0);
        
        for (int i = 1; i < route.size() - 1; i++) { // Saltar nodo inicial y final
            Node node = route.get(i);
            if (node instanceof OrderDeliverNode) {
                OrderDeliverNode deliverNode = (OrderDeliverNode) node;
                currentGLP -= deliverNode.amountGLP;
                
                if (currentGLP < 0) {
                    // Necesitamos reabastecimiento antes de este pedido
                    insertPosition = i;
                    break;
                }
            } else if (node instanceof ProductRefillNode) {
                ProductRefillNode refillNodeCast = (ProductRefillNode) node;
                currentGLP += refillNodeCast.amountGLP;
            }
        }
        
        // Insertar el reabastecimiento
        route.add(insertPosition, refillNode);
    }
}
