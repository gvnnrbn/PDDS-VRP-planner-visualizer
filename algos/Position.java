public class Position implements Cloneable {
    public int x;
    public int y;

    @Override
    public Position clone() {
        Position clone = new Position();
        clone.x = this.x;
        clone.y = this.y;
        return clone;
    }
} 