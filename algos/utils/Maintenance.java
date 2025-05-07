package utils;

import domain.Time;

public class Maintenance {
    int id;
    String vehicleId;
    int year;
    Time date;

    Maintenance(int id, String vehicleId, int year, Time date) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.year = year;
        this.date = date;
    }

    
}
