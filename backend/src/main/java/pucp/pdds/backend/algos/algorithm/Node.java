package pucp.pdds.backend.algos.algorithm;

import pucp.pdds.backend.algos.utils.Position;

public abstract class Node implements Cloneable {
    public int id;
    public Node(int id) {
        this.id = id;
    }

    public abstract Node clone();

    public abstract Position getPosition();
}
