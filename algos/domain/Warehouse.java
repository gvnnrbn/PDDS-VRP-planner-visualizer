package domain;

public record Warehouse(int id, Position position, int currentGLP, int maxGLP, boolean isMain, boolean wasVehicle) {
}
