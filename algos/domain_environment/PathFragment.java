package domain_environment;
/* 
 * Linear path fragment between two positions.
*/

public class PathFragment {
    public Position start;
    public Position end;

    public int getDistance() {
        return Math.abs(end.x - start.x) + Math.abs(end.y - start.y); // Manhattan distance in grid
    }

    public double calculateTimeSpent(Environment environment) {
        // Fixed speed
        return getDistance() / environment.vehicleSpeed;
    }

    public double calculateFuelSpent(Vehicle vehicle, Environment environment) {
        // Formula for fuel consumption
        return getDistance() * (environment.GLPWeightPerm3 * vehicle.currentGLP + vehicle.weight) / 180;
    }
}
