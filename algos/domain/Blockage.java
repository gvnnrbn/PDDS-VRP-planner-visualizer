package domain;

import java.util.List;

public record Blockage(List<Position> vertices) {
    // Assumes a and b are colinear
    public boolean blocksRoute(Position a, Position b) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
