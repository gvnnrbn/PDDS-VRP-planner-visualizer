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

    public PlannerOrder(int id, Time arrivalTime, Position position, int amountGLP, String clientId, Time deadline) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.position = position;
        this.amountGLP = amountGLP;
        this.clientId = clientId;
        this.deadline = deadline;
        this.deliverTime = null;
        this.releaseTime = arrivalTime;
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
