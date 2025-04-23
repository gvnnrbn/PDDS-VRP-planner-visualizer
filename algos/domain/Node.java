package domain;

public abstract class Node implements Cloneable {
    public int id;
    public Node(int id) {
        this.id = id;
    }

    public abstract Node clone();

    public abstract boolean isInfiniteNode();

    public abstract Position getPosition();
} 