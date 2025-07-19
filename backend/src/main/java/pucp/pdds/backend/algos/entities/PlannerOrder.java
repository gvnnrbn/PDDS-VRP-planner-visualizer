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

    // Sistema anti-colapso mejorado
    public int timesForgiven = 0;
    public static int timesToForgive = 3; // Aumentado de 2 a 3
    public static int forgivenTime = 180; // Aumentado de 120 a 180 minutos
    
    // Nuevas estrategias anti-colapso
    public boolean isEmergency = false;
    public int priorityLevel = 1; // 1=normal, 2=urgente, 3=crítico
    public Time originalDeadline; // Para tracking
    public double urgencyScore = 0.0;
    
    // Sistema de deadline dinámico
    public static int maxDeadlineExtension = 480; // 8 horas máximo
    public static int emergencyThreshold = 60; // 1 hora antes del deadline = emergencia

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
     * Extiende el deadline de manera inteligente
     */
    public boolean extendDeadline(Time currentTime, int additionalMinutes) {
        if (timesForgiven >= timesToForgive) {
            return false; // No más extensiones permitidas
        }
        
        // Verificar que no exceda el máximo
        long totalExtension = currentTime.minutesUntil(originalDeadline) + additionalMinutes;
        if (totalExtension > maxDeadlineExtension) {
            additionalMinutes = (int) Math.max(0, maxDeadlineExtension - currentTime.minutesUntil(originalDeadline));
        }
        
        if (additionalMinutes > 0) {
            this.deadline = this.deadline.addMinutes(additionalMinutes);
            this.timesForgiven++;
            this.updateUrgency(currentTime);
            
            System.out.println("🔄 Pedido " + this.id + " extendido por " + additionalMinutes + 
                             " minutos. Nuevo deadline: " + this.deadline + 
                             " (extensión #" + this.timesForgiven + ")");
            return true;
        }
        
        return false;
    }

    /**
     * Estrategia de emergencia: deadline crítico
     */
    public void activateEmergencyMode(Time currentTime) {
        if (!this.isEmergency && this.timesForgiven < timesToForgive) {
            this.isEmergency = true;
            this.priorityLevel = 3;
            
            // Extender deadline de emergencia
            int emergencyExtension = Math.min(120, maxDeadlineExtension - timesForgiven * forgivenTime);
            this.deadline = this.deadline.addMinutes(emergencyExtension);
            this.timesForgiven++;
            
            System.out.println("🚨 EMERGENCIA: Pedido " + this.id + " activado modo crítico. " +
                             "Nuevo deadline: " + this.deadline);
        }
    }

    /**
     * Verifica si el pedido está en riesgo de colapso
     */
    public boolean isAtRiskOfCollapse(Time currentTime) {
        long minutesToDeadline = currentTime.minutesUntil(deadline);
        return minutesToDeadline < emergencyThreshold && timesForgiven >= timesToForgive;
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
