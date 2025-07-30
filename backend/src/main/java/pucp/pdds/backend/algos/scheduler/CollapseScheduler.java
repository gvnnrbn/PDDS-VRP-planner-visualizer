package pucp.pdds.backend.algos.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import pucp.pdds.backend.algos.algorithm.Algorithm;
import pucp.pdds.backend.algos.algorithm.Solution;
import pucp.pdds.backend.algos.data.DataChunk;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.dto.SimulationResponse;
import pucp.pdds.backend.dto.UpdateFailuresMessage;
import pucp.pdds.backend.dto.collapse.CollapseSimulationResponse;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollapseScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CollapseScheduler.class);
    private final SimpMessagingTemplate messagingTemplate;
    private volatile boolean running = true;
    private SchedulerState state;
    private Lock stateLock = new ReentrantLock();
    private static final DateTimeFormatter SIM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Variables para acumulación de datos (similar a WeeklyScheduler)
    private List<Map<String, Object>> simulacionCompleta = new ArrayList<>();
    private Time tiempoInicio;
    private long tiempoPlanificacionInicio;

    public CollapseScheduler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setState(SchedulerState state) {
        this.state = state;
        // Inicializar acumulación de datos
        simulacionCompleta.clear();
        tiempoInicio = state.getCurrTime().clone();
        tiempoPlanificacionInicio = System.currentTimeMillis();
    }

    public void stop() {
        this.running = false;
        logger.info("Collapse simulation stop requested.");
    }

    @Override
    public void run() {
        logger.info("Collapse simulation thread started.");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                stateLock.lock();
                pucp.pdds.backend.algos.algorithm.Environment environment = new pucp.pdds.backend.algos.algorithm.Environment(
                    state.getActiveVehicles(), 
                    state.getActiveOrders(), 
                    state.getWarehouses(), 
                    state.getActiveBlockages(), 
                    state.getFailures(), 
                    state.getActiveMaintenances(), 
                    state.getCurrTime(), 
                    state.minutesToSimulate
                );
                logger.info("Planning interval for collapse simulation started at " + state.getCurrTime());
                stateLock.unlock();

                Algorithm algorithm = new Algorithm(true);
                Solution sol = algorithm.run(environment, state.minutesToSimulate);

                if (!sol.isFeasible()) {
                    sendError("System collapsed. Could not find a feasible plan for the next " + state.minutesToSimulate + " minutes.");
                    running = false;
                    break;
                }

                state.initializeVehicles();

                for (int i = 0; i < state.minutesToSimulate && running && !Thread.currentThread().isInterrupted(); i++) {
                    stateLock.lock();
                    state.advance(sol, true);
                    onAfterExecution(i, sol);
                    stateLock.unlock();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        running = false;
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Error during collapse simulation", e);
                sendError("Unexpected error during collapse simulation: " + e.getMessage());
                running = false;
            }
        }
        sendSimulationSummary();
        sendResponse("COLLAPSE_SIMULATION_STOPPED", "Collapse simulation finished.");
        logger.info("Collapse simulation thread finished.");
    }

    private void onAfterExecution(int iteration, Solution sol) {
        java.util.Map<String, Object> response = buildSimulationUpdateResponse(state, sol);
        
        // Acumular datos para el resumen
        simulacionCompleta.add(new HashMap<>(response));
        
        sendResponse("SIMULATION_UPDATE", response);
    }

    private java.util.Map<String, Object> buildSimulationUpdateResponse(SchedulerState state, Solution sol) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("minuto", formatSimTime(state.getCurrTime()));
        var almacenes = DataChunk.convertWarehousesToDataChunk(state.getWarehouses());
        var vehiculos = DataChunk.convertVehiclesToDataChunk(state.getVehicles(), sol.routes);
        var pedidos = DataChunk.convertOrdersToDataChunk(state.getPastOrders(), state.getVehicles(), sol.routes, state.getCurrTime());
        var incidencias = DataChunk.convertIncidentsToDataChunk(state.getFailures(), state.getCurrTime());
        var mantenimientos = DataChunk.convertMaintenancesToDataChunk(state.getActiveMaintenances());
        var bloqueos = DataChunk.convertBlockagesToDataChunk(state.getActiveBlockages());
        var indicadores = DataChunk.convertIndicatorsToDataChunk(state.getActiveIndicators());

        response.put("almacenes", almacenes);
        response.put("vehiculos", vehiculos);
        response.put("pedidos", pedidos);
        response.put("incidencias", incidencias);
        response.put("mantenimientos", mantenimientos);
        response.put("bloqueos", bloqueos);
        response.put("indicadores", indicadores);
        return response;
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
        System.out.println("=== COLLAPSE SIMULATION SUMMARY DEBUG ===");
        System.out.println("Fecha inicio: " + formatSimTime(tiempoInicio));
        System.out.println("Fecha fin: " + formatSimTime(state.getCurrTime()));
        System.out.println("Duración: " + duracion);
        System.out.println("Pedidos entregados: " + estadisticas.get("pedidosEntregados"));
        System.out.println("Consumo petróleo: " + estadisticas.get("consumoPetroleo"));
        System.out.println("Tiempo planificación: " + tiempoPlanificacion);
        System.out.println("Minutos simulados: " + simulacionCompleta.size());
        System.out.println("Estadísticas: " + estadisticas);
        System.out.println("=========================================");
        
        // Crear resumen optimizado (últimos 10 minutos si hay muchos datos)
        List<Map<String, Object>> simulacionOptimizada = simulacionCompleta;
        boolean historialReducido = false;
        int totalElementosOriginales = simulacionCompleta.size();
        
        if (simulacionCompleta.size() > 10) {
            simulacionOptimizada = simulacionCompleta.subList(
                Math.max(0, simulacionCompleta.size() - 10), 
                simulacionCompleta.size()
            );
            historialReducido = true;
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("fechaInicio", formatSimTime(tiempoInicio));
        summary.put("fechaFin", formatSimTime(state.getCurrTime()));
        summary.put("duracion", duracion);
        summary.put("pedidosEntregados", estadisticas.get("pedidosEntregados"));
        summary.put("consumoPetroleo", estadisticas.get("consumoPetroleo"));
        summary.put("tiempoPlanificacion", tiempoPlanificacion);
        summary.put("simulacionCompleta", simulacionOptimizada);
        summary.put("estadisticas", estadisticas);
        summary.put("historialReducido", historialReducido);
        summary.put("totalElementosOriginales", totalElementosOriginales);
        
        sendResponse("COLLAPSE_SIMULATION_SUMMARY", summary);
    }

    private Map<String, Object> calculateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int pedidosEntregados = 0;
        double consumoPetroleo = 0.0;
        int totalVehiculos = 0;
        int maxVehiculosActivosEnUnMinuto = 0;
        java.util.Set<String> placasActivasUnicas = new java.util.HashSet<>();
        
        // Calcular estadísticas de los datos acumulados
        for (Map<String, Object> minuto : simulacionCompleta) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> vehiculos = (java.util.List<Object>) minuto.get("vehiculos");
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
        java.util.Set<Integer> pedidosEntregadosUnicos = new java.util.HashSet<>();
        if (!simulacionCompleta.isEmpty()) {
            Map<String, Object> ultimoMinuto = simulacionCompleta.get(simulacionCompleta.size() - 1);
            @SuppressWarnings("unchecked")
            java.util.List<Object> pedidos = (java.util.List<Object>) ultimoMinuto.get("pedidos");
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

    private void sendSimulationUpdate(Solution sol) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("minuto", formatSimTime(state.getCurrTime()));
        var almacenes = DataChunk.convertWarehousesToDataChunk(state.getWarehouses());
        var vehiculos = DataChunk.convertVehiclesToDataChunk(state.getVehicles(), sol.routes);
        var pedidos = DataChunk.convertOrdersToDataChunk(state.getPastOrders(), state.getVehicles(), sol.routes, state.getCurrTime());
        var incidencias = DataChunk.convertIncidentsToDataChunk(state.getFailures(), state.getCurrTime());
        var mantenimientos = DataChunk.convertMaintenancesToDataChunk(state.getActiveMaintenances());
        var bloqueos = DataChunk.convertBlockagesToDataChunk(state.getActiveBlockages());
        var indicadores = DataChunk.convertIndicatorsToDataChunk(state.getActiveIndicators());

        response.put("almacenes", almacenes);
        response.put("vehiculos", vehiculos);
        response.put("pedidos", pedidos);
        response.put("incidencias", incidencias);
        response.put("mantenimientos", mantenimientos);
        response.put("bloqueos", bloqueos);
        response.put("indicadores",indicadores);
        
        sendResponse("SIMULATION_UPDATE", response);
    }
    
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
        if (this.messagingTemplate != null) {
            CollapseSimulationResponse response = new CollapseSimulationResponse(type, data);
            messagingTemplate.convertAndSend("/topic/collapse-simulation", response);
        }
    }

    private void sendError(String message) {
        sendResponse("COLLAPSE_SIMULATION_ERROR", message);
    }
} 