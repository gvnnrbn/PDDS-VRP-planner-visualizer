package pucp.pdds.backend.algos.algorithm;

import pucp.pdds.backend.algos.utils.Position;

public class EmptyNode extends Node {
    public Position position;

    @Override
    public EmptyNode clone() {
        return new EmptyNode(id, position);
    }

    public EmptyNode(int id, Position position) {
        super(id);
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("Empty node at %s", position);
    }

    @Override
    public Position getPosition() {
        return position;
    }
}

