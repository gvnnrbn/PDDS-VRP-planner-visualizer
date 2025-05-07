package scheduler;

import domain.Time;
import domain.Position;
import java.util.List;

public class StateBlockage {
    int id;
    Time startTime;
    Time endTime;
    List<Position> vertices; 
}
