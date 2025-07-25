package pucp.pdds.backend.algos.algorithm;

import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.utils.Position;
import pucp.pdds.backend.algos.utils.Time;

public class OrderDeliverNode extends Node {
    public PlannerOrder order;
    public int amountGLP;

    public static int chunkSize = 5; // Max number of m3 of GLP that can be transported in one chunk
    
    // Estrategia de entrega inteligente
    public static int maxCompleteDeliverySize = 15; // Pedidos hasta 15m³ se pueden entregar completos
    public static int emergencyCompleteDeliverySize = 25; // En emergencia, hasta 25m³ completos
    public static int urgentTimeThreshold = 120; // 2 horas para considerar urgente

    @Override
    public OrderDeliverNode clone() {
        return new OrderDeliverNode(id, order, amountGLP);
    }

    public OrderDeliverNode(int id, PlannerOrder order, int amountGLP) {
        super(id);
        this.order = order;
        this.amountGLP = amountGLP;
    }

    /**
     * Determina si un pedido debe entregarse completo o dividirse
     */
    public static boolean shouldDeliverComplete(PlannerOrder order, Time currentTime, int vehicleCapacity, int availableVehicles) {
        // Si el pedido es muy pequeño, entregarlo completo
        if (order.amountGLP <= maxCompleteDeliverySize) {
            return true;
        }
        
        // Si el pedido es urgente (deadline cercano), considerar entrega completa
        long minutesUntilDeadline = currentTime.minutesUntil(order.deadline);
        if (minutesUntilDeadline < urgentTimeThreshold) {
            // En emergencia, permitir entregas completas más grandes
            if (order.isEmergency && order.amountGLP <= emergencyCompleteDeliverySize) {
                return true;
            }
            // Para pedidos urgentes normales, hasta 20m³
            if (order.amountGLP <= 20) {
                return true;
            }
        }
        
        // Si hay un vehículo con capacidad suficiente, entregar completo
        if (order.amountGLP <= vehicleCapacity) {
            return true;
        }
        
        // NUEVA LÓGICA: Si hay muchos vehículos libres, considerar entregas completas más grandes
        if (availableVehicles > 3 && order.amountGLP <= vehicleCapacity * 1.5) {
            return true; // Usar vehículos libres para pedidos grandes
        }
        
        // En otros casos, dividir
        return false;
    }

    /**
     * Calcula el tamaño óptimo de chunk para un pedido
     */
    public static int calculateOptimalChunkSize(PlannerOrder order, Time currentTime, int vehicleCapacity) {
        // Si debe entregarse completo, usar todo el pedido
        if (shouldDeliverComplete(order, currentTime, vehicleCapacity, 1)) { // 1 como valor por defecto
            return order.amountGLP;
        }
        
        // Para pedidos grandes, usar chunks más grandes si no son urgentes
        long minutesUntilDeadline = currentTime.minutesUntil(order.deadline);
        if (minutesUntilDeadline > 240) { // Más de 4 horas
            return Math.min(chunkSize * 2, order.amountGLP); // Chunks dobles
        } else if (minutesUntilDeadline > 120) { // Más de 2 horas
            return Math.min(chunkSize + 2, order.amountGLP); // Chunks ligeramente más grandes
        }
        
        // Para pedidos urgentes, usar chunks estándar
        return Math.min(chunkSize, order.amountGLP);
    }

    @Override
    public String toString() {
        return String.format("To deliver %dm3 of GLP for order %d at %s", amountGLP, order.id, order.position);
    }

    @Override
    public Position getPosition() {
        return order.position;
    }
}
