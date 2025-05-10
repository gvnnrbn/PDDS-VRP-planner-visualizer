package scheduler;

public enum EnumVehicleState {
    IDLE,       //no routes planned, AVAILABLE
    ONTHEWAY,   // has routes planned, AVAILABLE
    WAITING,    // failure occured, AVAILABLE AS WAREHOUSE
    REPAIR,     // inside main warehouse, NOT AVAILABLE until x shift according to failure type
    MAINTENANCE, // heading to or in maintenance, NOT AVAILABLE until 23:59
}
