package pucp.pdds.backend.algos.data;

import java.util.ArrayList;
import java.util.List;

public class Indicator {
    
    public double fuelCounterTA = 0;
    public double fuelCounterTB = 0;
    public double fuelCounterTC = 0;
    public double fuelCounterTD = 0;
    public double fuelCounterTotal = 0;

    public double glpFilledNorth = 0;
    public double glpFilledEast = 0;
    public double glpFilledMain = 0;
    public double glpFilledTotal = 0;

    public int completedOrders = 0;
    public int totalOrders = 0;

    public List<Double> deliveryTimes = new ArrayList<>();
    public double meanDeliveryTime = 0; //minutos

    public Indicator() {
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public double getFuelCounterTA() {
        return fuelCounterTA;
    }

    public double getFuelCounterTB() {
        return fuelCounterTB;
    }

    public double getFuelCounterTC() {
        return fuelCounterTC;
    }

    public double getFuelCounterTD() {
        return fuelCounterTD;
    }

    public double getFuelCounterTotal() {
        return fuelCounterTotal;
    }

    public double getGlpFilledNorth() {
        return glpFilledNorth;
    }

    public double getGlpFilledEast() {
        return glpFilledEast;
    }

    public double getGlpFilledMain() {
        return glpFilledMain;
    }

    public double getGlpFilledTotal() {
        return glpFilledTotal;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public List<Double> getDeliveryTimes() {
        return deliveryTimes;
    }

    public double getMeanDeliveryTime() {
        return meanDeliveryTime;
    }

    public void calculateMeanDeliveryTime() {
        if (!deliveryTimes.isEmpty()) {
            meanDeliveryTime = deliveryTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        } else {
            meanDeliveryTime = 0;
        }
    }
}
