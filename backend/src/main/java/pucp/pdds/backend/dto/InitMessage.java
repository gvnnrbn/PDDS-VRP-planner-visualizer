package pucp.pdds.backend.dto;

import pucp.pdds.backend.algos.utils.Time;

public class InitMessage {
    private Time initialTime;
    
    public InitMessage() {}
    
    public InitMessage(Time initialTime) {
        this.initialTime = initialTime;
    }
    
    public Time getInitialTime() {
        return initialTime;
    }
    
    public void setInitialTime(Time initialTime) {
        this.initialTime = initialTime;
    }
}