package utils;

import domain.*;
import scheduler.ScheduleState;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationEngine {
    public static void apply(Solution plan, ScheduleState state, int minutesToSimulate) {
        Time startTime = state.currentTime;
        Time endTime = startTime.addMinutes(minutesToSimulate);

        // Copiar pedidos y vehículos del estado
        List<Order> updatedOrders = new ArrayList<>(state.pendingOrders);
        List<Vehicle> updatedVehicles = new ArrayList<>();

        Map<Integer, Order> orderMap = updatedOrders.stream()
                .collect(Collectors.toMap(Order::id, o -> o));

        Map<Integer, Warehouse> warehouseMap = state.warehouses.stream()
                .collect(Collectors.toMap(Warehouse::id, w -> w));


        /*
         * loop (i = 0; i <minutesToSImulate; i+=1) // probar primero con 1 min
                for (vehiculo in Vehiculos)
         * 
         */
        for (Vehicle vehicle : state.vehicles) {
            List<Node> route = plan.routes.get(vehicle.id());
            if (route == null || route.size() < 2) {
                updatedVehicles.add(vehicle);
                continue;
            }

            Position currentPos = vehicle.initialPosition();
            double fuel = vehicle.currentFuel();
            int glp = vehicle.currentGLP();
            Time currentTime = state.currentTime;

            for (int i = 0; i < route.size() - 1; i++) {
                Node from = route.get(i);
                Node to = route.get(i + 1);

                int distance = Environment.calculateManhattanDistance(from.getPosition(), to.getPosition());
                int timeSpent = (int) Math.ceil((double) distance / Environment.speed * 60); // min
                Time arrivalTime = currentTime.addMinutes(timeSpent);

                if (arrivalTime.isAfter(endTime))
                    break;

                // Avanzar el tiempo
                currentTime = arrivalTime;
                fuel -= Environment.calculateFuelCost(from, to, new Environment().getDistances(), vehicle); // simplified
                currentPos = to.getPosition();

                // Manejar entrega o recarga
                if (to instanceof OrderDeliverNode deliverNode) {
                    int deliverAmount = deliverNode.amountGLP;

                    if (glp >= deliverAmount) {
                        glp -= deliverAmount;
                        Order o = orderMap.get(deliverNode.order.id());
                        if (o != null) {
                            int remaining = o.amountGLP() - deliverAmount;
                            if (remaining <= 0) {
                                orderMap.remove(o.id());
                            } else {
                                orderMap.put(o.id(), new Order(o.id(), remaining, o.position(), o.deadline()));
                            }
                        }
                        currentTime = currentTime.addMinutes(Environment.timeAfterDelivery);
                    }
                } else if (to instanceof ProductRefillNode refillNode) {
                    glp += refillNode.amountGLP;
                    fuel = vehicle.maxFuel();
                    currentTime = currentTime.addMinutes(Environment.timeAfterRefill);
                }
            }

            updatedVehicles.add(new Vehicle(vehicle.id(), vehicle.weight(), vehicle.maxFuel(), fuel, vehicle.maxGLP(),
                    glp, currentPos));
        }

        // Actualizar el estado
        state.advanceTime(minutesToSimulate);
        state.updateVehicles(updatedVehicles);
        state.updateOrders(new ArrayList<>(orderMap.values()));
    }
}