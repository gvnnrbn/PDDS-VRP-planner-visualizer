package pucp.pdds.backend.algos.algorithm;

import pucp.pdds.backend.algos.entities.PlannerWarehouse;
import pucp.pdds.backend.algos.utils.Position;

public class ProductRefillNode extends Node {
    public PlannerWarehouse warehouse;
    public int amountGLP;

    public static int chunkSize = 5; // Max number of m3 of GLP that can be refilled in one chunk

    @Override
    public ProductRefillNode clone() {
        return new ProductRefillNode(id, warehouse, amountGLP);
    }

    public ProductRefillNode(int id, PlannerWarehouse warehouse, int amountGLP) {
        super(id);
        this.warehouse = warehouse;
        this.amountGLP = amountGLP;
    }

    @Override
    public String toString() {
        return String.format("To refill %dm3 from warehouse %d at %s", amountGLP, warehouse.id, warehouse.position);
    }

    @Override
    public Position getPosition() {
        return warehouse.position;
    }
} 