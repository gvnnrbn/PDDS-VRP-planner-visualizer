package domain;

public record Vehicle(
    int id,
    int weight,
    int maxFuel,
    double currentFuel,
    int maxGLP,
    int currentGLP,
    Position initialPosition
) {}
