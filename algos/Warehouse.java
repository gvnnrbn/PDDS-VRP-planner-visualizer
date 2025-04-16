public class Warehouse {
    Position position;
    int currentGLP;
    int currentFuel;
    boolean isMainWarehouse;
    boolean isBrokenVehicle;

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