package pucp.pdds.backend.algos.scheduler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Environment;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.entities.PlannerFailure;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.SimulationSummaryDTO;
import pucp.pdds.backend.dto.UpdateFailuresMessage;

@Service
public class WeeklyScheduler implements Runnable {
    private SchedulerState state;
    private Lock stateLock = new ReentrantLock();
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean isRunning;
    private static final DateTimeFormatter SIM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Para acumular datos de la simulación
    private List<Map<String, Object>> simulacionCompleta = new ArrayList<>();
    private Time tiempoInicio;
    private long tiempoPlanificacionInicio;


    private SynchronousQueue<SchedulerState> stateQueue = new SynchronousQueue<>();
    private SynchronousQueue<Solution> solutionQueue = new SynchronousQueue<>();

    private Thread algorithmThread;

    public void setState(SchedulerState state) {
        this.state = state;
    }

    @Autowired
    public WeeklyScheduler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.isRunning = true;
    }

    @Override
    public void run() {
        algorithmThread = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    while (true) {
                        SchedulerState state = stateQueue.take();
                        Environment environment = new Environment(
                            state.getActiveVehicles(), 
                            state.getActiveOrders(), 
                            state.getWarehouses(), 
                            state.getActiveBlockages(), 
                            state.getFailures(), 
                            state.getActiveMaintenances(), 
                            state.getCurrTime(), 
                            state.minutesToSimulate
                        );
                        Algorithm algorithm = new Algorithm(true);
                        Solution sol = algorithm.run(environment, state.minutesToSimulate);
                        System.out.println("======================================");
                        System.out.println("For environment at " + state.getCurrTime() + ":");
                        System.out.println(environment);
                        if (sol.isFeasible(environment)) {
                            System.out.println("I can guarantee there's a feasible solution:");
                            System.out.println(sol);
                        } else {
                            System.out.println("I can't guarantee there's a feasible solution:");
                        }
                        System.out.println("======================================");
                        solutionQueue.put(sol);
                    }
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }
            }
        });

        algorithmThread.start();

        // Inicializar acumulación de datos
        simulacionCompleta.clear();
        tiempoInicio = state.getCurrTime().clone();
        tiempoPlanificacionInicio = System.currentTimeMillis();
        
        Time endSimulationTime = state.getCurrTime().addTime(new Time(0,0,7,0,0));

        stateLock.lock();
        SchedulerState initialState = state.clone();
        stateLock.unlock();

        Environment initialEnvironment = new Environment(
            initialState.getActiveVehicles(), 
            initialState.getActiveOrders(), 
            initialState.getWarehouses(), 
            initialState.getActiveBlockages(), 
            initialState.getFailures(), 
            initialState.getActiveMaintenances(), 
            initialState.getCurrTime(), 
            initialState.minutesToSimulate
        );

        Solution initialSolution = initialEnvironment.getRandomSolution();
        Thread initialSolutionThread = new Thread(() -> {
            try {
                solutionQueue.put(initialSolution);
            } catch (InterruptedException e) {
                isRunning = false;
                Thread.currentThread().interrupt();
                algorithmThread.interrupt();
                sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
            }
        });
        initialSolutionThread.start();

        while(state.getCurrTime().isBefore(endSimulationTime) && isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Solution sol;
                try {
                    sol = solutionQueue.take();
                    debugPrint("SOLUTION TAKEN: " + sol);
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }

                stateLock.lock();
                SchedulerState clonedState = state.clone();
                stateLock.unlock();

                clonedState.initializeVehicles();
                for (int i = 0; i < clonedState.minutesToSimulate; i++) {
                    clonedState.advance(sol, false);
                }

                try {
                    stateQueue.put(clonedState);
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                    algorithmThread.interrupt();
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    return;
                }

                stateLock.lock();
                state.initializeVehicles();
                stateLock.unlock();

                for (int iteration = 0; iteration < state.minutesToSimulate && isRunning && !Thread.currentThread().isInterrupted(); iteration++) {
                    stateLock.lock();
                    state.advance(sol, true);
                    onAfterExecution(iteration, sol);
                    stateLock.unlock();

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        isRunning = false;
                        Thread.currentThread().interrupt();
                        algorithmThread.interrupt();
                        sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                        sendSimulationSummary();
                        return;
                    }

                    stateLock.lock();
                    state.getOrders().stream().filter(o -> o.deadline.isBefore(state.getCurrTime()) && !o.isDelivered()).forEach(o -> {
                        if (!o.hasBeenForgiven) {
                            o.deadline = o.deadline.addMinutes(60);
                            o.hasBeenForgiven = true;
                        }
                    });
                    boolean shouldTerminate = state.getOrders().stream().anyMatch(o -> o.deadline.isBefore(state.getCurrTime()) && !o.isDelivered());
                    stateLock.unlock();

                    if (shouldTerminate) {
                        isRunning = false;
                        Thread.currentThread().interrupt();
                        algorithmThread.interrupt();
                        sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                        sendSimulationSummary();
                        return;
                    }
                }

                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                    algorithmThread.interrupt();
                    sendSimulationSummary();
                    return;
                }
            } catch (Exception e) {
                sendError("Unexpected error during simulation: " + e.getMessage());
                e.printStackTrace();
                algorithmThread.interrupt();
                isRunning = false;
                sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
                sendSimulationSummary();
                return;
            }
        }

        if (!isRunning) {
            sendResponse("SIMULATION_STOPPED", "Simulation stopped by user");
            algorithmThread.interrupt();
            sendSimulationSummary();
        } else {
            // Simulación completada normalmente
            sendSimulationSummary();
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void updateFailures(UpdateFailuresMessage message) {
        stateLock.lock();
        int lastId = state.getFailures().size() > 0 ? state.getFailures().getLast().id : 0;

        PlannerFailure failure = new PlannerFailure(
            lastId + 1, message.getType(), message.getShiftOccurredOn(),
            message.getVehiclePlaque(), null);
        List<PlannerFailure> failures = new ArrayList<>(state.getFailures());
        failures.add(failure);
        state.setFailures(failures);
        stateLock.unlock();
    }

    private void debugPrint(String message) {
        System.out.println(state.getCurrTime() + " | " + message);
    }

    private void onAfterExecution(int iteration, Solution sol) {
        java.util.Map<String, Object> response = buildSimulationUpdateResponse(state, sol);
        
        // Acumular datos para el resumen
        simulacionCompleta.add(new HashMap<>(response));
        
        sendResponse("SIMULATION_UPDATE", response);
            // SimulationVisualizer.draw(state.getActiveVehicles(), state.getActiveBlockages(), state.getCurrTime(), state.minutesToSimulate, state.getWarehouses(), sol);
    }

    private java.util.Map<String, Object> buildSimulationUpdateResponse(SchedulerState state, Solution sol) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("minuto", formatSimTime(state.getCurrTime()));
        var almacenes = DataChunk.convertWarehousesToDataChunk(state.getWarehouses());
        var vehiculos = DataChunk.convertVehiclesToDataChunk(state.getVehicles(), sol.routes);
        var pedidos = DataChunk.convertOrdersToDataChunk(state.getPastOrders(), state.getVehicles(), sol.routes, state.getCurrTime());
        var incidencias = DataChunk.convertIncidentsToDataChunk(state.getFailures());
        var mantenimientos = DataChunk.convertMaintenancesToDataChunk(state.getActiveMaintenances());
        var bloqueos = DataChunk.convertBlockagesToDataChunk(state.getActiveBlockages());
        var indicadores = DataChunk.convertIndicatorsToDataChunk(state.getActiveIndicators());

        formatAlmacenes(almacenes);
        formatVehiculos(vehiculos);
        formatPedidos(pedidos);
        formatIncidencias(incidencias);
        formatMantenimientos(mantenimientos);
        formatBloqueos(bloqueos);

        response.put("almacenes", almacenes);
        response.put("vehiculos", vehiculos);
        response.put("pedidos", pedidos);
        response.put("incidencias", incidencias);
        response.put("mantenimientos", mantenimientos);
        response.put("bloqueos", bloqueos);
        response.put("indicadores", indicadores);
        return response;
    }

    // Formatters for each list
    private void formatAlmacenes(java.util.List<?> almacenes) {
    }
    private void formatVehiculos(java.util.List<?> vehiculos) {
        for (Object v : vehiculos) {
            try {
                var etaField = v.getClass().getField("eta");
                Object eta = etaField.get(v);
                if (eta != null && !eta.toString().equals("-")) {
                    etaField.set(v, formatSimTime(eta));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatPedidos(java.util.List<?> pedidos) {
        for (Object p : pedidos) {
            try {
                var fechaLimiteField = p.getClass().getField("fechaLimite");
                Object fechaLimite = fechaLimiteField.get(p);
                if (fechaLimite != null) {
                    fechaLimiteField.set(p, formatSimTime(fechaLimite));
                }
                var vehiculosAtendiendoField = p.getClass().getField("vehiculosAtendiendo");
                java.util.List<?> vehiculosAtendiendo = (java.util.List<?>) vehiculosAtendiendoField.get(p);
                for (Object va : vehiculosAtendiendo) {
                    try {
                        var etaField = va.getClass().getField("eta");
                        Object eta = etaField.get(va);
                        if (eta != null) {
                            etaField.set(va, formatSimTime(eta));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatIncidencias(java.util.List<?> incidencias) {
        for (Object i : incidencias) {
            try {
                var fechaInicioField = i.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(i);
                if (fechaInicio != null) {
                    fechaInicioField.set(i, formatSimTime(fechaInicio));
                }
                var fechaFinField = i.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(i);
                if (fechaFin != null) {
                    fechaFinField.set(i, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatMantenimientos(java.util.List<?> mantenimientos) {
        for (Object m : mantenimientos) {
            try {
                var fechaInicioField = m.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(m);
                if (fechaInicio != null) {
                    fechaInicioField.set(m, formatSimTime(fechaInicio));
                }
                var fechaFinField = m.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(m);
                if (fechaFin != null) {
                    fechaFinField.set(m, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }
    private void formatBloqueos(java.util.List<?> bloqueos) {
        for (Object b : bloqueos) {
            try {
                var fechaInicioField = b.getClass().getField("fechaInicio");
                Object fechaInicio = fechaInicioField.get(b);
                if (fechaInicio != null) {
                    fechaInicioField.set(b, formatSimTime(fechaInicio));
                }
                var fechaFinField = b.getClass().getField("fechaFin");
                Object fechaFin = fechaFinField.get(b);
                if (fechaFin != null) {
                    fechaFinField.set(b, formatSimTime(fechaFin));
                }
            } catch (Exception ignored) {}
        }
    }

    // Format a Time or LocalDateTime to the required string format
    private String formatSimTime(Object timeObj) {
        if (timeObj == null) return "";
        if (timeObj instanceof pucp.pdds.backend.algos.utils.Time t) {
            return String.format("%02d/%02d/%04d %02d:%02d", t.getDay(), t.getMonth(), t.getYear(), t.getHour(), t.getMinute());
        }
        if (timeObj instanceof java.time.LocalDateTime ldt) {
            return ldt.format(SIM_FORMATTER);
        }
        return timeObj.toString();
    }


    private void sendResponse(String type, Object data) {
        SimulationResponse response = new SimulationResponse(type, data);
        messagingTemplate.convertAndSend("/topic/simulation", response);
    }

    private void sendError(String message) {
        sendResponse("ERROR", message);
    }

    private void sendSimulationSummary() {
        long tiempoPlanificacionFin = System.currentTimeMillis();
        long tiempoPlanificacionMs = tiempoPlanificacionFin - tiempoPlanificacionInicio;
        
        // Calcular estadísticas
        Map<String, Object> estadisticas = calculateStatistics();
        
        // Calcular duración de la simulación
        String duracion = calculateDuration(tiempoInicio, state.getCurrTime());
        
        // Calcular tiempo de planificación
        String tiempoPlanificacion = String.format("%02d:%02d:%02d", 
            tiempoPlanificacionMs / 3600000, 
            (tiempoPlanificacionMs % 3600000) / 60000, 
            (tiempoPlanificacionMs % 60000) / 1000);
        
        // Debug: imprimir información
        System.out.println("=== SIMULATION SUMMARY DEBUG ===");
        System.out.println("Fecha inicio: " + formatSimTime(tiempoInicio));
        System.out.println("Fecha fin: " + formatSimTime(state.getCurrTime()));
        System.out.println("Duración: " + duracion);
        System.out.println("Pedidos entregados: " + estadisticas.get("pedidosEntregados"));
        System.out.println("Consumo petróleo: " + estadisticas.get("consumoPetroleo"));
        System.out.println("Tiempo planificación: " + tiempoPlanificacion);
        System.out.println("Minutos simulados: " + simulacionCompleta.size());
        System.out.println("Estadísticas: " + estadisticas);
        System.out.println("================================");
        
        SimulationSummaryDTO summary = new SimulationSummaryDTO(
            formatSimTime(tiempoInicio),
            formatSimTime(state.getCurrTime()),
            duracion,
            (int) estadisticas.get("pedidosEntregados"),
            (double) estadisticas.get("consumoPetroleo"),
            tiempoPlanificacion,
            simulacionCompleta,
            estadisticas
        );
        
        sendResponse("SIMULATION_SUMMARY", summary);
    }

    private Map<String, Object> calculateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int pedidosEntregados = 0;
        double consumoPetroleo = 0.0;
        int totalVehiculos = 0;
        int maxVehiculosActivosEnUnMinuto = 0;
        Set<String> placasActivasUnicas = new HashSet<>();
        
        // Calcular estadísticas de los datos acumulados
        for (Map<String, Object> minuto : simulacionCompleta) {
            @SuppressWarnings("unchecked")
            List<Object> vehiculos = (List<Object>) minuto.get("vehiculos");
            if (vehiculos != null) {
                totalVehiculos = Math.max(totalVehiculos, vehiculos.size());
                int vehiculosActivosEsteMinuto = 0;
                for (Object v : vehiculos) {
                    try {
                        var estadoField = v.getClass().getField("estado");
                        String estado = (String) estadoField.get(v);
                        if ("ONTHEWAY".equals(estado) || "DELIVERING".equals(estado)) {
                            vehiculosActivosEsteMinuto++;
                            // Contar placa única
                            var placaField = v.getClass().getField("placa");
                            String placa = (String) placaField.get(v);
                            if (placa != null) {
                                placasActivasUnicas.add(placa);
                            }
                        }
                        var combustibleField = v.getClass().getField("combustible");
                        var maxCombustibleField = v.getClass().getField("maxCombustible");
                        int combustible = (int) combustibleField.get(v);
                        int maxCombustible = (int) maxCombustibleField.get(v);
                        consumoPetroleo += (maxCombustible - combustible) * 0.1; // Factor de conversión
                    } catch (Exception ignored) {}
                }
                maxVehiculosActivosEnUnMinuto = Math.max(maxVehiculosActivosEnUnMinuto, vehiculosActivosEsteMinuto);
            }
        }

        // Contar pedidos entregados únicos SOLO en el último minuto simulado
        Set<Integer> pedidosEntregadosUnicos = new HashSet<>();
        if (!simulacionCompleta.isEmpty()) {
            Map<String, Object> ultimoMinuto = simulacionCompleta.get(simulacionCompleta.size() - 1);
            @SuppressWarnings("unchecked")
            List<Object> pedidos = (List<Object>) ultimoMinuto.get("pedidos");
            if (pedidos != null) {
                for (Object p : pedidos) {
                    try {
                        var estadoField = p.getClass().getField("estado");
                        String estado = (String) estadoField.get(p);
                        var idField = p.getClass().getField("idPedido");
                        int idPedido = (int) idField.get(p);
                        if ("Completado".equals(estado)) {
                            pedidosEntregadosUnicos.add(idPedido);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        pedidosEntregados = pedidosEntregadosUnicos.size();

        stats.put("pedidosEntregados", pedidosEntregados);
        stats.put("consumoPetroleo", Math.round(consumoPetroleo * 100.0) / 100.0);
        stats.put("totalVehiculos", totalVehiculos);
        stats.put("maxVehiculosActivosEnUnMinuto", maxVehiculosActivosEnUnMinuto);
        stats.put("vehiculosActivosUnicos", placasActivasUnicas.size());
        stats.put("minutosSimulados", simulacionCompleta.size());
        
        return stats;
    }

    private String calculateDuration(Time inicio, Time fin) {
        int minutos = 0;
        Time temp = inicio.clone();
        
        while (temp.isBefore(fin)) {
            minutos++;
            temp = temp.addMinutes(1);
        }
        
        int horas = minutos / 60;
        int mins = minutos % 60;
        
        return String.format("%02d:%02d:00", horas, mins);
    }
}