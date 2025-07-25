package pucp.pdds.backend.algos.entities;

import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.Time;
import pucp.pdds.backend.model.Pedido;
import java.time.LocalDateTime;

public class PlannerOrder implements Cloneable {
    public int id;
    public Time arrivalTime;
    public Position position;
    public int amountGLP;
    public String clientId;
    public Time deadline;
    public Time deliverTime;
    public Time releaseTime;

    // Sistema anti-colapso INFINITO - nunca colapsar
    public int timesForgiven = 0;
    public static int timesToForgive = Integer.MAX_VALUE; // INFINITO - nunca dejar de perdonar
    public static int forgivenTime = 180; // 3 horas por extensión
    
    // Nuevas estrategias anti-colapso
    public boolean isEmergency = false;
    public int priorityLevel = 1; // 1=normal, 2=urgente, 3=crítico
    public Time originalDeadline; // Para tracking
    public double urgencyScore = 0.0;
    
    // Sistema de deadline dinámico INFINITO
    public static int maxDeadlineExtension = Integer.MAX_VALUE; // INFINITO - sin límite
    public static int emergencyThreshold = 60; // 1 hora antes del deadline = emergencia
    public static int criticalThreshold = 30; // 30 minutos = crítico

    public PlannerOrder(int id, Time arrivalTime, Position position, int amountGLP, String clientId, Time deadline) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.position = position;
        this.amountGLP = amountGLP;
        this.clientId = clientId;
        this.deadline = deadline;
        this.originalDeadline = deadline.clone();
        this.deliverTime = null;
        this.releaseTime = arrivalTime;
        this.urgencyScore = calculateInitialUrgency();
    }

    /**
     * Calcula la urgencia inicial del pedido
     */
    private double calculateInitialUrgency() {
        long minutesToDeadline = arrivalTime.minutesUntil(deadline);
        if (minutesToDeadline < emergencyThreshold) {
            return 10.0; // Crítico
        } else if (minutesToDeadline < 180) {
            return 5.0; // Urgente
        } else {
            return 1.0; // Normal
        }
    }

    /**
     * Actualiza la urgencia basada en el tiempo actual
     */
    public void updateUrgency(Time currentTime) {
        long minutesToDeadline = currentTime.minutesUntil(deadline);
        
        if (minutesToDeadline < emergencyThreshold) {
            this.isEmergency = true;
            this.priorityLevel = 3;
            this.urgencyScore = 10.0 + (emergencyThreshold - minutesToDeadline);
        } else if (minutesToDeadline < 180) {
            this.priorityLevel = 2;
            this.urgencyScore = 5.0 + (180 - minutesToDeadline) / 30.0;
        } else {
            this.priorityLevel = 1;
            this.urgencyScore = 1.0;
        }
    }

    /**
     * Extiende el deadline de manera INFINITA - nunca falla
     */
    public boolean extendDeadline(Time currentTime, int additionalMinutes) {
        // SISTEMA INFINITO: Siempre permitir extensión
        if (additionalMinutes <= 0) {
            return false; // Solo fallar si no hay minutos para agregar
        }
        
        // Calcular extensión inteligente basada en urgencia
        int smartExtension = calculateSmartExtension(currentTime, additionalMinutes);
        
        this.deadline = this.deadline.addMinutes(smartExtension);
        this.timesForgiven++;
        this.updateUrgency(currentTime);
        
        String urgencyLevel = this.isEmergency ? "🚨 CRÍTICO" : 
                            this.priorityLevel == 2 ? "⚠️ URGENTE" : "📦 NORMAL";
        
        System.out.println("🔄 " + urgencyLevel + " - Pedido " + this.id + " extendido por " + smartExtension + 
                         " minutos. Nuevo deadline: " + this.deadline + 
                         " (extensión #" + this.timesForgiven + ")");
        return true;
    }
    
    /**
     * Calcula extensión inteligente basada en urgencia
     */
    private int calculateSmartExtension(Time currentTime, int baseExtension) {
        long minutesUntilDeadline = currentTime.minutesUntil(this.deadline);
        
        // Si está en modo crítico (menos de 30 min), extensión más agresiva
        if (minutesUntilDeadline < criticalThreshold) {
            return Math.max(baseExtension * 2, 240); // Mínimo 4 horas
        }
        
        // Si está en emergencia (menos de 1 hora), extensión moderada
        if (minutesUntilDeadline < emergencyThreshold) {
            return Math.max(baseExtension, 180); // Mínimo 3 horas
        }
        
        // Si está urgente (menos de 2 horas), extensión estándar
        if (minutesUntilDeadline < 120) {
            return Math.max(baseExtension, 120); // Mínimo 2 horas
        }
        
        // Extensión normal
        return baseExtension;
    }

    /**
     * Estrategia de emergencia INFINITA: nunca falla
     */
    public void activateEmergencyMode(Time currentTime) {
        if (!this.isEmergency) {
            this.isEmergency = true;
            this.priorityLevel = 3;
            
            // Extensión de emergencia INFINITA - siempre funciona
            int emergencyExtension = calculateEmergencyExtension(currentTime);
            this.deadline = this.deadline.addMinutes(emergencyExtension);
            this.timesForgiven++;
            
            System.out.println("🚨 EMERGENCIA INFINITA: Pedido " + this.id + " activado modo crítico. " +
                             "Extendido por " + emergencyExtension + " minutos. Nuevo deadline: " + this.deadline);
        }
    }
    
    /**
     * Calcula extensión de emergencia basada en historial
     */
    private int calculateEmergencyExtension(Time currentTime) {
        long minutesUntilDeadline = currentTime.minutesUntil(this.deadline);
        
        // Si ya está muy tarde, extensión más agresiva
        if (minutesUntilDeadline < 0) {
            return 360; // 6 horas si ya pasó el deadline
        }
        
        // Si está crítico, extensión moderada
        if (minutesUntilDeadline < criticalThreshold) {
            return 240; // 4 horas
        }
        
        // Extensión estándar de emergencia
        return 180; // 3 horas
    }

    /**
     * Verifica si el pedido está en riesgo de colapso (INFINITO)
     */
    public boolean isAtRiskOfCollapse(Time currentTime) {
        long minutesUntilDeadline = currentTime.minutesUntil(deadline);
        
        // SISTEMA INFINITO: Solo está en riesgo si ya pasó el deadline
        // y no se ha extendido recientemente
        if (minutesUntilDeadline < 0) {
            // Si ya pasó el deadline, está en riesgo
            return true;
        }
        
        // Si está muy cerca del deadline (menos de 30 min), está en riesgo
        if (minutesUntilDeadline < criticalThreshold) {
            return true;
        }
        
        return false;
    }

    /**
     * Obtiene el score de prioridad para el algoritmo
     */
    public double getPriorityScore(Time currentTime) {
        updateUrgency(currentTime);
        
        double baseScore = this.urgencyScore;
        double glpMultiplier = Math.log10(this.amountGLP + 1); // Más GLP = más importante
        double timeMultiplier = 1.0 + (this.timesForgiven * 0.5); // Más extensiones = más urgente
        
        return baseScore * glpMultiplier * timeMultiplier;
    }

    public static PlannerOrder fromEntity(Pedido pedido) {
        Time arrivalTime = new Time(
            pedido.getFechaRegistro().getYear(),
            pedido.getFechaRegistro().getMonthValue(),
            pedido.getFechaRegistro().getDayOfMonth(),
            pedido.getFechaRegistro().getHour(),
            pedido.getFechaRegistro().getMinute()
        );
        
        Time deadline = new Time(
            pedido.getFechaRegistro().getYear(),
            pedido.getFechaRegistro().getMonthValue(),
            pedido.getFechaRegistro().getDayOfMonth(),
            pedido.getFechaRegistro().getHour(),
            pedido.getFechaRegistro().getMinute()
        ).addMinutes(pedido.getTiempoTolerancia() * 60);
        
        Position position = new Position(pedido.getPosicionX(), pedido.getPosicionY());
        
        return new PlannerOrder(
            pedido.getId().intValue(),
            arrivalTime,
            position,
            pedido.getCantidadGLP(),
            pedido.getCodigoCliente(),
            deadline
        );
    }

    public boolean isDelivered() {
        return deliverTime != null;
    }

    public boolean isLate(Time currentTime) {
        return currentTime.isAfter(deadline);
    }

    @Override
    public String toString() {
        return "PlannerOrder{" +
            "id=" + id +
            ", arrivalTime=" + arrivalTime.toString() +
            ", position=" + position.toString() +
            ", amountGLP=" + amountGLP + "m3" +
            ", clientId='" + clientId + "'" +
            ", deadline=" + deadline.toString() +
            ", deliverTime=" + (deliverTime != null ? deliverTime.toString() : "null") +
            ", releaseTime=" + releaseTime.toString() +
            ", isDelivered=" + isDelivered() +
            '}';
    }

    @Override
    public PlannerOrder clone() {
        try {
            PlannerOrder clone = new PlannerOrder(
                this.id,
                this.arrivalTime.clone(),
                this.position.clone(),
                this.amountGLP,
                this.clientId,
                this.deadline.clone()
            );
            clone.releaseTime = this.releaseTime.clone();
            clone.deliverTime = this.deliverTime != null ? this.deliverTime.clone() : null;
            return clone;
        } catch (Exception e) {
            throw new AssertionError("Clone failed: " + e.getMessage());
        }
    }

    public boolean isActive(Time currentTime) {
        return deliverTime == null && currentTime.isAfter(arrivalTime) && currentTime.isBefore(deadline);
    }
}
