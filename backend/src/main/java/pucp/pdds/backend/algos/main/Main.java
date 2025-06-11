package pucp.pdds.backend.algos.main;

import pucp.pdds.backend.algos.scheduler.Scheduler;
import pucp.pdds.backend.algos.scheduler.SchedulerAgent;
import pucp.pdds.backend.algos.scheduler.SchedulerAgentTextFiles;
import pucp.pdds.backend.algos.utils.Time;

public class Main {
    public static void main(String[] args) {
        SchedulerAgent agent = new SchedulerAgentTextFiles();

        Time initialTime = new Time(2025, 1, 1, 0, 0);
        int weekTime = 7 * 24 * 60;
        Scheduler scheduler = new Scheduler(agent, initialTime, weekTime, 60);
        scheduler.setDebug(true);
        scheduler.run();
    }
}
