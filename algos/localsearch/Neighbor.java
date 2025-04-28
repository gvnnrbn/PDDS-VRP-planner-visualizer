package localsearch;

import domain.Solution;

public class Neighbor {
    public Solution solution;
    public Movement movement;

    public Neighbor(Solution solution, Movement movement) {
        this.solution = solution;
        this.movement = movement;
    }
}
