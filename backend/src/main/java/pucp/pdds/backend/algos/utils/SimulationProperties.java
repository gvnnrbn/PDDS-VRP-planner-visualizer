package pucp.pdds.backend.algos.utils;

public class SimulationProperties {
    public static double speed = 70.0; // km/h
    public static int timeAfterDelivery = 15; // minutes
    public static int timeAfterRefill = 0; // minutes

    public static int gridLength = 70; // km, grid unit
    public static int gridWidth = 50; // km, grid unit

    public static int msPerMinute = 250;
    public static int replanningInterval = 90;

    public static int maxTimeMs = (int) (msPerMinute * replanningInterval * 0.8);
}
