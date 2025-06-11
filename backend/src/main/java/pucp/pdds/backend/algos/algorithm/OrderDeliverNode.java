package pucp.pdds.backend.algos.algorithm;

import pucp.pdds.backend.algos.entities.PlannerOrder;
import pucp.pdds.backend.algos.utils.Position;

public class OrderDeliverNode extends Node {
    public PlannerOrder order;
    public int amountGLP;

    public static int chunkSize = 10; // Max number of m3 of GLP that can be transported in one chunk

    @Override
    public OrderDeliverNode clone() {
        return new OrderDeliverNode(id, order, amountGLP);
    }

    public OrderDeliverNode(int id, PlannerOrder order, int amountGLP) {
        super(id);
        this.order = order;
        this.amountGLP = amountGLP;
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
