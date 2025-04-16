import java.util.List;

public class Environment {
    List<Vehicle> vehicles;
    List<Order> orders;
    List<Warehouse> warehouses;
    List<Blockage> blockages;

    int vehicleSpeed;
    int dischargeSpeed;
    int chargeSpeed;
    int transferSpeed;
    int fuelLoadSpeed;

    int gridLength;
    int gridWidth;

    int GLPWeightPerm3;

    public Environment() {
    }

    public List<PathFragment> getShortestPath(Position start, Position end) {
        return AStar.findPath(start, end, gridLength, gridWidth, blockages);
    }
}
