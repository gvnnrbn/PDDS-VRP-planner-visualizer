package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pucp.pdds.backend.algos.scheduler.WeeklyScheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerState;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.entities.PlannerVehicle.VehicleState;
import pucp.pdds.backend.algos.scheduler.DataProvider;
import pucp.pdds.backend.dto.InitMessage;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class SimulationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final DataProvider dataProvider;

    private WeeklyScheduler currentSimulation;
    private Thread simulationThread;
    private final Object simulationLock = new Object();
    private boolean isSimulationActive = false;

    public void isSimulationActive() {
        sendResponse("SIMULATION_STATE", isSimulationActive);
    }
    @Autowired
    public SimulationService(SimpMessagingTemplate messagingTemplate, 
                           DataProvider dataProvider) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");


    public void startSimulation(String fechaInicioStr) {
        synchronized (simulationLock) {
            stopCurrentSimulation();

            try {
                logger.info("Starting simulation - loading fresh data from database...");
                sendResponse("SIMULATION_LOADING", "Loading data from database...");

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(fechaInicioStr);
                JsonNode timeNode = root.path("initialTime");

                int year = timeNode.path("year").asInt();
                int month = timeNode.path("month").asInt();
                int day = timeNode.path("day").asInt();
                int hour = timeNode.path("hour").asInt();
                int minute = timeNode.path("minute").asInt();

                LocalDateTime fechaInicio = LocalDateTime.of(year, month, day, hour, minute);

                // Initialize scheduler state with fresh data from database
                var vehicles = dataProvider.getVehicles();
                var orders = dataProvider.getOrdersForWeek(fechaInicio);
                var blockages = dataProvider.getBlockages();
                var warehouses = dataProvider.getWarehouses();
                var failures = dataProvider.getFailures();
                var maintenances = dataProvider.getMaintenances();
                
                logger.info("Loaded {} vehicles, {} orders, {} blockages, {} warehouses, {} failures, {} maintenances", 
                    vehicles.size(), orders.size(), blockages.size(), warehouses.size(), failures.size(), maintenances.size());

                Position mainWarehousePosition = warehouses.stream().filter(w -> w.isMain).findFirst().orElseThrow().position;
                
                vehicles.forEach(v -> {
                    v.currentFuel = v.maxFuel;
                    v.currentGLP = v.maxGLP;
                    v.initialPosition = mainWarehousePosition;
                    v.position = mainWarehousePosition;
                    v.currentPath = null;
                    v.nextNodeIndex = 0;
                    v.currentFailure = null;
                    v.minutesUntilFailure = 0;
                    v.reincorporationTime = null;
                    v.state = VehicleState.IDLE;
                    v.waitTransition = 0;
                });

                warehouses.forEach(w -> {
                    w.currentGLP = w.maxGLP;
                });

                SchedulerState schedulerState = new SchedulerState(
                    vehicles.stream().map(v -> v.clone()).toList(),
                    orders.stream().map(o -> o.clone()).toList(),
                    blockages.stream().map(b -> b.clone()).toList(),
                    warehouses.stream().map(w -> w.clone()).toList(),
                    failures.stream().map(f->f.clone()).toList(),
                    maintenances.stream().map(m -> m.clone()).toList(),
                    new Time(fechaInicio.getYear(), fechaInicio.getMonthValue(), 
                    fechaInicio.getDayOfMonth(), fechaInicio.getHour(), fechaInicio.getMinute()),
                    120,
                    new Time(fechaInicio.getYear(), fechaInicio.getMonthValue(),
                    fechaInicio.getDayOfMonth(), fechaInicio.getHour(), fechaInicio.getMinute())
                );

                currentSimulation = new WeeklyScheduler(messagingTemplate);
                simulationThread = new Thread(currentSimulation, "simulation-thread");
                currentSimulation.setState(schedulerState);
                simulationThread.start();

                isSimulationActive = true;

                logger.info("Simulation started successfully");
                sendResponse("SIMULATION_STARTED", "Simulation initialized with fresh data from database");
            } catch (Exception e) {
                isSimulationActive = false;
                logger.error("Error starting simulation", e);
                sendResponse("SIMULATION_ERROR", "Error starting simulation: " + e.getMessage());
            }
        }
    }
    
    public void updateFailures(UpdateFailuresMessage message) {
        synchronized (simulationLock) {
            if (currentSimulation != null) {
                try {
                    currentSimulation.updateFailures(message);
                    sendResponse("STATE_UPDATED", "State updated successfully");
                } catch (Exception e) {
                    sendResponse("ERROR", "Failed to update state: " + e.getMessage());
                }
            } else {
                sendResponse("ERROR", "No active simulation");
            }
            isSimulationActive = false;
        }
    }

    public void stopSimulation() {
        synchronized (simulationLock) {
            stopCurrentSimulation();
            sendResponse("SIMULATION_STOPPED", "Simulation stopped");
        }
    }

    private void stopCurrentSimulation() {
        if (currentSimulation != null) {
            currentSimulation.stop();
            if (simulationThread != null && simulationThread.isAlive()) {
                simulationThread.interrupt();
                try {
                    simulationThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            currentSimulation = null;
            simulationThread = null;
        }
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }
}
