package data;

import java.util.List;
import java.util.ArrayList;
import utils.Time;
import utils.Position;
import entities.PlannerVehicle.VehicleState;

public class DataChunk {
    public static class MinuteData {
        public Time currTime;
        public List<VehicleData> vehicles;

        public MinuteData(Time currTime) {
            this.currTime = currTime;
            this.vehicles = new ArrayList<>();
        }
    }

    public static class VehicleData {
        public String plaque;
        public Position position;
        public VehicleState state;

        public VehicleData(String plaque, Position position, VehicleState state) {
            this.plaque = plaque;
            this.position = position;
            this.state = state;
        }
    }

    public List<MinuteData> minutes;

    public DataChunk() {
        this.minutes = new ArrayList<>();
    }
}
