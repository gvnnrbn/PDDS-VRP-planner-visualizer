package pucp.pdds.backend.algos.utils;

public class Position implements Cloneable {
    public double x;
    public double y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean isInteger() {
        return x % 1 == 0 && y % 1 == 0;
    }

    public Position round() {
        return new Position(Math.round(x), Math.round(y));
    }

    public boolean isPossible() {
        boolean firstGuard = x >= 0 && x <= SimulationProperties.gridLength && y >= 0 && y <= SimulationProperties.gridWidth;
        boolean secondGuard = (isInteger() && y % 1 != 0) || (!isInteger() && y % 1 == 0);
        return firstGuard && secondGuard;
    }

    @Override
    public String toString() {
        return "Position{" + "x=" + x + ", y=" + y + '}';
    }

    @Override
    public Position clone() {
        try {
            Position clone = (Position) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can never happen
        }
    }
}
