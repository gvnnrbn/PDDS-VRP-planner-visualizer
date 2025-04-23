package domain;

public class ProductRefillNode extends Node {
    public Warehouse warehouse;
    public int amountGLP;

    @Override
    public ProductRefillNode clone() {
        return new ProductRefillNode(id, warehouse, amountGLP);
    }

    public ProductRefillNode(int id, Warehouse warehouse, int amountGLP) {
        super(id);
        this.warehouse = warehouse;
        this.amountGLP = amountGLP;
    }

    @Override
    public String toString() {
        return String.format("To refill %dm3 from warehouse %d at %s", amountGLP, warehouse.id(), warehouse.position());
    }

    @Override
    public boolean isInfiniteNode() {
        return warehouse.isMain();
    }

    @Override
    public Position getPosition() {
        return warehouse.position();
    }
} 