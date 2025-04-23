package domain;

public class OrderDeliverNode extends Node {
    public Order order;
    public int amountGLP;

    @Override
    public OrderDeliverNode clone() {
        return new OrderDeliverNode(id, order, amountGLP);
    }

    public OrderDeliverNode(int id, Order order, int amountGLP) {
        super(id);
        this.order = order;
        this.amountGLP = amountGLP;
    }

    @Override
    public String toString() {
        return String.format("To deliver %dm3 of GLP for order %d at %s", amountGLP, order.id(), order.position());
    }

    @Override
    public boolean isInfiniteNode() {
        return false;
    }

    @Override
    public Position getPosition() {
        return order.position();
    }
} 
