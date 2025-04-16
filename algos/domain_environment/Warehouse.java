package domain_environment;
public class Warehouse {
    public Position position;
    public int currentGLP;
    public int currentFuel;
    public boolean isMainWarehouse;
    public boolean isBrokenVehicle;

    public boolean canLoadGLP(int amount) {
        if (isMainWarehouse) {
            return true;
        } else {
            return amount <= currentGLP;
        }
    }

    public boolean canLoadFuel(int amount) {
        if (isMainWarehouse) {
            return true;
        } else {
            return amount <= currentFuel && !isBrokenVehicle;
        }
    }
}