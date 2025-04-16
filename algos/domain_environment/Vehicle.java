package domain_environment;
import java.util.ArrayList;
import java.util.List;

public class Vehicle {
    public int id;
    public Position position;
    public int currentGLP;
    public int maxGLP;
    public int currentFuel;
    public int maxFuel;
    public double weight; // tons
    public TimeMoment startAvailabilityMoment;
    public List<Maintenance> maintenances = new ArrayList<Maintenance>();
}