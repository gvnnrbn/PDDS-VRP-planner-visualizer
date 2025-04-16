package domain_environment;
import java.util.List;

public class Environment {
    public List<Vehicle> vehicles;
    public List<Order> orders;
    public List<Warehouse> warehouses;
    public List<Blockage> blockages;

    public double vehicleSpeed;
    public double dischargeSpeed;
    public double chargeSpeed;
    public double transferSpeed;
    public double fuelLoadSpeed;

    public int gridLength;
    public int gridWidth;

    public double GLPWeightPerm3;

    public Environment() {
    }

    public List<PathFragment> getShortestPath(Position start, Position end) {
        return AStar.findPath(start, end, gridLength, gridWidth, blockages);
    }
}
