package domain;

// Intended for the empty node at the end of the route
public class FinalNode extends Node {
    public Position position;

    public FinalNode(int id, Position position) {
        super(id);
        this.position = position;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public FinalNode clone() {
        return new FinalNode(id, position);
    }

    @Override
    public String toString() {
        return "FinalNode";
    }
}