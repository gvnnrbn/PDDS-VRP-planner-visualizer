package pucp.pdds.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pucp.pdds.backend.algos.scheduler.CollapseScheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerState;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.algos.scheduler.DataProvider;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.collapse.CollapseSimulationResponse;

@Service
public class CollapseService {
    private static final Logger logger = LoggerFactory.getLogger(CollapseService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final DataProvider dataProvider;

    private CollapseScheduler currentSimulation;
    private Thread simulationThread;
    private final Object simulationLock = new Object();
    private boolean isSimulationActive = false;

    @Autowired
    public CollapseService(SimpMessagingTemplate messagingTemplate, 
                           DataProvider dataProvider) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
    }

    public void startSimulation(String fechaInicioStr) {
        synchronized (simulationLock) {
            stopCurrentSimulation();

            try {
                logger.info("Starting collapse simulation - loading fresh data from database...");
                sendResponse("COLLAPSE_SIMULATION_LOADING", "Loading data from database...");

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(fechaInicioStr);
                JsonNode timeNode = root.path("initialTime");

                int year = timeNode.path("year").asInt();
                int month = timeNode.path("month").asInt();
                int day = timeNode.path("day").asInt();
                int hour = timeNode.path("hour").asInt();
                int minute = timeNode.path("minute").asInt();

                LocalDateTime fechaInicio = LocalDateTime.of(year, month, day, hour, minute);

                var vehicles = dataProvider.getVehicles();
                var orders = dataProvider.getOrdersForWeek(fechaInicio);
                var blockages = dataProvider.getBlockages();
                var warehouses = dataProvider.getWarehouses();
                var failures = dataProvider.getFailures();
                var maintenances = dataProvider.getMaintenances();
                
                logger.info("Loaded {} vehicles, {} orders, {} blockages, {} warehouses, {} failures, {} maintenances", 
                    vehicles.size(), orders.size(), blockages.size(), warehouses.size(), failures.size(), maintenances.size());
                
                vehicles.forEach(v -> {
                    v.currentFuel = v.maxFuel;
                    v.currentGLP = v.maxGLP;
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
                    60,
                    new Time(fechaInicio.getYear(), fechaInicio.getMonthValue(),
                    fechaInicio.getDayOfMonth(), fechaInicio.getHour(), fechaInicio.getMinute())
                );

                currentSimulation = new CollapseScheduler(messagingTemplate);
                currentSimulation.setState(schedulerState);
                simulationThread = new Thread(currentSimulation, "collapse-simulation-thread");
                simulationThread.start();

                isSimulationActive = true;

                logger.info("Collapse simulation started successfully");
                sendResponse("COLLAPSE_SIMULATION_STARTED", "Collapse simulation initialized with fresh data");
            } catch (Exception e) {
                isSimulationActive = false;
                logger.error("Error starting collapse simulation", e);
                sendResponse("COLLAPSE_SIMULATION_ERROR", "Error starting collapse simulation: " + e.getMessage());
            }
        }
    }
    
    public void stopSimulation() {
        synchronized (simulationLock) {
            stopCurrentSimulation();
            sendResponse("COLLAPSE_SIMULATION_STOPPED", "Collapse simulation stopped");
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
        isSimulationActive = false;
    }

    private void sendResponse(String type, Object data) {
        CollapseSimulationResponse response = new CollapseSimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/collapse-simulation", response);
    }
} 