package domain;

public abstract class Node {
    public int id;
    public Node(int id) {
        this.id = id;
    }

    public abstract boolean isInfiniteNode();

    public abstract Position getPosition();
} 