package domain;

public class EmptyNode extends Node {
    public Position position;

    public EmptyNode(int id, Position position) {
        super(id);
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("Empty node at %s", position);
    }

    @Override
    public boolean isInfiniteNode() {
        return false;
    }

    @Override
    public Position getPosition() {
        return position;
    }
}
