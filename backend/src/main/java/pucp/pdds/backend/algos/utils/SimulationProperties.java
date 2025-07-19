package pucp.pdds.backend.algos.utils;

public class SimulationProperties {
    public static double speed = 75.0; // km/h
    public static int timeAfterDelivery = 15; // minutes
    public static int timeAfterRefill = 0; // minutes

    public static int gridLength = 70; // km, grid unit
    public static int gridWidth = 50; // km, grid unit

    public static int msPerMinute = 200; // Más rápido para simular más tiempo
    public static int replanningInterval = 120; // Más tiempo entre replanificaciones

    public static int maxTimeMs = (int) (msPerMinute * replanningInterval * 0.8); // Más tiempo para el algoritmo
}
