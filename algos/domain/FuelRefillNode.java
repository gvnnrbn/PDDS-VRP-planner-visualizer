package domain;

public class FuelRefillNode extends Node {
    public Warehouse warehouse;

    @Override
    public FuelRefillNode clone() {
        return new FuelRefillNode(id, warehouse);
    }

    public FuelRefillNode(int id, Warehouse warehouse) {
        super(id);
        this.warehouse = warehouse;
    }

    @Override
    public String toString() {
        return String.format("To fully refill fuel at warehouse %d at %s", warehouse.id(), warehouse.position());
    }

    @Override
    public boolean isInfiniteNode() {
        return true;
    }

    @Override
    public Position getPosition() {
        return warehouse.position();
    }
} 