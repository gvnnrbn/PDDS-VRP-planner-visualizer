package domain_environment;
public class Position implements Cloneable {
    public int x;
    public int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Position clone() {
        return new Position(x, y);
    }
} 