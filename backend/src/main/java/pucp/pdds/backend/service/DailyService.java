package pucp.pdds.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.entities.PlannerBlockage;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.entities.PlannerVehicle.VehicleState;
import pucp.pdds.backend.algos.scheduler.DailyScheduler;
import pucp.pdds.backend.algos.scheduler.DataProvider;
import pucp.pdds.backend.algos.scheduler.SchedulerState;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;

@Service
public class DailyService {
    private static final Logger logger = LoggerFactory.getLogger(DailyService.class);

    private final SimpMessagingTemplate messagingTemplate;  
    private final DataProvider dataProvider;

    private DailyScheduler currentSimulation;
    private Thread simulationThread;
    private final Object simulationLock = new Object();
    private boolean isSimulationActive = false;

    Time startTime;

    public void isSimulationActive() {
        sendResponse("SIMULATION_STATE", isSimulationActive);
    }

    @Autowired
    public DailyService(SimpMessagingTemplate messagingTemplate, DataProvider dataProvider) {
        this.messagingTemplate = messagingTemplate;
        this.dataProvider = dataProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            logger.info("Initializing daily service");

            LocalDateTime fechaInicio = LocalDateTime.now();

            startTime = new Time(fechaInicio.getYear(), fechaInicio.getMonthValue(), fechaInicio.getDayOfMonth(), fechaInicio.getHour(), fechaInicio.getMinute());

            var vehicles = dataProvider.getVehicles(); // GETS ALL AT THE START
            var orders = new ArrayList<PlannerOrder>(); // GETS NONE
            var blockages = dataProvider.getCurrentBlockages(startTime); // GETS CURRENT ACTIVE
            var warehouses = dataProvider.getWarehouses(); // GETS ALL AT THE START
            var failures = new ArrayList<PlannerFailure>(); // GETS NONE
            var maintenances = dataProvider.getMaintenances(); // GETS ALL AT THE START

            logger.info("Loaded {} vehicles, {} orders, {} blockages, {} warehouses, {} failures, {} maintenances", 
                vehicles.size(), orders.size(), blockages.size(), warehouses.size(), failures.size(), maintenances.size());

            System.out.println("[SHOW] Blockages loaded at the start: " + blockages.size());
            for (PlannerBlockage blockage : blockages) {
                System.out.println("[SHOW] Blockage: " + blockage.id);
            }

            vehicles.forEach(v -> {
                v.currentFuel = v.maxFuel;
                v.currentGLP = v.maxGLP;
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
               1,
               new Time(fechaInicio.getYear(), fechaInicio.getMonthValue(),
                fechaInicio.getDayOfMonth(), fechaInicio.getHour(), fechaInicio.getMinute())
            );

            currentSimulation = new DailyScheduler(messagingTemplate, dataProvider);
            currentSimulation.setState(schedulerState);
            simulationThread = new Thread(currentSimulation, "daily-operation-thread");
            simulationThread.start();

            isSimulationActive = true;

            logger.info("Daily operation started successfully");
        } catch (Exception e) {
            isSimulationActive = false;
            logger.error("Error initializing daily service", e);
        } 
    }

    public void refetchData() {
        synchronized (simulationLock) {
            try {
                currentSimulation.refetchData(startTime);
                sendResponse("SIMULATION_UPDATED", "Simulation updated successfully");
            } catch (Exception e) {
                sendResponse("ERROR", "Failed to update simulation: " + e.getMessage());
            }
        }
    }

    public void fetchData() {
        try {
            currentSimulation.fetchData();
        } catch (Exception e) {
            sendResponse("ERROR", "Failed to fetch data: " + e.getMessage());
        }
    }

    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }
}
