package pucp.pdds.backend.algos.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle;
import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerMaintenance;
import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.algorithm.Node;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.algorithm.OrderDeliverNode;
import pucp.pdds.backend.algos.algorithm.ProductRefillNode;
import pucp.pdds.backend.algos.utils.PathBuilder;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.SimulationProperties;
import pucp.pdds.backend.algos.utils.SimulationVisualizer;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.data.DataChunk;

public class Scheduler {
    private SchedulerAgent agent;
    private int minutesToSimulate;
    private Time initialTime;

    private boolean isDebug = false;
    private boolean isVisualize = false;

    private Time currTime;

    private List<PlannerVehicle> vehicles;
    private List<PlannerOrder> orders;
    private List<PlannerBlockage> blockages;
    private List<PlannerWarehouse> warehouses;
    private List<PlannerFailure> failures;
    private List<PlannerMaintenance> maintenances;

    private List<PlannerBlockage> activeBlockages;
    private List<PlannerOrder> activeOrders;
    private List<PlannerMaintenance> activeMaintenances;
    private List<PlannerVehicle> activeVehicles;

    private int totalIntervals;

    private static final int MAX_QUEUE_SIZE = 3; // Maximum number of chunks ahead
    private final BlockingQueue<DataChunk> dataChunkQueue;
    private final AtomicBoolean isRunning;
    private Thread computationThread;
    private Thread ioThread;
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    public Scheduler(SchedulerAgent agent, Time currTime, int totalMinutes, int minutesToSimulate) {
        this.agent = agent;
        this.currTime = currTime;
        this.initialTime = currTime;
        this.minutesToSimulate = minutesToSimulate;
        this.dataChunkQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.isRunning = new AtomicBoolean(true);
        logger.info("Initializing scheduler with {} total minutes and {} minutes per interval", totalMinutes, minutesToSimulate);

        this.vehicles = agent.getVehicles();
        this.orders = agent.getOrders();
        this.blockages = agent.getBlockages();
        this.warehouses = agent.getWarehouses();
        this.failures = agent.getFailures();
        this.maintenances = agent.getMaintenances();

        this.totalIntervals = totalMinutes / minutesToSimulate;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public void setVisualize(boolean isVisualize) {
        this.isVisualize = isVisualize;
    }

    public void run() {
        // Start I/O thread first
        ioThread = new Thread(this::runIOThread);
        ioThread.start();

        // Start computation thread
        computationThread = new Thread(this::runComputationThread);
        computationThread.start();

        try {
            // Wait for both threads to complete
            computationThread.join();
            ioThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
    }

    private void stop() {
        isRunning.set(false);
        if (computationThread != null) {
            computationThread.interrupt();
        }
        if (ioThread != null) {
            ioThread.interrupt();
        }
    }

    private void runComputationThread() {
        logger.info("Starting computation thread");
        for(int i=0; i<totalIntervals && isRunning.get(); i++) {
            try {
                long startTime = System.currentTimeMillis();
                DataChunk dataChunk = computeInterval(i);
                long computationTime = System.currentTimeMillis() - startTime;
                logger.debug("Computed interval {} in {}ms", i, computationTime);
                
                // Try to put with timeout to avoid indefinite blocking
                if (!dataChunkQueue.offer(dataChunk, 5, TimeUnit.SECONDS)) {
                    logger.warn("Queue full, waiting for IO thread to catch up");
                    dataChunkQueue.put(dataChunk);
                }
            } catch (InterruptedException e) {
                logger.warn("Computation thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.info("Computation thread finished");
    }

    private void runIOThread() {
        logger.info("Starting IO thread");
        int sequence = 0;
        while (isRunning.get()) {
            try {
                DataChunk dataChunk = dataChunkQueue.poll(5, TimeUnit.SECONDS);
                if (dataChunk == null) {
                    if (computationThread.isAlive()) {
                        logger.warn("No data available for 5 seconds, but computation thread is still running");
                        continue;
                    }
                    logger.info("No more data to process, IO thread finishing");
                    break;
                }
                sequence++;
                long startTime = System.currentTimeMillis();
                agent.export(dataChunk, sequence);
                long exportTime = System.currentTimeMillis() - startTime;
                logger.debug("Exported interval at {} in {}ms", sequence, exportTime);
                
                if (isVisualize) {
                    SimulationVisualizer.draw(vehicles, activeBlockages, 
                        dataChunk.getDeliveryNodes(), dataChunk.getRefillNodes(), 
                        currTime, minutesToSimulate);
                }
                
                currTime = currTime.addMinutes(minutesToSimulate);
            } catch (InterruptedException e) {
                logger.warn("IO thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.info("IO thread finished");
    }

    private DataChunk computeInterval(int intervalIndex) {
        updateActiveBlockages();
        updateActiveOrders();
        updateActiveMaintenances();
        updateActiveVehicles();

        Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
        debugPrint("Planning interval " + intervalIndex + " started at " + currTime + " with " + activeVehicles.size() + " vehicles and " + activeOrders.size() + " orders");
        Algorithm algorithm = new Algorithm(isDebug);
        Solution sol = algorithm.run(environment, minutesToSimulate);
        debugPrint(sol.getReport());

        if (!sol.isFeasible(environment)) {
            throw new RuntimeException("Solution is not feasible");
        }

        for (PlannerVehicle vehicle : activeVehicles) {
            vehicle.nextNodeIndex = 1;
            if (vehicle.state == PlannerVehicle.VehicleState.FINISHED) {
                vehicle.state = PlannerVehicle.VehicleState.IDLE;
            }
        }

        DataChunk dataChunk = new DataChunk();

        for (int iteration = 0; iteration < minutesToSimulate; iteration++) {
            debugPrint("--- Time: " + currTime + " ---");

            for (PlannerVehicle plannerVehicle : vehicles) {
                // If vehicle should pass to maintenance
                if (plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE && activeMaintenances.stream().anyMatch(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque))) {
                    plannerVehicle.state = PlannerVehicle.VehicleState.MAINTENANCE;
                    plannerVehicle.currentMaintenance = activeMaintenances.stream().filter(maintenance -> maintenance.vehiclePlaque.equals(plannerVehicle.plaque)).findFirst().get();
                    debugPrint("Vehicle " + plannerVehicle.id + " is going into maintenance: " + plannerVehicle.currentMaintenance);
                } 
                
                // If vehicle should leave maintenance
                if (plannerVehicle.state == PlannerVehicle.VehicleState.MAINTENANCE && plannerVehicle.currentMaintenance.endDate.isBefore(currTime)) {
                    plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                    plannerVehicle.currentMaintenance = null;
                    debugPrint("Vehicle " + plannerVehicle.id + " is leaving maintenance");
                }

                // If vehicle should schedule a failure
                Time currTimeCopy = currTime.clone();
                PlannerFailure matchingFailure = failures.stream().filter(
                    failure -> 
                        failure.vehiclePlaque.equals(plannerVehicle.plaque) &&
                        !failure.hasBeenAssigned() &&
                        (failure.shiftOccurredOn == PlannerFailure.Shift.T1 && currTimeCopy.getHour() >= 0 && currTimeCopy.getHour() < 8) ||
                        (failure.shiftOccurredOn == PlannerFailure.Shift.T2 && currTimeCopy.getHour() >= 8 && currTimeCopy.getHour() < 16) ||
                        (failure.shiftOccurredOn == PlannerFailure.Shift.T3 && currTimeCopy.getHour() >= 16 && currTimeCopy.getHour() < 24)
                        ).findFirst().orElse(null);
                if (plannerVehicle.state != PlannerVehicle.VehicleState.STUCK &&
                plannerVehicle.state != PlannerVehicle.VehicleState.MAINTENANCE &&
                 plannerVehicle.currentFailure == null &&
                  matchingFailure != null) {
                    List<Node> route = sol.routes.get(plannerVehicle.id);
                    if (route != null && route.size() > 0) {
                        List<Position> path = PathBuilder.buildPath(plannerVehicle.position, route.get(1).getPosition(), activeBlockages);
                        int distance = (int)(PathBuilder.calculateDistance(path) * (0.05 + Math.random() * 0.35));
                        if (distance > 0) {
                            plannerVehicle.minutesUntilFailure = distance;
                            plannerVehicle.currentFailure = matchingFailure;
                            debugPrint("Assigned failure to happen to vehicle " + plannerVehicle.id + " in " + plannerVehicle.minutesUntilFailure + " minutes");
                        }
                    }
                }
                // If vehicle should fail
                else if (plannerVehicle.minutesUntilFailure <= 0 &&
                 plannerVehicle.currentFailure != null &&
                 plannerVehicle.state != PlannerVehicle.VehicleState.STUCK) {
                    plannerVehicle.state = PlannerVehicle.VehicleState.STUCK;
                    plannerVehicle.currentFailure.timeOccuredOn = currTime;
                    plannerVehicle.currentPath = null;
                    debugPrint("Vehicle " + plannerVehicle.id + " has failed");
                } 
                // If vehicle stuck time has ended
                else if (plannerVehicle.state == PlannerVehicle.VehicleState.STUCK &&
                 plannerVehicle.currentFailure != null &&
                 plannerVehicle.currentFailure.timeOccuredOn.addMinutes(plannerVehicle.currentFailure.type.getMinutesStuck()).isBefore(currTime)) {
                    PlannerWarehouse mainWarehouse = warehouses.stream().filter(warehouse -> warehouse.isMain).findFirst().orElse(null);
                    if (mainWarehouse == null) {
                        throw new RuntimeException("No main warehouse found");
                    }
                    List<Position> path = PathBuilder.buildPath(plannerVehicle.position, mainWarehouse.position, activeBlockages);

                    switch (plannerVehicle.currentFailure.type) {
                        case Ti1:
                            plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                            plannerVehicle.currentFailure = null;
                            debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti1");
                            break;
                        case Ti2:
                            plannerVehicle.state = PlannerVehicle.VehicleState.RETURNING_TO_BASE;
                            Time failureTime = plannerVehicle.currentFailure.timeOccuredOn;
                            Time reincorporationTime;
                            
                            // Determine reincorporation time based on the shift
                            switch (plannerVehicle.currentFailure.shiftOccurredOn) {
                                case T1:  // 00:00-08:00
                                    // Available in T3 of same day
                                    reincorporationTime = new Time(
                                        failureTime.getYear(),
                                        failureTime.getMonth(),
                                        failureTime.getDay(),
                                        16,  // T3 starts at 16:00
                                        0
                                    );
                                    break;
                                case T2:  // 08:00-16:00
                                    // Available in T1 of next day
                                    reincorporationTime = new Time(
                                        failureTime.getYear(),
                                        failureTime.getMonth(),
                                        failureTime.getDay() + 1,
                                        0,  // T1 starts at 00:00
                                        0
                                    );
                                    break;
                                case T3:  // 16:00-24:00
                                    // Available in T2 of next day
                                    reincorporationTime = new Time(
                                        failureTime.getYear(),
                                        failureTime.getMonth(),
                                        failureTime.getDay() + 1,
                                        8,  // T2 starts at 08:00
                                        0
                                    );
                                    break;
                                default:
                                    throw new RuntimeException("Invalid shift");
                            }
                            
                            plannerVehicle.reincorporationTime = reincorporationTime;
                            plannerVehicle.currentFailure = null;
                            debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti2, will be available at " + reincorporationTime);
                            break;
                        case Ti3:
                            plannerVehicle.state = PlannerVehicle.VehicleState.RETURNING_TO_BASE;
                            plannerVehicle.reincorporationTime = new Time(
                                plannerVehicle.currentFailure.timeOccuredOn.getYear(),
                                plannerVehicle.currentFailure.timeOccuredOn.getMonth(),
                                plannerVehicle.currentFailure.timeOccuredOn.getDay() + 2,
                                0,
                                0
                            );
                            plannerVehicle.currentFailure = null;
                            debugPrint("Vehicle " + plannerVehicle.id + " has recovered from failure of type Ti3");
                            break;
                    }
                    plannerVehicle.currentPath = path;
                }

                if (plannerVehicle.state == PlannerVehicle.VehicleState.RETURNING_TO_BASE &&
                plannerVehicle.reincorporationTime.isSameDate(currTime)) {
                    plannerVehicle.state = PlannerVehicle.VehicleState.IDLE;
                    plannerVehicle.currentFailure = null;
                    debugPrint("Vehicle " + plannerVehicle.id + " has finished repairing");
                }
                
                if (plannerVehicle.minutesUntilFailure > 0) {
                    plannerVehicle.minutesUntilFailure--;
                    debugPrint("Vehicle " + plannerVehicle.id + " has " + plannerVehicle.minutesUntilFailure + " minutes until failure");
                }

                updateActiveVehicles();
                if (!activeVehicles.contains(plannerVehicle)) {
                    continue;
                }

                // If no path or path is empty, check if at next node; if not, build path
                if (plannerVehicle.currentPath == null || plannerVehicle.currentPath.isEmpty() ) {
                    List<Node> route = sol.routes.get(plannerVehicle.id);
                    if (route == null || plannerVehicle.nextNodeIndex >= route.size()) {
                        continue;
                    }
                    Node nextNode = route.get(plannerVehicle.nextNodeIndex);
                    // Check if at the node's position
                    if (!plannerVehicle.position.equals(nextNode.getPosition())) {
                        // Not at node yet: build path to it
                        plannerVehicle.currentPath = PathBuilder.buildPath(plannerVehicle.position, nextNode.getPosition(), activeBlockages);
                        continue;
                    }
                    // Has arrived at location
                    debugPrint("Vehicle " + plannerVehicle.id + " has arrived at location of node " + nextNode);
                    plannerVehicle.processNode(nextNode, plannerVehicle, activeOrders, warehouses, currTime);

                    if (plannerVehicle.nextNodeIndex == route.size() - 1) {
                        // Just processed the FinalNode
                        debugPrint("Vehicle " + plannerVehicle.id + " has reached final node");
                        plannerVehicle.state = PlannerVehicle.VehicleState.FINISHED;
                        plannerVehicle.nextNodeIndex++; // Optional: move index past end
                        continue;
                    }
                    plannerVehicle.nextNodeIndex++;
                    // No need to build path here; will do so on next iteration if needed
                } else {
                    if (plannerVehicle.waitTransition > 0) {
                        debugPrint("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
                        plannerVehicle.waitTransition--;
                    } else {
                        plannerVehicle.advancePath(SimulationProperties.speed / 60.0);
                        plannerVehicle.state = PlannerVehicle.VehicleState.ONTHEWAY;
                    }
                }
            }

            // Collect delivery and refill nodes
            List<Node> deliveryNodes = new ArrayList<>();
            List<Node> refillNodes = new ArrayList<>();
            for (PlannerVehicle v : activeVehicles) {
                List<Node> route = sol.routes.get(v.id);
                if (route != null && v.nextNodeIndex < route.size()) {
                    Node nextNode = route.get(v.nextNodeIndex);
                    if (nextNode instanceof OrderDeliverNode) {
                        deliveryNodes.add(nextNode);
                    } else if (nextNode instanceof ProductRefillNode) {
                        refillNodes.add(nextNode);
                    }
                }
            }

            dataChunk.setFechaInicio(initialTime.toLocalDateTime());
            dataChunk.setBloqueos(activeBlockages.stream()
                .map(blockage -> {
                    DataChunk.Bloqueo bloqueo = new DataChunk.Bloqueo(
                        blockage.id,
                        blockage.startTime.toLocalDateTime(),
                        blockage.endTime.toLocalDateTime()
                    );
                    // Add segments if they exist
                    if (blockage.vertices != null) {
                        List<DataChunk.Bloqueo.Segmento> segmentos = blockage.vertices.stream()
                            .map(vertex -> new DataChunk.Bloqueo.Segmento(
                                (int)vertex.x,
                                (int)vertex.y
                            ))
                            .collect(Collectors.toList());
                        bloqueo.setSegmentos(segmentos);
                    }
                    return bloqueo;
                })
                .collect(Collectors.toList()));

            // Create a new simulation minute
            DataChunk.SimulacionMinuto simulationMinuto = new DataChunk.SimulacionMinuto(currTime.minutesSince(initialTime));

            // Map vehicles
            simulationMinuto.setVehiculos(vehicles.stream()
                .map(vehicle -> {
                    DataChunk.Vehiculo vehiculo = new DataChunk.Vehiculo(
                        vehicle.id,
                        vehicle.type,
                        (int)vehicle.currentFuel,
                        vehicle.maxFuel,
                        vehicle.maxGLP,
                        vehicle.currentGLP,
                        vehicle.plaque,
                        (int)vehicle.position.x,
                        (int)vehicle.position.y
                    );
                    // Map route points if they exist
                    if (vehicle.currentPath != null) {
                        List<DataChunk.RutaPunto> rutaActual = vehicle.currentPath.stream()
                            .map(point -> {
                                int glpAmount = 0;
                                // Only try to get next node if vehicle is still active
                                if (sol.routes.containsKey(vehicle.id) && vehicle.nextNodeIndex < sol.routes.get(vehicle.id).size()) {
                                    Node nextNode = sol.routes.get(vehicle.id).get(vehicle.nextNodeIndex);
                                    if (nextNode instanceof OrderDeliverNode) {
                                        glpAmount = ((OrderDeliverNode)nextNode).order.amountGLP;
                                    } else if (nextNode instanceof ProductRefillNode) {
                                        glpAmount = ((ProductRefillNode)nextNode).amountGLP;
                                    }
                                }
                                return new DataChunk.RutaPunto(
                                    (int)point.x,
                                    (int)point.y, 
                                    vehicle.state.toString(),
                                    vehicle.state.toString(),
                                    sol.routes.containsKey(vehicle.id) && vehicle.nextNodeIndex < sol.routes.get(vehicle.id).size() ? 
                                        (sol.routes.get(vehicle.id).get(vehicle.nextNodeIndex) instanceof OrderDeliverNode ? 
                                            ((OrderDeliverNode) sol.routes.get(vehicle.id).get(vehicle.nextNodeIndex)).order.id : 0) : 0,
                                    glpAmount
                                );
                            })
                            .collect(Collectors.toList());
                        vehiculo.setRutaActual(rutaActual);
                    }
                    return vehiculo;
                })
                .collect(Collectors.toList()));

            // Map warehouses
            simulationMinuto.setAlmacenes(warehouses.stream()
                .map(warehouse -> new DataChunk.Almacen(
                    warehouse.id,
                    (int)warehouse.position.x,
                    (int)warehouse.position.y,
                    warehouse.currentGLP,
                    warehouse.maxGLP,
                    warehouse.isMain,
                    warehouse.wasVehicle
                ))
                .collect(Collectors.toList()));

            // Map orders
            simulationMinuto.setPedidos(activeOrders.stream()
                .map(order -> {
                    DataChunk.Pedido pedido = new DataChunk.Pedido(
                        order.id,
                        order.isDelivered() ? "Completado" : "Pendiente",
                        order.amountGLP,
                        order.deadline.toString(),
                        (int)order.position.x,
                        (int)order.position.y
                    );
                    // Map attending vehicles if they exist
                    List<DataChunk.VehiculoAtendiendo> vehiculosAtendiendo = new ArrayList<>();
                    for (PlannerVehicle vehicle : activeVehicles) {
                        if (vehicle.currentPath != null && !vehicle.currentPath.isEmpty()) {
                            Node nextNode = sol.routes.get(vehicle.id).get(vehicle.nextNodeIndex);
                            if (nextNode instanceof OrderDeliverNode && ((OrderDeliverNode) nextNode).order.id == order.id) {
                                vehiculosAtendiendo.add(new DataChunk.VehiculoAtendiendo(
                                    vehicle.plaque,
                                    currTime.toString()
                                ));
                            }
                        }
                    }
                    pedido.setVehiculosAtendiendo(vehiculosAtendiendo);
                    return pedido;
                })
                .collect(Collectors.toList()));

            // Map incidents (failures and maintenances)
            List<DataChunk.Incidencia> incidencias = new ArrayList<>();
            
            // Add failures as incidents
            failures.forEach(failure -> incidencias.add(new DataChunk.Incidencia(
                failure.id,
                failure.timeOccuredOn != null ? failure.timeOccuredOn.toString() : "",
                failure.timeOccuredOn != null ? failure.timeOccuredOn.toString() : "",
                failure.shiftOccurredOn.toString(),
                failure.type.toString(),
                failure.vehiclePlaque,
                failure.timeOccuredOn != null ? "FINISHED" : "ACTIVE"
            )));

            // Add maintenances as incidents
            activeMaintenances.forEach(maintenance -> incidencias.add(new DataChunk.Incidencia(
                maintenance.id,
                maintenance.startDate.toString(),
                maintenance.endDate.toString(),
                "T1", // Default shift since it's not specified in PlannerMaintenance
                "MAINTENANCE",
                maintenance.vehiclePlaque,
                "ACTIVE"
            )));

            simulationMinuto.setIncidencias(incidencias);

            // Add the simulation minute to the data chunk
            dataChunk.getSimulacion().add(simulationMinuto);

            currTime = currTime.addMinutes(1);
        }

        return dataChunk;
    }

    private void updateActiveBlockages() {
        activeBlockages = blockages.stream()
            .filter(blockage -> blockage.isActive(currTime, currTime.addMinutes(minutesToSimulate)))
            .collect(Collectors.toList());
    }

    private void updateActiveOrders() {
        activeOrders = orders.stream()
            .filter(order -> order.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void updateActiveMaintenances() {
        activeMaintenances = maintenances.stream()
            .filter(maintenance -> maintenance.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void updateActiveVehicles() {
        activeVehicles = vehicles.stream()
            .filter(vehicle -> vehicle.isActive(currTime))
            .collect(Collectors.toList());
    }

    private void debugPrint(String message) {
        if (isDebug) {
            System.out.println(currTime + " | " + message);
        }
    }
}
